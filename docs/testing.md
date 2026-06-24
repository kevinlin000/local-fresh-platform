# Testing Strategy

This project uses a small but explicit test pyramid. The goal is not to chase
coverage numbers; it is to prove the business boundaries that matter for a
backend portfolio project.

## Test Layers

| Layer | Purpose | Examples |
|---|---|---|
| Unit tests | Validate isolated business rules without Spring context cost. | `OrderStatusTransitionPolicyTest`, `OrderServiceImplTest`, `AdminOperationLogServiceImplTest`, `GoogleOAuthClientImplTest` |
| Spring integration tests | Validate HTTP/interceptor/mapper behavior against H2 in MySQL mode. | IDOR order/address regression tests, product/cart API tests, audit-log API tests, group-buy controller tests |
| Redis integration tests | Validate Redisson lock behavior against real Redis through Testcontainers. | `GroupBuyRedisIntegrationTest`, `GroupBuyExpirationServiceTest` |

## Default Backend Test Command

The default backend quality gate is:

```bash
cd backend-environment/local-fresh-backend
mvn -pl local-fresh-server -am verify
```

This runs the backend test suite and generates the JaCoCo HTML report used as
portfolio evidence. `local-fresh-server` configures Maven Surefire with
`excludedGroups=redis`, so Redis Testcontainers tests are skipped unless they
are explicitly requested. This keeps ordinary local review and GitHub Actions
runs deterministic.

## Backend Coverage Report

`local-fresh-server` also configures JaCoCo for local evidence generation. The
report is intentionally informational; there is no hard coverage gate because
this portfolio project values tested business boundaries over an arbitrary
percentage.

```bash
cd backend-environment/local-fresh-backend
mvn -pl local-fresh-server -am verify
```

The generated HTML report is written to:

```text
backend-environment/local-fresh-backend/local-fresh-server/target/site/jacoco/index.html
```

In GitHub Actions, the backend job uploads two artifacts:

- `backend-jacoco-report`: the JaCoCo HTML report from the verified backend run.
- `backend-release-package`: the repackaged Spring Boot jar plus `release.env`,
  `release-manifest.txt`, `SHA256SUMS`, EC2 deploy commands, and
  `deploy-templates/` copied from `deploy/ec2/`. CI runs
  `scripts/verify-backend-release-package.sh output/backend-release` before
  uploading this artifact.

The current coverage emphasis is the order lifecycle:

- `OrderStatusTransitionPolicy`: legal order-state transitions.
- `OrderServiceImpl`: member/admin ownership and status guard behavior,
  including payment request guards that reject cancelled orders and pending
  group-buy pre-orders before any provider request is created.
- `OrderPaymentServiceImpl`: provider-neutral payment request creation, demo
  immediate success strategy, payment callback command handling, payment event
  recording, ECPay duplicate callback with provider trade number evidence,
  concurrent callback race, and invalid transition handling.
- `DemoPaymentGatewayTest`: demo callback HMAC verification and payload mapping.
- `EcpayCheckMacValueCalculatorTest`: ECPay callback checksum sorting,
  CheckMacValue exclusion, and case-insensitive verification.
- `EcpayPaymentGatewayTest`: ECPay callback CheckMacValue verification,
  MerchantTradeNo / TradeNo / RtnCode mapping, signed redirect payload shape,
  and provider query-result status mapping.
- `PaymentGatewayProviderSelectionTest`: Spring conditional provider selection
  for default demo payments and ECPay readiness, including deployed ReturnURL /
  OrderResultURL in the signed checkout payload.
- `PaymentCallbackControllerEcpayContractTest`: the real ECPay gateway wired
  into `/payment/callback`, proving signed form callbacks return `1|OK` and
  invalid `CheckMacValue` callbacks return `0|FAIL` without reaching payment
  handling.
- `PaymentCallbackControllerTest`: provider callback HTTP response contract
  (`1|OK` / `0|FAIL`) without requiring member or admin JWT, including
  form-url-encoded ECPay callback payload handling.
- `PaymentEventMapperTest`: payment-event insert, order-number lookup, provider
  trade number filtering, and idempotency-key filtering through the real mapper
  and H2 schema.
- `PaymentEventApiTest`: admin payment-event pagination and filtering through
  the HTTP layer and JWT interceptor, including provider trade number,
  idempotency-key query fields, and the minimal pending-request reconciliation
  endpoint.
- `PaymentReconciliationServiceImplTest`: provider-query reconciliation over
  pending payment requests, including succeeded, still-pending, and rejected
  provider query results.
- `BusinessMetricsServiceImplTest`: custom Micrometer business counters for
  payment callback outcomes, payment reconciliation outcomes,
  duplicate/applied order cancellations, and group-buy transitions.
- `PrometheusEndpointTest`: Prometheus registry and `/actuator/prometheus`
  scrape-format endpoint registration.
- `DeploymentInfoContributorTest`: `/actuator/info` deployment identity,
  including commit, branch, active profiles, and the payment callback path used
  by public readiness checks.
- `OrderCancellationServiceImpl`: refund metadata, inventory restoration,
  product-level restore aggregation, and duplicate-cancellation guards that
  avoid repeated refunds or stock restores.
- `OrderFulfillmentServiceImpl`: admin confirm, delivery, completion, and member
  reminder behavior, including missing-order guard behavior.
- `GroupBuyExpirationServiceTest`: expired active groups fail, pending group-buy
  orders are canceled, refund intent is logged, and a held Redisson lock skips
  duplicate processing.
- `ProductServiceImpl`: product deletion guards, successful product/spec
  deletion side effects, product disable cascading to related gift boxes, manual
  inventory adjustment audit logging, and product cache invalidation.
- `ProductInventoryOrderTest`: order reserve/restore inventory logs and the
  nullable `product_inventory_log.idempotency_key` unique constraint used as a
  DB-level duplicate-restore guard.
- `AdminOperationLogServiceImpl`: admin order and inventory operation audit
  entries plus paginated query mapping.
- `AdminOperationLogApiTest`: HTTP-level audit-log pagination, filtering, and
  newest-first ordering through the real mapper.

## Redis / Testcontainers Command

Run the Redis-tagged concurrency tests when Docker is available:

## Redis / Testcontainers Command

```bash
cd backend-environment/local-fresh-backend
mvn -pl local-fresh-server -DexcludedGroups= -Dgroups=redis test
```

These tests intentionally use real Redis instead of pure mocks because the risk
being tested is distributed lock behavior under concurrent group-buy joins.

## Frontend Checks

Repository-level hygiene is checked in CI with:

```bash
node scripts/check-repo-hygiene.mjs
```

This verifies README screenshot references, legacy course/platform terminology,
and trailing whitespace in tracked text files.

The admin frontend is checked in CI with:

```bash
cd frontend-environment/local-fresh-admin
npm ci
npm run build
npm audit --omit=dev
```

The user storefront is also checked in CI:

```bash
cd frontend-environment/local-fresh-user
corepack pnpm@10.25.0 install --frozen-lockfile
corepack pnpm@10.25.0 run build
```

## Manual Browser Acceptance

Frontend visual quality is currently validated through targeted browser
acceptance rather than a full visual-regression suite. For portfolio review, the
latest accepted screenshots are committed under `docs/screenshots/` and cover:

- User storefront: home, product list, product detail, group-buy detail, cart,
  and order history.
- Admin console: dashboard, product management, and order management.

The local acceptance pass uses Playwright against the running dev services:

```bash
# backend
cd backend-environment/local-fresh-backend
mvn install -DskipTests
mvn -pl local-fresh-server spring-boot:run

# user storefront
cd frontend-environment/local-fresh-user
corepack pnpm@10.25.0 run dev

# admin console
cd frontend-environment/local-fresh-admin
npm run dev
```

The current manual browser acceptance also checks the admin operation-log and
payment-event pages for API data loading, filtering surfaces, and layout width.
For the ECPay redirect scaffold, browser inspection verifies that a signed
payment response creates a hidden POST form targeting the stage checkout URL
with provider fields such as `MerchantID`, `MerchantTradeNo`, and
`CheckMacValue`.

Before a screenshot refresh or live demo, run the local smoke precheck against
the running services:

```bash
API_BASE_URL=http://127.0.0.1:8080 npm run smoke:backend

USER_BASE_URL=http://127.0.0.1:5176 \
ADMIN_BASE_URL=http://127.0.0.1:5177 \
scripts/check-ui-smoke-local.mjs
```

`npm run smoke:backend` verifies that `/actuator/info` identifies the service as
`local-fresh-server` before checking health. This prevents a misleading pass
when another local project is occupying port `8080`.

If port `8080` is already in use, start the backend on a temporary demo port and
point the frontends/smoke checks at that port:

```bash
cd backend-environment/local-fresh-backend
MYSQL_ROOT_PASSWORD=password mvn -f local-fresh-server/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=18080

cd frontend-environment/local-fresh-user
VITE_API_PROXY_TARGET=http://localhost:18080 corepack pnpm@10.25.0 exec vite --host 127.0.0.1 --port 5176

cd frontend-environment/local-fresh-admin
VITE_API_PROXY_TARGET=http://localhost:18080 npm run dev -- --host 127.0.0.1 --port 5177

cd ../..
API_BASE_URL=http://127.0.0.1:18080 npm run smoke:backend
```

The full local smoke script intentionally has no third-party dependency. It
verifies that the backend identity and health endpoints are correct, both Vue
app shells are served, member mock login works, admin login works,
product/order APIs return data, and admin dashboard / order APIs are reachable.
It is not a visual-regression test; it catches broken local services,
proxy/auth regressions, wrong-backend port conflicts, and empty demo data before
manual browser acceptance.

The browser smoke test adds a real Chromium pass over the most important local
UI routes:

```bash
npm install
npx playwright install chromium
USER_BASE_URL=http://127.0.0.1:5176 \
ADMIN_BASE_URL=http://127.0.0.1:5177 \
npm run smoke:browser
```

This Playwright check logs into the member storefront with the mock account,
verifies the home and order-history pages, logs into the admin console, and
verifies the dashboard, order-management, and product-management pages. It is
still a smoke test, not a pixel-level visual regression suite.

This is intentionally lighter than full E2E automation. The current project
goal is to prove the backend workflow and provide credible UI evidence; a future
production-facing slice should add automated smoke tests for login, checkout,
group-buy join, and admin order fulfillment.

## Test Profile

Backend tests run with `application-test.yml`:

- H2 runs in MySQL compatibility mode.
- Flyway is disabled; `schema-test.sql` initializes the test schema.
- Redis health and repository auto-configuration are disabled unless a specific
  Redis/Testcontainers test opts in.
- SpringDoc and mapper DEBUG logs are suppressed to keep CI output readable.
- Spring Boot deprecated `@MockBean` usage has been migrated to Spring
  Framework `@MockitoBean`.

## Current Coverage Priorities

- Keep adding pure service tests before adding new features.
- Prefer integration tests for IDOR/security boundaries and request
  interceptors.
- Keep Testcontainers reserved for infrastructure behavior that mocks cannot
  prove, such as Redisson locking.
- Add UI smoke or visual-regression coverage once the demo deployment and main
  browser flows stop changing between portfolio polish passes.
