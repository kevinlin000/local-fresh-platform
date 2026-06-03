# Testing Strategy

This project uses a small but explicit test pyramid. The goal is not to chase
coverage numbers; it is to prove the business boundaries that matter for a
backend portfolio project.

## Test Layers

| Layer | Purpose | Examples |
|---|---|---|
| Unit tests | Validate isolated business rules without Spring context cost. | `OrderStatusTransitionPolicyTest`, `OrderServiceImplTest`, `GoogleOAuthClientImplTest` |
| Spring integration tests | Validate HTTP/interceptor/mapper behavior against H2 in MySQL mode. | IDOR order/address regression tests, product/cart API tests, group-buy controller tests |
| Redis integration tests | Validate Redisson lock behavior against real Redis through Testcontainers. | `GroupBuyRedisIntegrationTest` |

## Default Backend Test Command

The default command is CI-safe and does not require local Redis or Docker:

```bash
cd backend-environment/sky-take-out
mvn test
```

`sky-server` configures Maven Surefire with `excludedGroups=redis`, so Redis
Testcontainers tests are skipped by default. This keeps ordinary local review
and GitHub Actions runs deterministic.

## Redis / Testcontainers Command

Run the Redis-tagged concurrency tests when Docker is available:

```bash
cd backend-environment/sky-take-out
mvn -pl sky-server -DexcludedGroups= -Dgroups=redis test
```

These tests intentionally use real Redis instead of pure mocks because the risk
being tested is distributed lock behavior under concurrent group-buy joins.

## Frontend Checks

The admin frontend is checked in CI with:

```bash
cd frontend-environment/sky-admin-vue-ts
npm ci
npm run build
npm audit --omit=dev
```

The user storefront is still demo-focused and should be added to CI once its
dependency and build baseline are normalized.

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
- Avoid UI E2E until the deployment target and demo flows are stable.
