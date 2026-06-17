# Portfolio Upgrade Progress

## 2026-06-17 17:35 +0800

- Started the next portfolio-upgrade slice after commit `d0d5f87`.
- Read `CONTEXT.md` and `docs/adr/0001-portfolio-positioning.md`.
- Created this planning set to track demo data, admin polish, bundle hygiene, evidence, and checkpoint commit.

## 2026-06-17 17:38 +0800

- Inspected backend SQL/migration layout.
- Found Flyway migrations `V1` through `V9`, with localized catalog data already present in `V5` and image URLs in `V6`.
- Recorded that Phase 1 should focus on end-to-end demo journey data and reset documentation, not duplicate the existing catalog seed.
- Read `V1__initial_schema.sql`, `V5__taiwan_localization.sql`, `docker-compose.yml`, and `application.yml`.
- Confirmed next seed should be a later migration adding member/address/cart/order/group-buy journey data.
- Read order and group-buy mapper/UI surfaces to confirm which fields need meaningful seed values.
- Found legacy S3 bucket naming in `V6__add_product_images.sql`; added to Phase 1 cleanup scope.
- Decided to use frontend static `/demo-assets/...` paths for seeded product images so user/admin deployments can serve the same deterministic demo visuals.

## 2026-06-17 17:52 +0800

- Added deterministic SVG demo assets to both user and admin frontend `public/demo-assets/`.
- Replaced legacy seeded product image URLs in `V6__add_product_images.sql` with `/demo-assets/...` paths.
- Added `V10__demo_journey_seed.sql` with demo members, addresses, carts, orders, and group-buy journeys for quick portfolio walkthroughs.
- Removed the MySQL init mount from backend `docker-compose.yml`; Flyway now owns migration ordering during app startup.
- Replaced leftover legacy logging namespace in `application.yml` with `com.localfresh`.

## 2026-06-17 18:13 +0800

- Cleaned legacy platform terms and broad simplified-Chinese residue from backend comments, admin views, report widgets, and docs.
- Deleted unused iconfont demo HTML/CSS files from the admin source tree.
- Fixed `backend-environment/local-fresh-backend/.env.example` so `MYSQL_DATABASE=local_fresh` matches `application-dev.yml`.
- Verified with Docker MySQL/Redis and Spring Boot startup that Flyway validates and applies all 11 migrations through `V10__demo_journey_seed.sql`.
- Verified seeded counts: 3 demo members, 9 demo orders, 2 demo group buys, and 3 demo carts.
- Adjusted the pending-payment demo order to be recent enough that the scheduled timeout task does not cancel it immediately.
- Updated README and user frontend README with the local demo seed story and 試用會員 login labels.

## 2026-06-17 18:41 +0800

- Replaced Fastjson usage with a shared Jackson-backed `JsonUtil` and removed Fastjson from Maven dependencies.
- Replaced the legacy datasource pool with Spring Boot's default HikariCP configuration and updated docs accordingly.
- Converted admin/user Element Plus usage away from avoidable full-bundle output:
  - Admin uses Element Plus component resolvers and explicit heavy component chunk groups.
  - User manually registers the actual Element Plus components used by the app and imports services from subpath modules.
- Verified frontend builds:
  - Admin build passes; largest JS chunk is `ep-table` at about 266 kB.
  - User build passes; `element-plus` JS chunk is about 202 kB.
- Verified backend with `mvn test`: 100 tests, 0 failures/errors, 5 skipped external/manual tests.
- Verified Spring Boot startup with HikariCP and Flyway V1-V10 on local Docker MySQL/Redis.
- Ran hard legacy-term scans with no matches for old project names, old login provider names, old cloud-provider names, legacy JSON/pool dependencies, or old icon CDN residue.

## 2026-06-17 18:52 +0800

- Started Phase 6 browser acceptance pass after commit `617ea65`.
- Goal: run the backend, user frontend, and admin frontend locally; inspect real flows with Playwright; fix only high-impact UI/UX or flow blockers found during the walkthrough.
- Docker MySQL and Redis are already healthy.
- Started local services:
  - Backend on port 8080 with Flyway V1-V10 validated.
  - User frontend at `http://127.0.0.1:5173/`.
  - Admin frontend at `http://127.0.0.1:5176/` because 5174/5175 were already occupied.

## 2026-06-17 18:59 +0800

- Fixed the user homepage initial catalog load so the valid `全部商品` state (`categoryId = null`) still loads products.
- Rechecked the browser with Playwright:
  - User homepage calls `/api/user/product/list`.
  - Product grid renders `29 / 29` available products.
  - Browser console reports 0 errors.
- Added `.playwright-cli/` to `.gitignore` because Playwright acceptance snapshots are local test output.

## 2026-06-17 19:13 +0800

- Continued Phase 6 browser acceptance through the full member and admin order lifecycle.
- Verified member flow in Playwright:
  - Product detail page loads from `/api/user/product/{id}`.
  - Cart add/list works and checkout dialog loads saved addresses.
  - Order submission succeeds in dev after disabling delivery range checks by default.
  - Simulated payment moves the new order to `待確認`.
  - Member order history shows the submitted order and final admin-confirmed `已確認` status.
- Fixed checkout/dev blockers:
  - Added `delivery.range-check-enabled: ${DELIVERY_RANGE_CHECK_ENABLED:false}` to dev config and example config.
  - Updated README/perf docs to require `mvn install -DskipTests` before running `local-fresh-server` directly, avoiding stale sibling-module artifacts.
  - Made user API error handling surface backend `msg`/`message` payloads.
  - Replaced deprecated Element Plus cart radio `label` value binding with `value`.
- Verified admin flow:
  - Admin login with seeded local credentials works.
  - Dashboard, orders, products, and reports pages load real seeded data with clean console output.
  - Admin order confirmation sends `PUT /api/order/confirm`, updates statistics, and changes the member order to `已確認`.
- Fixed admin order action handling so Element Plus confirm cancel/close does not leave unhandled Vue console errors, and API failures are shown through `ElMessage`.
- Fixed admin mobile layout by replacing the fixed desktop sidebar grid with a top horizontal nav under 900px and removing the global body min-width at mobile sizes.
- Verified responsive screenshots:
  - User mobile home and orders are readable at 390x844.
  - Admin mobile dashboard and products are readable at 390x844 after the layout fix.
- Verification:
  - `npm run build` passes for user frontend.
  - `npm run build` passes cleanly for admin frontend after filtering only the known `@vueuse/core` Rolldown `INVALID_ANNOTATION` dependency warning through `onLog`.
  - `mvn test` passes for backend: 100 tests, 0 failures, 0 errors, 5 skipped.
  - Legacy-term scan reports no matches for old project/login/map/cloud provider terms outside generated build artifacts.
