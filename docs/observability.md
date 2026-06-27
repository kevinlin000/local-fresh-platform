# Observability

This project keeps observability intentionally small: Spring Boot Actuator plus a few business counters that explain the most important portfolio workflows. The backend exposes a Prometheus scrape endpoint, includes an importable Grafana dashboard artifact, and now ships a local Prometheus / Grafana / Alertmanager stack for demo evidence. It is still not a full production SRE platform.

## Actuator Endpoints

The backend exposes these endpoints by default:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

The exposed endpoint list can be overridden without changing code:

```bash
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true
```

`/actuator/metrics` is useful for local inspection of individual meters.
`/actuator/prometheus` emits the scrape-format payload used by Prometheus.

## Deployment Identity

`/actuator/info` includes a small non-sensitive `deployment` block:

```json
{
  "deployment": {
    "application": "local-fresh-server",
    "profiles": ["prod"],
    "commit": "42c12ce",
    "branch": "main",
    "paymentCallbackPath": "/payment/callback"
  }
}
```

Set these environment variables on EC2 when starting the Spring Boot process:

```bash
SOURCE_COMMIT=<deployed git commit>
SOURCE_BRANCH=main
```

The checked-in `deploy/ec2/local-fresh-server.env.example` template includes
these variables so the release package and runtime metadata stay aligned.

The app also accepts `localfresh.deployment.commit` and
`localfresh.deployment.branch` if you prefer Spring properties. If neither is
set, the values fall back to `unknown`. This endpoint does not expose secrets;
it exists so public preflight checks can prove which backend version is running.

## Business Metrics

The current on-call questions are intentionally narrow:

- Are payment callbacks being accepted, rejected, or ignored as duplicates?
- Are payment reconciliation jobs applying provider results or repeatedly
  seeing pending/query-error outcomes?
- How many pending payment requests did the latest reconciliation scan find?
- Are order cancellation and group-buy transitions behaving normally?

| Metric | Tags | Meaning |
|---|---|---|
| `localfresh.payment.callback.total` | `provider`, `result` | Counts payment callback outcomes such as `succeeded`, `rejected`, and `ignored`. |
| `localfresh.payment.reconciliation.total` | `provider`, `result` | Counts provider-query reconciliation outcomes such as `applied`, `rejected`, `pending`, `unknown`, `query_error`, and `unsupported`. |
| `localfresh.payment.reconciliation.pending.candidates` | `provider` | Gauge for the latest number of pending payment requests scanned by reconciliation. |
| `localfresh.order.cancellation.total` | `result` | Counts applied cancellations and duplicate cancellation skips. |
| `localfresh.group_buy.transition.total` | `result` | Counts group-buy transitions such as `completed`, `failed`, and `canceled`. |

Tag values are normalized to lowercase, trim whitespace, replace `-` with `_`, and use `unknown` when blank.

## Local Checks

Start the backend, exercise a payment or cancellation flow, then inspect the counters:

```bash
curl http://localhost:8080/actuator/metrics/localfresh.payment.callback.total
curl http://localhost:8080/actuator/metrics/localfresh.payment.reconciliation.total
curl http://localhost:8080/actuator/metrics/localfresh.payment.reconciliation.pending.candidates
curl http://localhost:8080/actuator/metrics/localfresh.order.cancellation.total
curl http://localhost:8080/actuator/metrics/localfresh.group_buy.transition.total
curl http://localhost:8080/actuator/prometheus
```

A minimal external Prometheus scrape job can target the deployed backend like
this:

```yaml
scrape_configs:
  - job_name: "local-fresh-backend"
    scheme: https
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["localfresh-demo.duckdns.org"]
```

## Local Prometheus / Grafana / Alertmanager

The repository includes a local observability stack under:

```text
deploy/observability/
```

It is optional and does not replace the normal backend/frontend dev flow.
Start the Spring Boot backend on `localhost:8080` first, then run:

```bash
npm run observability:up
```

That script runs
`docker compose -f deploy/observability/docker-compose.yml up -d`. Stop the
stack with `npm run observability:down`.

Open:

| Tool | URL | Purpose |
|---|---|---|
| Prometheus | `http://localhost:9091` | Scrape target health, raw PromQL, alert rule status |
| Grafana | `http://localhost:3002` | Dashboard view; login `admin` / `localfresh` |
| Alertmanager | `http://localhost:9093` | Local alert state and grouping |

The default Prometheus target is `host.docker.internal:8080`, which works for
Docker Desktop on macOS and the included Linux `host-gateway` mapping. If the
backend runs elsewhere, edit
`deploy/observability/prometheus/prometheus.yml`.

Prometheus loads:

- `deploy/observability/prometheus/prometheus.yml`
- `deploy/observability/prometheus/rules/local-fresh-alerts.yml`

Grafana provisions:

- datasource: `Prometheus` -> `http://prometheus:9090`
- dashboard folder: `Local Fresh`
- dashboard file: `docs/grafana/local-fresh-operations-dashboard.json`

For a demo screenshot, verify these three things:

1. Prometheus `Status -> Targets` shows `local-fresh-backend` as `UP`.
2. Grafana shows the `Local Fresh Operations` dashboard with a selected
   Prometheus datasource.
3. Prometheus `Alerts` lists the `LocalFreshPayment...` alert rules.

Local validation on 2026-06-25:

- `docker compose -f deploy/observability/docker-compose.yml config`: passed.
- Prometheus readiness at `http://localhost:9091/-/ready`: passed.
- Alertmanager readiness at `http://localhost:9093/-/ready`: passed.
- Grafana health at `http://localhost:3002/api/health`: passed.
- Grafana search with `admin` / `localfresh`: `Local Fresh Operations`
  dashboard provisioned.
- Prometheus rules API: all `LocalFreshPayment...` alert rules loaded with
  `health="ok"`.

If Prometheus shows `local-fresh-backend` as `DOWN`, first confirm port `8080`
is actually this project's backend:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/actuator/prometheus
```

During local validation on 2026-06-27, port `8080` was occupied by a different
project (`restaurant-admin-backend`), so Prometheus correctly reported the
target as `DOWN` with HTTP `403`. If you intentionally run Local Fresh on a
different port, edit `deploy/observability/prometheus/prometheus.yml`; if the
correct backend is running but still returns `403`, check the actuator exposure
and security settings for `/actuator/prometheus`.

## Grafana Dashboard Artifact

The repository includes an importable Grafana dashboard:

```text
docs/grafana/local-fresh-operations-dashboard.json
```

It uses the Prometheus datasource variable `${DS_PROMETHEUS}` and the
`provider` dashboard variable. The dashboard is intentionally narrow and maps
directly to the business-risk questions above:

| Panel | Question it answers |
|---|---|
| Payment Callback Outcomes | Are callbacks succeeding, rejected, or being ignored as duplicates? |
| Latest Reconciliation Backlog | How many pending payment requests did the latest reconciliation scan find? |
| Reconciliation Query Errors | Is the provider query path failing right now? |
| Rejected Callbacks | Are callback signatures or provider settings breaking user payment completion? |
| Payment Reconciliation Outcomes | Are provider-query results being applied, rejected, pending, unknown, or unsupported? |
| Order Cancellation Outcomes | Are cancellation idempotency guards being hit unexpectedly? |
| Group-Buy Transitions | Are group-buy completion/failure/cancel transitions moving normally? |

Import steps:

1. Add the backend as a Prometheus scrape target using `/actuator/prometheus`.
2. In Grafana, import `docs/grafana/local-fresh-operations-dashboard.json`.
3. Select the Prometheus datasource for `${DS_PROMETHEUS}`.
4. Exercise payment, cancellation, or group-buy flows, then verify the panels
   show the corresponding counters/gauge.

## Alert Thresholds

These rules are checked into
`deploy/observability/prometheus/rules/local-fresh-alerts.yml` and wired to the
local Alertmanager container. They define the symptoms to watch when the same
Prometheus/Grafana setup is moved from local evidence to a longer-running
environment.

| Severity | Symptom | Suggested threshold | First check |
|---|---|---|---|
| ticket | Reconciliation query errors | `increase(localfresh_payment_reconciliation_total{result="query_error"}[10m]) > 0` | Check ECPay query URL/env, provider availability, and backend logs for `Payment reconciliation query failed`. |
| ticket | Pending reconciliation backlog remains high | `localfresh_payment_reconciliation_pending_candidates{provider="ecpay"} > 5` for 30m | Open `/admin/paymentEvents/pendingRequests` and compare with recent ECPay checkout attempts. |
| ticket | Provider keeps reporting pending payments | `increase(localfresh_payment_reconciliation_total{result="pending"}[30m]) > 5` | Check whether provider settlement is delayed or callbacks are failing to reach `/payment/callback`. |
| page | Payment callbacks are rejected repeatedly | `increase(localfresh_payment_callback_total{result="rejected"}[5m]) >= 3` | Check callback signature settings, `PAYMENT_PROVIDER`, ReturnURL, and recent Nginx `/payment/callback` logs. |
| ticket | Duplicate callbacks spike | `increase(localfresh_payment_callback_total{result="ignored"}[15m]) > 10` | Confirm provider retries and verify idempotency behavior is still writing `CALLBACK_DUPLICATE`. |

Runbook notes:

- Query current callback counters:
  `curl http://localhost:8080/actuator/metrics/localfresh.payment.callback.total`
- Query reconciliation counters:
  `curl http://localhost:8080/actuator/metrics/localfresh.payment.reconciliation.total`
- Query latest pending reconciliation candidate count:
  `curl http://localhost:8080/actuator/metrics/localfresh.payment.reconciliation.pending.candidates`
- Query pending work queue:
  `GET /admin/paymentEvents/pendingRequests?page=1&pageSize=20&provider=ECPAY`
- Keep `PAYMENT_RECONCILIATION_ENABLED=false` for ordinary demos unless you
  are intentionally collecting external-query evidence.

## Current Boundary

This is enough to show production thinking in an interview: the system can answer whether payment callbacks are being accepted, rejected, duplicated, whether reconciliation is applying provider results or getting stuck, how many pending payment requests the latest reconciliation scan found, whether cancellation idempotency guards are being hit, and whether group-buy transitions are moving as expected.

The scrape endpoint, dashboard artifact, local Grafana provisioning, Prometheus
alert rules, and local Alertmanager wiring are available. Dashboard screenshots,
long-running cloud monitoring evidence, notification receivers, and distributed
tracing are intentionally deferred until the core demo and cloud environment are
stable. The alert symptoms and first checks above are the contract for that
later monitoring stack.
