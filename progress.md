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

## 2026-06-17 19:34 +0800

- Started Phase 7 after user confirmation to improve frontend visual quality.
- Re-audited member `App.vue`/`HomeView.vue` and admin `vue3-admin.scss`/`DashboardView.vue`.
- Decided to keep the existing Vue/Element Plus stack and focus on a product-grade visual pass:
  - Member side: replace template-like marketing hero with shopping-first storefront context, denser filters, cleaner product cards, and a less repetitive color/card system.
  - Admin side: reduce decorative gradients and oversized cards in favor of a quieter operations-console feel with clearer status hierarchy.

## 2026-06-17 23:57 +0800

- Completed Phase 7 frontend visual pass.
- Member frontend changes:
  - Reworked home into a shopping-first storefront with market context, delivery facts, integrated group-buy module, stronger catalog toolbar, cleaner category rail, and product/gift-box cards with commerce metadata.
  - Aligned product detail, cart, and orders pages with the new visual language by reducing heavy shadows, removing uppercase/pill-heavy treatment, and using clearer shopping-flow headings.
- Admin frontend changes:
  - Reworked global admin shell toward an operations-console look: white sidebar, subtle borders, less decorative color, denser dashboard cards, and calmer toolbars.
  - Aligned product/order management summary cards with the dashboard style.
  - Fixed product management table usability at 1280px so all row actions remain visible instead of clipping off the right edge.
- Verification:
  - `npm run build` passes for member frontend.
  - `npm run build` passes for admin frontend.
  - Playwright screenshots checked member home/product detail/cart/orders, admin dashboard/products/orders, plus mobile member/admin layouts.
  - Browser console checks reported 0 errors for member and admin sessions.
  - Legacy-term scan reported no matches for old project/login/map/cloud provider terms in source/planning files checked in this phase.

## 2026-06-18 00:10 +0800

- Started Phase 8 after user confirmation.
- Confirmed working tree is clean on `hardening-and-upgrade`, ahead of remote by 7 commits.
- Confirmed local services are still running:
  - Backend on `http://127.0.0.1:8080`
  - Member frontend on `http://127.0.0.1:5176`
  - Admin frontend on `http://127.0.0.1:5177`
- Identified documentation drift: README screenshots and copy still describe the pre-Phase-7 storefront and do not show the improved admin operations console.

## 2026-06-18 00:24 +0800

- Completed the Phase 8 portfolio evidence pass.
- Refreshed README screenshots and demo copy to cover 9 reviewer-facing screens:
  - member home, product list, product detail, group-buy detail, cart, and orders
  - admin dashboard, product management, and order management
- Added `docs/interview-guide.md` with a 30-second pitch, 5-minute demo route, backend/full-stack talking points, common interview questions, and honest limitations.
- Updated `docs/known-issues.md` so current limitations are explicit: mock payment, no automated visual-regression suite yet, portfolio-grade AWS deployment, and dev-disabled delivery range checks.
- Updated `docs/testing.md` with the current manual browser acceptance approach and screenshot evidence scope.
- Verification:
  - `npm run build` passes for member frontend.
  - `npm run build` passes for admin frontend.
  - README screenshot references point to the committed `docs/screenshots/01-09` PNG files.
  - Legacy-term scan found no old course/platform/login/map/cloud provider terms; the only `Fastjson` matches are historical notes saying it was removed.

## 2026-06-20 13:38 +0800

- Started Phase 9 after user asked to push the portfolio toward 100% completeness for interview readiness.
- Scope for this slice: refresh screenshot evidence and documentation checks after restoring the user's real food PNG assets.
- Loaded the planning-with-files and Playwright skills.
- Confirmed `npx` is available for Playwright CLI workflows.
- Confirmed working tree was clean on `hardening-and-upgrade` before starting Phase 9.
- Screenshot attempt found a real demo-data freshness issue: `GB-DEMO-ACTIVE` had been marked failed by the scheduled expiry task because the seed was created on 2026-06-17.
- Added a new migration plan for `V16__refresh_demo_group_buy_window.sql` and a frontend countdown display tweak so longer demo windows render as days instead of hundreds of hours.
- Added `V16__refresh_demo_group_buy_window.sql`; local Flyway startup applied it successfully and advanced the schema to v16.
- Refreshed all 9 committed screenshots under `docs/screenshots/` at 1280x720:
  - member home, product list, product detail, group-buy detail, cart, and orders
  - admin dashboard, product management, and order management
- Visual inspection verified:
  - `01-home.png` is the top storefront view and shows real PNG product cards.
  - `04-group-buy.png` shows real pork imagery, `揪團中`, `29 天 23 小時`, and `2/3` progress.
  - `09-admin-orders.png` shows group-buy preorders and admin fulfillment actions without obvious clipping.
- Verification:
  - Member frontend `npm run build` passed.
  - Admin frontend `npm run build` passed.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 135 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
  - `git diff --check` passed.

## 2026-06-20 15:09 +0800

- Started Phase 10 backend boundary test hardening after the real-image evidence refresh.
- Reviewed order lifecycle test coverage:
  - Payment, cancellation, status transition, and audit-log service tests are already strong.
  - Group-buy expiry behavior already has Redis/Testcontainers integration coverage.
  - The focused gap is missing-order guard behavior in `OrderFulfillmentServiceImplTest`.
- Added service tests for missing-order handling in admin confirm, delivery, completion, and member reminder flows.
- Updated `docs/testing.md` so the existing `GroupBuyExpirationServiceTest` coverage is visible in the portfolio testing evidence.
- Verification:
  - Focused `OrderFulfillmentServiceImplTest` passed with 12 tests.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 139 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
  - `git diff --check` passed.

## 2026-06-20 15:15 +0800

- Started Phase 11 product admin boundary coverage.
- Reviewed product/order submission service tests and found the highest-value gap in `ProductServiceImplTest`:
  - Existing coverage proved manual inventory adjustment audit logging.
  - Missing coverage: product deletion guards, successful delete side effects, product-disable gift-box cascading, and cache invalidation.
- Added unit tests for enabled-product deletion rejection, gift-box reference deletion rejection, successful disabled-product deletion, disabling related gift boxes, and enabling a product without touching gift boxes.
- Updated `docs/testing.md` and planning findings to include the product-management boundary coverage.
- Verification:
  - Focused `ProductServiceImplTest` passed with 6 tests.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 144 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
  - `git diff --check` passed.

## 2026-06-20 15:29 +0800

- Started Phase 12 frontend first-impression polish after the user asked whether the UI/UX still feels weak or too AI-generated.
- Reviewed current plan/progress/findings and confirmed the branch was clean before this phase.
- Used external reference direction from mature grocery commerce, admin tooling, and dashboard design patterns:
  - Grocery storefronts should make search, category browsing, replenishment shelves, and next cart action obvious.
  - Admin dashboards should prioritize actionable queues and meaningful operational metrics.
  - Metrics should lead to a next action rather than exist as decorative numbers.
- Implemented the first polish pass:
  - Member home now has a weekly replenishment shelf and recommended sorting that prioritizes everyday meal-building items.
  - Group-buy detail now emphasizes progress, remaining members, and the join/share decision path.
  - Added `V17__refresh_demo_dashboard_activity.sql` so seeded dashboard activity shows today's completed, confirmed, pending, canceled, and new-member data through existing APIs.
- Verification:
  - Member frontend `npm run build` passed.
  - Admin frontend `npm run build` passed.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 144 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
  - Local backend startup applied Flyway v17 successfully.
  - Browser acceptance verified member home, group-buy detail, admin dashboard, and admin orders with clean latest console output.
  - Refreshed `docs/screenshots/01-home.png`, `02-product-list.png`, `04-group-buy.png`, and `07-admin-dashboard.png`.
  - `git diff --check` passed.
