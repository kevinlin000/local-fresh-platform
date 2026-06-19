# Testing Strategy

This project uses a small but explicit test pyramid. The goal is not to chase
coverage numbers; it is to prove the business boundaries that matter for a
backend portfolio project.

## Test Layers

| Layer | Purpose | Examples |
|---|---|---|
| Unit tests | Validate isolated business rules without Spring context cost. | `OrderStatusTransitionPolicyTest`, `OrderServiceImplTest`, `AdminOperationLogServiceImplTest`, `GoogleOAuthClientImplTest` |
| Spring integration tests | Validate HTTP/interceptor/mapper behavior against H2 in MySQL mode. | IDOR order/address regression tests, product/cart API tests, group-buy controller tests |
| Redis integration tests | Validate Redisson lock behavior against real Redis through Testcontainers. | `GroupBuyRedisIntegrationTest` |

## Default Backend Test Command

The default command is CI-safe and does not require local Redis or Docker:

```bash
cd backend-environment/local-fresh-backend
mvn test
```

`local-fresh-server` configures Maven Surefire with `excludedGroups=redis`, so Redis
Testcontainers tests are skipped by default. This keeps ordinary local review
and GitHub Actions runs deterministic.

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

The current coverage emphasis is the order lifecycle:

- `OrderStatusTransitionPolicy`: legal order-state transitions.
- `OrderServiceImpl`: member/admin ownership and status guard behavior.
- `OrderPaymentServiceImpl`: payment success, duplicate callback, and invalid
  transition handling.
- `OrderCancellationServiceImpl`: refund metadata and inventory restoration.
- `OrderFulfillmentServiceImpl`: admin confirm, delivery, completion, and member
  reminder behavior.
- `AdminOperationLogServiceImpl`: admin order and inventory operation audit
  entries plus paginated query mapping.

## Redis / Testcontainers Command

Run the Redis-tagged concurrency tests when Docker is available:

```bash
cd backend-environment/local-fresh-backend
mvn -pl local-fresh-server -DexcludedGroups= -Dgroups=redis test
```

These tests intentionally use real Redis instead of pure mocks because the risk
being tested is distributed lock behavior under concurrent group-buy joins.

## Frontend Checks

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
