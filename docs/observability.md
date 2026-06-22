# Observability

This project keeps observability intentionally small: Spring Boot Actuator plus a few business counters that explain the most important portfolio workflows. It does not run Prometheus or Grafana yet.

## Actuator Endpoints

The backend exposes these endpoints by default:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

The exposed endpoint list can be overridden without changing code:

```bash
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
```

## Deployment Identity

`/actuator/info` includes a small non-sensitive `deployment` block:

```json
{
  "deployment": {
    "application": "local-fresh-server",
    "profiles": ["prod"],
    "commit": "42c12ce",
    "branch": "hardening-and-upgrade",
    "paymentCallbackPath": "/payment/callback"
  }
}
```

Set these environment variables on EC2 when starting the Spring Boot process:

```bash
SOURCE_COMMIT=<deployed git commit>
SOURCE_BRANCH=hardening-and-upgrade
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
- Are order cancellation and group-buy transitions behaving normally?

| Metric | Tags | Meaning |
|---|---|---|
| `localfresh.payment.callback.total` | `provider`, `result` | Counts payment callback outcomes such as `succeeded`, `rejected`, and `ignored`. |
| `localfresh.payment.reconciliation.total` | `provider`, `result` | Counts provider-query reconciliation outcomes such as `applied`, `rejected`, `pending`, `unknown`, `query_error`, and `unsupported`. |
| `localfresh.order.cancellation.total` | `result` | Counts applied cancellations and duplicate cancellation skips. |
| `localfresh.group_buy.transition.total` | `result` | Counts group-buy transitions such as `completed`, `failed`, and `canceled`. |

Tag values are normalized to lowercase, trim whitespace, replace `-` with `_`, and use `unknown` when blank.

## Local Checks

Start the backend, exercise a payment or cancellation flow, then inspect the counters:

```bash
curl http://localhost:8080/actuator/metrics/localfresh.payment.callback.total
curl http://localhost:8080/actuator/metrics/localfresh.payment.reconciliation.total
curl http://localhost:8080/actuator/metrics/localfresh.order.cancellation.total
curl http://localhost:8080/actuator/metrics/localfresh.group_buy.transition.total
```

## Alert Thresholds

These are portfolio-grade alert rules rather than a deployed Prometheus setup.
They define the symptoms to watch when Prometheus/Grafana is added later.

| Severity | Symptom | Suggested threshold | First check |
|---|---|---|---|
| ticket | Reconciliation query errors | `increase(localfresh_payment_reconciliation_total{result="query_error"}[10m]) > 0` | Check ECPay query URL/env, provider availability, and backend logs for `Payment reconciliation query failed`. |
| ticket | Pending reconciliation keeps growing | `increase(localfresh_payment_reconciliation_total{result="pending"}[30m]) > 5` | Open `/admin/paymentEvents/pendingRequests` and compare with recent ECPay checkout attempts. |
| page | Payment callbacks are rejected repeatedly | `increase(localfresh_payment_callback_total{result="rejected"}[5m]) >= 3` | Check callback signature settings, `PAYMENT_PROVIDER`, ReturnURL, and recent Nginx `/payment/callback` logs. |
| ticket | Duplicate callbacks spike | `increase(localfresh_payment_callback_total{result="ignored"}[15m]) > 10` | Confirm provider retries and verify idempotency behavior is still writing `CALLBACK_DUPLICATE`. |

Runbook notes:

- Query current callback counters:
  `curl http://localhost:8080/actuator/metrics/localfresh.payment.callback.total`
- Query reconciliation counters:
  `curl http://localhost:8080/actuator/metrics/localfresh.payment.reconciliation.total`
- Query pending work queue:
  `GET /admin/paymentEvents/pendingRequests?page=1&pageSize=20&provider=ECPAY`
- Keep `PAYMENT_RECONCILIATION_ENABLED=false` for ordinary demos unless you
  are intentionally collecting external-query evidence.

## Current Boundary

This is enough to show production thinking in an interview: the system can answer whether payment callbacks are being accepted, rejected, duplicated, whether reconciliation is applying provider results or getting stuck, whether cancellation idempotency guards are being hit, and whether group-buy transitions are moving as expected.

Prometheus scraping, Grafana dashboards, and distributed tracing are intentionally deferred until the core demo and cloud environment are stable. The alert symptoms and first checks above are the contract for that later monitoring stack.
