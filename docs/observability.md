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

## Business Metrics

| Metric | Tags | Meaning |
|---|---|---|
| `localfresh.payment.callback.total` | `provider`, `result` | Counts payment callback outcomes such as `succeeded`, `rejected`, and `ignored`. |
| `localfresh.order.cancellation.total` | `result` | Counts applied cancellations and duplicate cancellation skips. |
| `localfresh.group_buy.transition.total` | `result` | Counts group-buy transitions such as `completed`, `failed`, and `canceled`. |

Tag values are normalized to lowercase, trim whitespace, replace `-` with `_`, and use `unknown` when blank.

## Local Checks

Start the backend, exercise a payment or cancellation flow, then inspect the counters:

```bash
curl http://localhost:8080/actuator/metrics/localfresh.payment.callback.total
curl http://localhost:8080/actuator/metrics/localfresh.order.cancellation.total
curl http://localhost:8080/actuator/metrics/localfresh.group_buy.transition.total
```

## Current Boundary

This is enough to show production thinking in an interview: the system can answer whether payment callbacks are being accepted, rejected, duplicated, whether cancellation idempotency guards are being hit, and whether group-buy transitions are moving as expected.

Prometheus scraping, Grafana dashboards, alert rules, and distributed tracing are intentionally deferred until the core demo and cloud environment are stable.
