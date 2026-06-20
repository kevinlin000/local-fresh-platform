# Local Fresh Platform

> A B2C grocery platform combining local farm-to-table delivery with group-buy promotions

![Java 17](https://img.shields.io/badge/Java-17-3A7D44?style=flat-square)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat-square)
![Vue 3](https://img.shields.io/badge/Vue-3-42B883?style=flat-square)
![License MIT](https://img.shields.io/badge/License-MIT-4E9F3D?style=flat-square)

**Demo URL**

User storefront

[https://d3hqnux25iirgl.cloudfront.net](https://d3hqnux25iirgl.cloudfront.net)

> The user-facing storefront is publicly accessible. Try the dev-mode mock login for instant access, or sign in with a real Google account to experience the full OAuth flow.

Admin console

[https://d3czahyk4cnvb9.cloudfront.net](https://d3czahyk4cnvb9.cloudfront.net)

> The admin console is a Vue 3 + Vite + Element Plus operations surface for order handling, product operations, inventory checks, categories, delivery boxes, employees, and operational metrics.

> Backend API endpoint: `https://localfresh-demo.duckdns.org`
> Deployment topology: Vue 3 storefront hosted on AWS S3 + CloudFront (HTTPS), Spring Boot API on AWS EC2 (Nginx reverse proxy with Let's Encrypt TLS).

## Demo Screenshots

End-to-end flow: storefront browsing → checkout → group buy → order tracking → admin operations.

### 1. Home — Market Entry and Shopping Context

The home page is shopping-first: market context, delivery facts, search, category entry points, and the group-buy free-shipping offer as a supporting commerce module.

![Home](docs/screenshots/01-home.png)

### 2. Product List — Categories, Filters, Product Cards

The catalog combines category navigation, sorting, price filters, product metadata, delivery status, descriptions, and TWD pricing.

![Product List](docs/screenshots/02-product-list.png)

### 3. Product Detail — Two Checkout Paths

Either "Add to Cart" for batch checkout, or "Start Group Buy" to launch a 3-person free-shipping group.

![Product Detail](docs/screenshots/03-product-detail.png)

### 4. Group Buy Detail — Live Progress & Initiator Cancellation

Countdown, participant progress, shareable link, and member list are shown together. The initiator can cancel when no other member has joined, with the pre-order auto-canceled.

![Group Buy](docs/screenshots/04-group-buy.png)

### 5. Cart — Real-time Total

Cart items and the order summary are separated so item quantities and checkout totals are easy to scan before submitting an order.

![Cart](docs/screenshots/05-cart.png)

### 6. My Orders — Multi-State Tracking

Lists orders across all states (completed / delivering / accepted / unpaid / canceled),
with product details and cancellation notes.

![My Orders](docs/screenshots/06-orders.png)

### 7. Admin Dashboard — Operational Priorities

The dashboard gives operators a daily view of revenue, order states, low-stock items, and priority queues.

![Admin Dashboard](docs/screenshots/07-admin-dashboard.png)

### 8. Product Management — Listing Quality and Inventory

The product table supports search, status filters, low-stock filtering, listing-quality checks, inventory adjustment, and inventory logs.

![Admin Products](docs/screenshots/08-admin-products.png)

### 9. Order Management — Fulfillment Actions

The admin order page supports status search, detail inspection, confirmation, rejection, cancellation, delivery, and completion workflows.

![Admin Orders](docs/screenshots/09-admin-orders.png)

## Overview

Online grocery commerce in Taiwan often runs into two practical problems: small orders are heavily penalized by shipping fees, and most platforms stop at catalog plus checkout without offering a mechanism that encourages collaborative purchasing. Local Fresh Platform addresses both issues by combining local farm-to-table delivery with group-buy incentives. Users can browse individual products and curated gift boxes, add items to cart, manage delivery addresses, and place orders through a conventional checkout flow. If they want to reduce shipping costs, they can launch a group-buy campaign, share a link with others, and unlock free shipping once the required member count is reached. The platform also includes administrative capabilities for product operations, order handling, and store status management. The project is intentionally built as a production-oriented full-stack portfolio piece, with an emphasis on strong engineering fundamentals, coherent domain modeling, and deployment readiness.

## Architecture

```text
┌───────────────────────────────┐
│        User Web (Vue 3)       │
│ Browsing / Cart / Orders /    │
│ Group Buy / Google OAuth      │
└──────────────┬────────────────┘
               │ HTTP / JWT
               ▼
┌──────────────────────────────────────────────┐
│         Spring Boot 3.5 Backend API         │
│ Member / Product / Cart / Order / GroupBuy  │
│ Google OAuth / Cache / Scheduler / WS       │
└───────┬──────────────────┬──────────────────┘
        │                  │
        ▼                  ▼
┌───────────────┐   ┌──────────────────┐
│   MySQL 8     │   │   Redis 7        │
│ Orders /      │   │ Cache / Store    │
│ Products /    │   │ Status / Locking │
│ Members / GB  │   └─────────┬────────┘
└───────────────┘             │
                              ▼
                     ┌──────────────────┐
                     │   Redisson       │
                     │ Group Buy Locks  │
                     └─────────┬────────┘
                               │
                               ▼
                     ┌──────────────────┐
                     │ Scheduler / WS   │
                     │ Expiry / Notify  │
                     └──────────────────┘

External services:
- Google OAuth 2.0 for member login
- Google Maps API for delivery range checks
- AWS: EC2 (Dockerized MySQL + Redis), S3, CloudFront, DuckDNS
```

### Tech Stack

| Layer | Technologies |
|---|---|
| Backend | Java 17, Spring Boot 3.5.14, MyBatis, PageHelper, Flyway, JWT, HikariCP, Actuator |
| Frontend | User Vue 3 + Vite 5, Admin Vue 3 + Vite 8, TypeScript, Pinia, Vue Router 4, Element Plus |
| Infrastructure | MySQL 8, Redis 7, Redisson, Testcontainers, Docker, GitHub Actions |
| Third-party Services | Google OAuth 2.0, Google Maps API, AWS EC2 + S3 + CloudFront + DuckDNS |

## Core Features

- Product and gift box browsing: Browse seasonal groceries by category, including individual items and curated delivery boxes.
- Cart and checkout flow: Add items to cart, adjust quantities, manage delivery addresses, leave notes, and review order history.
- Group-buy free shipping campaigns: Create a campaign, share a link, and convert pre-orders into confirmed orders once 3 members join.
- Dual login model: Google OAuth 2.0 for realistic login behavior, plus mock login in development for testing and demos.
- Order lifecycle management: Covers ordinary order states as well as the dedicated pre-order state used during group-buy campaigns.
- Store and operations management: Admin-side support for products, categories, orders, and store open/close status.
- Cache and scheduled job coordination: Redis-backed cache plus scheduled group-buy expiry handling and refund simulation.

## Technical Highlights

### 1. Concurrency control for the group-buy module

The hardest part of the group-buy workflow is preventing oversubscription when multiple members attempt to join the same campaign at the same time. This project uses a Redisson `RLock` keyed by `groupNo` and configured with `tryLock(3, 5, TimeUnit.SECONDS)`: requests wait up to 3 seconds to acquire the lock, while lock ownership is capped at 5 seconds to avoid long-lived contention. Inside the protected section, validation, pre-order creation, participant insertion, count increment, and completion checks are executed as a single transaction. WebSocket notifications are intentionally sent only after the transaction commits, ensuring downstream consumers never observe a stale database state. The implementation is further validated with a 100-thread concurrency test.

### 2. Separating pre-orders from normal order submission

Group-buy orders are modeled as pre-orders instead of reusing the standard checkout path end-to-end. During the campaign window, each participant receives an order with status `PENDING_GROUP`, which avoids prematurely deducting stock, clearing cart state, or triggering the full delivery validation flow. Once the group succeeds, all related orders are promoted in batch to `TO_BE_CONFIRMED`; if the campaign expires, the orders are batch-cancelled and refund actions are logged in a mock payment flow. This keeps the original order module stable while isolating group-buy concerns in a clear and traceable way.

### 3. Order lifecycle rules backed by service-level tests

Order status transitions are centralized in `OrderStatusTransitionPolicy`, which defines the valid source states and target state for payment, member cancellation, admin confirmation, rejection, cancellation, delivery, and completion. Refunds are not modeled as a separate order status; a refunded order remains cancelled while `pay_status=REFUND` and payment/refund evidence carry the money-movement state. That keeps lifecycle rules out of scattered service conditionals while making the ordinary path and cancellation paths directly testable.

The backend now includes focused tests for `OrderServiceImpl`, `OrderPaymentServiceImpl`, `DemoPaymentGateway`, `EcpayPaymentGateway`, `OrderCancellationServiceImpl`, `OrderFulfillmentServiceImpl`, and `OrderStatusTransitionPolicy`. These tests cover payment request creation, cancelled and pending-group orders that cannot be paid, demo HMAC callback verification, ECPay CheckMacValue verification and callback mapping, payment event recording, duplicate payment callbacks, completed orders that cannot be cancelled, member ownership checks, unpaid rejections that should not refund, and gift-box cancellation restoring component product stock. `local-fresh-server` also produces a JaCoCo HTML report with `mvn -pl local-fresh-server -am verify`; see [docs/testing.md](docs/testing.md) for the repeatable command and report path.

Payment processing also writes a provider-neutral `payment_event` trail for `REQUEST_CREATED`, `CALLBACK_SUCCEEDED`, `CALLBACK_DUPLICATE`, and `CALLBACK_REJECTED`. Admins can query that trail through `GET /admin/paymentEvents/page` by order number, provider, event type, result, provider trade number, idempotency key, and time range. The current demo gateway still completes payments immediately for local demos, but the backend now has a `/payment/callback` provider callback boundary with demo HMAC verification and a switchable ECPay CheckMacValue parser, and the member frontend can turn an ECPay payment request into a POST form redirect. The data model already stores provider, provider reference, provider trade number, idempotency key, amount, raw payload, and processing result so a future ECPay sandbox adapter can reuse the same callback and reconciliation evidence.

The admin console also includes a Payment Events page, so demo reviewers can inspect payment requests, successful callbacks, duplicate callbacks, and rejected callbacks without calling the API manually.

The backend also exposes a minimal business-metrics slice through Actuator. It tracks payment callback outcomes, duplicate/applied order cancellations, and group-buy state transitions. This is intentionally lighter than a Prometheus/Grafana stack; see [docs/observability.md](docs/observability.md) for local query examples and current boundaries.

### 4. Admin operation audit log

Admin order confirmation, rejection, cancellation, delivery, completion, and manual product inventory adjustments now write to `admin_operation_log`. The log records the action, target type and id, before/after values, reason, operator, and timestamp, so the backend can answer who changed which business object and why. The admin API also exposes `GET /admin/operationLogs/page` for paginated filtering by action, target, operator, and time range; the admin console includes an Operation Logs page for direct review, and `AdminOperationLogApiTest` verifies pagination, filtering, and newest-first ordering from the HTTP layer. This is separate from `product_inventory_log`: inventory logs explain stock movement, while audit logs explain admin accountability.

### 5. Choosing Testcontainers over mocks for lock verification

A mocked Redis client would verify method flow, but not the real failure mode that matters here: concurrent access against an actual distributed lock. For that reason, the group-buy integration tests spin up Redis through Testcontainers and inject host and port dynamically with `@DynamicPropertySource`. This makes the 100-thread test substantially more trustworthy than a pure mock-based approach. The cost is a slightly heavier test setup, but the benefit is stronger evidence that the locking strategy behaves correctly under realistic concurrency.

### 6. JWT-based authentication with ThreadLocal request isolation

Both the admin API and member API use JWT for authentication. Incoming requests are intercepted, the token is parsed, and the current user context is stored in ThreadLocal so downstream services can access it without repeatedly threading member IDs through controller signatures. That design keeps application code clean, but it also introduces a known risk: if ThreadLocal is not cleared after request completion, data can leak across reused servlet threads. This project explicitly addresses that risk and includes regression coverage for the cleanup path.

### 7. Google OAuth 2.0 via Authorization Code Flow

The login system uses Google OAuth 2.0 Authorization Code Flow instead of the deprecated Implicit Flow. The frontend is responsible only for redirecting users to Google and receiving the callback code; the backend exchanges the code for tokens, verifies the `id_token`, and checks token claims such as `aud` and `exp`. The external Google integration is wrapped behind a `GoogleOAuthClient` abstraction, which keeps business logic testable and allows account-linking behavior to be verified independently from the Google SDK implementation details.

### 8. Clear separation between Redisson and Spring Data Redis

Redis plays two distinct roles in this system: distributed coordination for group-buy locks, and conventional KV/cache use cases such as product list caching and store status reads. Instead of forcing both concerns through the same client path, the project uses `RedissonClient` specifically for locking, while `RedisTemplate` is backed by Spring Boot’s standard Lettuce integration for general Redis access. This separation resolved a real compatibility issue around `Tuple` support and also leaves the codebase easier to reason about for future maintainers.

### 9. Built with deployment readiness in mind

Although this is a portfolio project, it is structured with deployment realism in mind rather than as a collection of disconnected demos. Database migrations are versioned, environment-specific configuration is separated cleanly, OAuth credentials are kept out of source control, and frontend/backend integration is designed around realistic local-to-cloud transitions. The deployment topology described below is the actual production setup serving the demo URL.

## Design Decisions Q&A

### Q1. Why is JWT TTL set to 2 hours?

Two hours is a pragmatic balance between security and usability. A shorter lifetime would interrupt ordinary browsing, cart, checkout, and group-buy actions too aggressively; a much longer lifetime would widen the risk window if a token were leaked. Since this platform is centered around transaction-oriented sessions rather than long-running editing workflows, a 2-hour TTL covers realistic user behavior while keeping the exposure window bounded.

### Q2. Why use Redisson instead of implementing a lock with `SETNX` manually?

A hand-rolled `SETNX` lock is possible, but getting all the details right—ownership checks, safe release, expiration handling, reentrancy, and failure semantics—adds complexity that does not meaningfully improve this project. Redisson already solves those infrastructure concerns, which allows the implementation to focus on the domain problem: join validation, campaign completion, and transactional consistency. For a portfolio project centered on engineering judgment, using a mature library is the more defensible choice.

### Q3. Why process expired group-buys with a scheduler instead of Redis TTL events?

Redis key expiration events look attractive at first, but they require `notify-keyspace-events` configuration and only offer best-effort delivery semantics. That makes them a weak fit for workflows that need stronger guarantees around database state transitions and refund logging. In this system, campaigns last 24 hours, so a once-per-minute scheduled scan is operationally sufficient while remaining easier to reason about, easier to test, and easier to maintain across environments.

### Q4. Why use Testcontainers instead of relying entirely on mocks?

Mocks are appropriate for isolating business logic, but not for validating distributed locking behavior. The point of the concurrency test is to prove that the real Redis-backed lock behaves as expected under contention. Testcontainers provides that realism in a local and reproducible way, without requiring a permanently provisioned shared Redis instance for CI. The tradeoff is heavier test execution, but the confidence gain is worth it for this part of the system.

### Q5. Why migrate the admin app to Vue 3 as well?

The admin console is an important verification surface for a backend portfolio. Without it, the repository would only demonstrate the consumer purchase path. It has therefore been migrated to Vue 3 + Vite + TypeScript + Pinia + Element Plus, covering login, list pages, forms, order operations, dashboards, and API proxy integration without expanding the project beyond its fundamentals-focused scope.

### Q6. Why keep both mock login and Google OAuth in the same repository?

Pure OAuth-only development would make local testing depend heavily on live credentials and interactive authorization. Pure mock login would be fast, but too artificial for a serious product-facing flow. The project therefore uses a dual-track strategy: Google OAuth for realistic authentication, and mock login gated by environment flags for fast development and repeatable demos. This keeps development efficient without compromising the production-facing design.

## System Requirements

- Java 17+
- Node.js 22+ (admin uses npm; user storefront can use pnpm)
- MySQL 8.x
- Redis 7.x (Docker is acceptable)
- A Google OAuth client for frontend and backend local development

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/kevinlin000/local-fresh-platform.git
cd local-fresh-platform
```

### 2. Prepare backend `application-dev.yml`

Create:

```text
backend-environment/local-fresh-backend/local-fresh-server/src/main/resources/application-dev.yml
```

At minimum, provide the following values:

```yaml
localfresh:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: local_fresh
    username: your_db_user
    password: your_db_password

  redis:
    host: localhost
    port: 6379
    database: 0

  jwt:
    admin-secret-key: your-admin-secret
    user-secret-key: your-user-secret

  aws:
    s3:
      region: your-aws-region
      access-key-id: your-access-key
      secret-access-key: your-secret-key
      bucket-name: your-bucket

  google:
    api-key: your-google-maps-api-key

  oauth:
    google:
      client-id: your-google-oauth-client-id
      client-secret: your-google-oauth-client-secret
      redirect-uri: http://127.0.0.1:5173/oauth/callback

  auth:
    mock-login-enabled: true

  shop:
    address: Taipei City Hall Rd. 1, Xinyi District, Taipei

springfox:
  documentation:
    enabled: false

knife4j:
  enable: false
```

### 3. Start local MySQL / Redis

```bash
cd backend-environment/local-fresh-backend
cp .env.example .env
docker compose up -d
```

The backend uses Flyway and automatically applies migrations from:

```text
backend-environment/local-fresh-backend/local-fresh-server/src/main/resources/db/migration/
```

For a fresh database, no manual SQL step is required. For an existing non-empty schema that does not yet have `flyway_schema_history`, start the backend once with `FLYWAY_BASELINE_ON_MIGRATE=true`, then disable that flag afterward.

If local port `3306` is already occupied, start only Redis and point the backend to your existing MySQL instance:

```bash
docker compose up -d redis
```

### 4. Start the backend

```bash
cd backend-environment/local-fresh-backend
mvn install -DskipTests
mvn -pl local-fresh-server spring-boot:run
```

The install step refreshes sibling module artifacts (`local-fresh-common`, `local-fresh-model`) in the local Maven repository before running the server module directly.

For the first Flyway baseline on a legacy database:

```bash
cd backend-environment/local-fresh-backend
mvn install -DskipTests
FLYWAY_BASELINE_ON_MIGRATE=true mvn -pl local-fresh-server spring-boot:run
```

### 5. Start the admin frontend

```bash
cd frontend-environment/local-fresh-admin
npm ci
npm run dev -- --host 127.0.0.1
```

The admin app defaults to `http://127.0.0.1:5174`, with `/api` proxied to `http://localhost:8080/admin`.

### 6. Prepare user frontend `.env.local`

Create:

```text
frontend-environment/local-fresh-user/.env.local
```

Example:

```env
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

### 7. Start the user frontend

```bash
cd frontend-environment/local-fresh-user
pnpm install
pnpm dev
```

### 8. Google OAuth setup guide

For the full frontend-side OAuth setup, see:

- [frontend-environment/local-fresh-user/README.md](frontend-environment/local-fresh-user/README.md)

## Deployment

The deployment topology is:

- **EC2** for the Spring Boot API
- **Nginx + Let's Encrypt** on EC2 for HTTPS reverse proxying from the public API domain to Spring Boot `8080`
- **Dockerized MySQL + Redis on EC2** for products, members, orders, group-buy data, cache, and locking
- **S3** for hosting frontend static assets
- **CloudFront** for CDN delivery and HTTPS entry points
- **DuckDNS** for the public backend API domain

> For detailed diagrams and request flows, see `docs/architecture.md`.

## Known Limitations

- Payment is still backed by a demo gateway by default; payment request/callback separation, demo HMAC verification, ECPay CheckMacValue parsing, member-side POST redirect, event recording, provider trade numbers, and idempotency keys are modeled, but no real ECPay sandbox end-to-end flow is included yet.
- The admin console now has a polished operations-console baseline, but it does not yet include automated visual regression coverage.
- The user frontend now covers desktop and mobile responsive basics, but does not yet include cross-browser visual regression testing.
- Legacy databases need a one-time Flyway baseline; fresh databases can apply migrations directly.

See also:
- [docs/known-issues.md](docs/known-issues.md)

## Related Documents

- [docs/known-issues.md](docs/known-issues.md)
- [docs/ecpay-sandbox-runbook.md](docs/ecpay-sandbox-runbook.md)
- [docs/observability.md](docs/observability.md)
- [docs/portfolio-roadmap.md](docs/portfolio-roadmap.md)
- [docs/testing.md](docs/testing.md)
- [docs/interview-guide.md](docs/interview-guide.md)
- [frontend-environment/local-fresh-user/README.md](frontend-environment/local-fresh-user/README.md)
- `docs/architecture.md` (system architecture and sequence diagrams)

Before switching the deployed API to ECPay sandbox, run:

```bash
scripts/check-ecpay-sandbox-readiness.sh
```

For the current completeness assessment and next-priority plan, see
[docs/portfolio-roadmap.md](docs/portfolio-roadmap.md). The project is already
interview-ready for its core Java backend story; after the local observability
slice, the next practical step is ECPay sandbox readiness verification before
full CD automation. The current public preflight passes health/Nginx checks but
still returns HTTP `404` for `/payment/callback`, so the deployed backend must
be synced before switching `PAYMENT_PROVIDER=ecpay`.

## License

This project is released under the MIT License. You are free to use, modify, and distribute it under the terms of the MIT License.
