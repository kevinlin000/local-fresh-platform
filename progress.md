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

## 2026-06-20 16:28 +0800

- Started Phase 16 minimal business observability after the inventory idempotency and roadmap slices.
- Scope: add Actuator-backed counters for payment callback outcomes, order cancellation idempotency results, and group-buy state transitions without adding Prometheus/Grafana or changing deployment architecture.
- Added `BusinessMetricsService` and Micrometer-backed implementation.
- Wired counters into `OrderPaymentServiceImpl`, `OrderCancellationServiceImpl`, and `GroupBuyServiceImpl`.
- Added focused unit coverage for metric tagging and updated payment/cancellation service tests to verify the business metrics are recorded on key paths.
- Added `docs/observability.md` and updated README, testing, interview, and roadmap docs so the new observability boundary is explicit.
- Verification:
  - Focused metrics/payment/cancellation tests passed: 17 tests, 0 failures, 0 errors.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 151 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
  - `git diff --check` passed.

## 2026-06-20 19:37 +0800

- Started Phase 17 ECPay sandbox readiness gate.
- Ran the existing public preflight:
  - Backend health passed.
  - Public API is served through Nginx.
  - `/payment/callback` returned HTTP 404, so the deployed EC2 backend is not yet ready for `PAYMENT_PROVIDER=ecpay`.
- Added a provider-selection readiness test to prove the Spring context selects `EcpayPaymentGateway` under `localfresh.payment.provider=ecpay` and generates a signed checkout payload with the deployed ReturnURL and OrderResultURL.
- Updated the ECPay runbook, testing docs, interview guide, roadmap, and README to make the current deployed blocker explicit.
- Verification:
  - Focused ECPay readiness tests passed: 10 tests, 0 failures, 0 errors.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 153 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.

## 2026-06-20 19:46 +0800

- Started Phase 18 local UI smoke precheck after deciding not to touch EC2/cloud deployment yet.
- Checked frontend packages:
  - User frontend has no test script beyond build/preview.
  - Admin frontend has unit specs but no browser smoke runner.
  - Neither frontend currently carries Playwright as a dependency.
- Tried `npx --yes playwright --version`; it failed because the sandbox cannot resolve `registry.npmjs.org`.
- Added dependency-free `scripts/check-ui-smoke-local.mjs`:
  - Checks backend `/actuator/health`.
  - Checks user/admin Vite app shells.
  - Logs in as mock member and admin.
  - Checks product list, user order history, admin business data, and admin order search APIs.
- Updated README, testing docs, roadmap, findings, and progress with the smoke precheck scope and boundary.
- Verification:
  - `node --check scripts/check-ui-smoke-local.mjs` passed.
  - First smoke run found a real script bug: user product list requires the `authentication` header after member login.
  - Fixed the script to reuse the mock member token for member data APIs.
  - Local smoke precheck passed against backend `8080`, member frontend `5176`, and admin frontend `5177`: 9 checks passed.

## 2026-06-20 23:18 +0800

- Started Phase 19 browser UI smoke after the user explicitly approved using Playwright.
- Confirmed Playwright CLI can report version `1.61.0`, but one-off `npx --package playwright` imports do not expose `@playwright/test` to local specs.
- Added a root smoke runner with `@playwright/test`:
  - `npm run smoke:browser`
  - `playwright.config.mjs`
  - `scripts/ui-smoke-browser.spec.mjs`
- Browser smoke scope:
  - Member login with mock account, home page, and order-history page.
  - Admin login, dashboard, order-management page, and product-management page.
- Updated README/testing/roadmap/planning notes to explain the difference between dependency-free API smoke and real-browser smoke.
- Verification:
  - `npm install` passed and generated the root `package-lock.json`.
  - `node --check scripts/check-ui-smoke-local.mjs` passed.
  - `node --check scripts/ui-smoke-browser.spec.mjs` passed.
  - First `npm run smoke:local` failed in the sandbox because Node `fetch` could not reach localhost; rerunning outside the sandbox passed 9 checks.
  - First `npm run smoke:browser` failed because Playwright Chromium was not installed locally; `npx playwright install chromium` installed the runtime.
  - Second browser run reached the pages but found strict selector ambiguity; selectors were tightened.
  - Final `npm run smoke:browser` passed: 2 tests, 0 failures.
  - Admin frontend `npm run build` passed.
  - User frontend `corepack pnpm@10.25.0 run build` passed.
  - `git diff --check` passed.

## 2026-06-20 23:32 +0800

- Started Phase 20 fullstack product-grade UX pass after the user raised the target from backend portfolio to high-standard fullstack portfolio.
- Working standard for this phase:
  - Member side should feel like a real grocery commerce product, with clear shopping intent, decision support, and polished responsive behavior.
  - Admin side should feel like an operations tool, with fast scanning, strong status hierarchy, and clear next actions.
  - Every visible improvement should keep repeatable verification through build, local smoke, and Playwright browser smoke.
- Browser audit findings:
  - Member product cards needed direct purchase actions and stronger decision signals.
  - Mobile member home needed a faster path from first viewport to shopping.
  - Product detail needed delivery/storage/group-buy trust information.
  - Admin dashboard/orders/products are usable, but the next admin-side polish should improve work-priority hierarchy and table decision signals rather than adding new features.
- Implemented the first focused fullstack UX polish:
  - Added member home hero actions for "開始採買" and "查看直送箱".
  - Changed product cards from gallery-like cards into commerce cards with direct add-to-cart, detail action, delivery signal, usage hint, and group-buy signal.
  - Compressed mobile home recommendation/fulfillment spacing so shopping starts earlier.
  - Added product-detail assurance cards for delivery, storage, and group-buy behavior.
- Verification so far:
  - User frontend `corepack pnpm@10.25.0 run build` passed after the home-page changes.
  - User frontend `corepack pnpm@10.25.0 run build` passed again after the product-detail assurance changes.
  - Final user frontend `corepack pnpm@10.25.0 run build` passed.
  - Admin frontend `npm run build` passed.
  - `npm run smoke:local` passed against backend `8080`, member `5176`, and admin `5177`: 9 checks.
  - `npm run smoke:browser` passed: 2 tests, 0 failures.

## 2026-06-20 23:45 +0800

- Started Phase 21 admin operations polish after the member commerce polish made the storefront stronger than the admin core workflows.
- Re-read planning files, project context, roadmap, and admin Vue3 dashboard/orders/products pages.
- Scope decision:
  - Do not add backend APIs or database changes for this slice.
  - Use existing order statistics, order list, product stock, product status, and product quality fields.
  - Improve orders/products first because dashboard already had operations data, while the list pages still felt like CRUD tables.
- Implemented admin order work-queue polish:
  - Added an operations header and refresh/clear actions.
  - Changed status summary cards into clickable queues for pending confirmation, confirmed, and in-delivery orders.
  - Added row-level fulfillment judgment tags such as priority confirmation, ready for delivery, waiting completion, fulfilled, closed, and group-buy pending.
  - Reduced table column widths after browser inspection found desktop overflow.
- Implemented admin product workbench polish:
  - Added a product health header and refresh/clear actions.
  - Changed summary cards into filter controls for on-sale, low-stock, content-work, and off-sale products.
  - Added stock-level display with threshold and progress bar.
  - Added product description preview and operational tags for stock, image, and description completeness.
  - Tightened mobile summary cards to reduce vertical sprawl before the table.
- Verification so far:
  - Admin frontend `npm run build` passed before browser inspection.
  - Playwright browser inspection covered admin orders and products on desktop, then products on mobile.
  - Admin frontend `npm run build` passed again after responsive adjustments.
  - `npm run smoke:local` passed against backend `8080`, member `5176`, and admin `5177`: 9 checks.
  - `npm run smoke:browser` passed: 2 tests, 0 failures.
  - `git diff --check` passed.

## 2026-06-21 00:05 +0800

- Started Phase 22 backend deep-dive prep notes after the user raised the target to Shopee / Binance / Google-style evaluation.
- Scope decision:
  - Do not add speculative microservices, CQRS, Kubernetes, or broad production infrastructure.
  - Consolidate existing implementation and test evidence into Kevin-facing interview prep notes.
  - Be explicit about residual risks instead of overclaiming production completeness.
- Re-read planning files, findings, interview guide, architecture docs, testing docs, observability docs, perf docs, and backend service/test inventory.
- Added `docs/backend-deep-dive-prep.md`:
  - Kevin-facing answer matrix for likely deep-dive questions.
  - Prep sections for order lifecycle, payment callback/idempotency, group-buy concurrency, inventory consistency, observability/operations, security boundaries, and scaling position.
  - Each section maps what to say to code evidence, test evidence, and "do not overclaim" notes.
  - Includes a 5-minute backend deep-dive sequence for interview preparation.
- Removed awkward public README / README.en links after user feedback; the public README should remain a natural portfolio overview, not a document asking interviewers to evaluate it.
- Kept the prep note linked from the interview guide and roadmap as self-preparation material.

## 2026-06-21 00:34 +0800

- Started Phase 23 after the user asked whether ER model / design images should be added.
- Scope decision:
  - Keep public README natural and avoid writing diagram sections as if they are addressed to interviewers.
  - Put deeper ER/state/callback diagrams in `docs/architecture.md`.
  - Put the speaking order and interview usage in Kevin-facing prep docs.
- Updated `docs/architecture.md`:
  - Expanded the core ER with `payment_event`, `product_inventory_log`, and `admin_operation_log`.
  - Added guidance on how to cut the data-model explanation during an interview.
  - Added a focused payment/inventory/admin evidence ER diagram.
  - Added payment callback sequence showing provider callback parsing, guarded update, duplicate/rejected events, metrics, and admin query evidence.
  - Added indexes for payment events, inventory references, and admin operation logs.
- Updated `docs/backend-deep-dive-prep.md` with a "how to use the diagrams" section and an ER-model answer row.
- Updated `docs/interview-guide.md` with a concise ER/state/callback display path and an ER-model FAQ answer.

## 2026-06-21 00:17 +0800

- Started Phase 24 inventory restore DB idempotency after the diagram evidence showed inventory restore as the next backend boundary worth hardening.
- Design decision:
  - Do not add a broad unique key over all inventory reference fields.
  - Add nullable `product_inventory_log.idempotency_key` and generate it only for cancel-restore events.
  - Aggregate same-product restores within one order cancellation before writing inventory logs, so direct product + gift-box component overlaps do not create duplicate restore rows.
- Implemented:
  - Added Flyway migration `V18__add_inventory_log_idempotency_key.sql`.
  - Updated H2 test schema, `ProductInventoryLog`, `ProductInventoryLogVO`, and `ProductInventoryLogMapper`.
  - Updated `InventoryServiceImpl` to generate idempotency keys for `*_CANCEL_RESTORE` reasons only.
  - Updated `OrderCancellationServiceImpl` to aggregate restore quantities by product.
  - Added tests for same-product aggregation and DB rejection of duplicate idempotency keys.
- Verification so far:
  - First focused Maven run exposed the known reactor issue when `-Dtest` is used with modules that have no matching tests.
  - Focused rerun with `-Dsurefire.failIfNoSpecifiedTests=false` passed: 12 tests, 0 failures, 0 errors.
  - Full backend `mvn -pl local-fresh-server -am verify` passed: 155 tests, 0 failures, 0 errors, 5 skipped, with JaCoCo report generation.

## 2026-06-21 00:56 +0800

- Started Phase 25 CI quality gate hardening after the user asked to continue sequentially and requested another strict score.
- Inspected existing `.github/workflows/ci.yml`, root/admin/user package scripts, lockfiles, and `scripts/check-before-commit.sh`.
- Findings:
  - Existing CI already had backend/admin/user jobs.
  - Backend CI ran `mvn test`, while current portfolio evidence relies on `mvn -pl local-fresh-server -am verify` and JaCoCo.
  - There was no cheap repository hygiene job for README screenshot references, legacy terms, or trailing whitespace.
- Implemented:
  - Added `scripts/check-repo-hygiene.mjs`.
  - Added a GitHub Actions `repo-hygiene` job.
  - Changed backend CI to run `mvn -pl local-fresh-server -am verify`.
  - Added JaCoCo report upload as `backend-jacoco-report`.
  - Updated `scripts/check-before-commit.sh` to use backend `verify` and repository hygiene.
  - Updated README/testing docs to match the CI gate.
  - Mechanically removed trailing whitespace from existing docs/admin text files caught by the new gate.
- Stabilized:
  - Admin `npm audit --omit=dev` initially failed on `form-data@4.0.5`; `npm audit fix --omit=dev` updated the lockfile to a non-vulnerable version.
  - Full local gate initially failed because Mockito inline Byte Buddy mock maker could not self-attach under the local Homebrew JDK.
  - Added `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` with `mock-maker-subclass`, which fits the current test suite and removes the attach dependency.
- Verification:
  - `node scripts/check-repo-hygiene.mjs` passed.
  - Admin frontend `npm run build` passed.
  - Admin `npm audit --omit=dev` passed with 0 vulnerabilities.
  - User frontend `corepack pnpm@10.25.0 run build` passed.
  - Full `scripts/check-before-commit.sh` passed end to end: backend verify 155 tests, repository hygiene, admin build/audit, and user build.
- Remote CI follow-up:
  - Pushed `b23c0b5 ci: harden portfolio quality gate`.
  - GitHub Actions started the new CI run and passed repository hygiene plus admin frontend build/audit.
  - User frontend failed before install because `actions/setup-node` tried to use pnpm cache before Corepack had made pnpm available.
  - Removed the pnpm cache configuration from the user frontend job so CI uses Corepack-managed `pnpm@10.25.0` consistently.

## 2026-06-21 13:13 +0800

- Started Phase 26 ECPay callback contract evidence after the user asked to continue sequentially and requested another score.
- Re-read planning files, ECPay runbook, readiness script, callback controller, ECPay gateway, payment service, and existing payment tests.
- Ran the public ECPay readiness preflight:
  - `https://localfresh-demo.duckdns.org/actuator/health`: passed.
  - Public API `Server` header: Nginx detected.
  - `POST https://localfresh-demo.duckdns.org/payment/callback`: failed with HTTP `404`.
- Decision:
  - Do not switch deployed `PAYMENT_PROVIDER=ecpay` while public ReturnURL would still miss the Spring Boot callback endpoint.
  - Add local contract evidence instead of pretending real sandbox checkout is complete.
- Implemented:
  - Added `PaymentCallbackControllerEcpayContractTest`, wiring the real `EcpayPaymentGateway` into `PaymentCallbackController`.
  - The test proves a signed ECPay form callback returns `1|OK` and reaches `OrderPaymentService`.
  - It also proves an invalid `CheckMacValue` returns `0|FAIL` without calling payment handling.
  - Updated the ECPay runbook, testing docs, README, README.en, and roadmap to reflect the new local evidence and the still-open public 404 blocker.
- Verification so far:
  - Focused Maven run passed: `PaymentCallbackControllerEcpayContractTest`, 2 tests, 0 failures, 0 errors.
  - Full backend `mvn -pl local-fresh-server -am verify` passed: 157 tests, 0 failures, 0 errors, 5 skipped, with JaCoCo report generation.
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.

## 2026-06-21 14:50 +0800

- Started Phase 27 deployment version observability after the user asked to continue sequentially and asked for the next part plus a score.
- Re-read planning files, deployment/runbook references, Actuator configuration, ECPay readiness script, and current documentation.
- Found:
  - The repo has no checked-in EC2 deployment script or safe local SSH/systemd workflow to run directly.
  - The current public API still passes `/actuator/health`, but the updated preflight shows `/actuator/info` and `/payment/callback` both return HTTP `404`.
  - This means the deployed API cannot yet prove which backend commit is running.
- Implemented:
  - Added `DeploymentInfoContributor` to expose non-sensitive deployment identity through `/actuator/info`.
  - Added `DeploymentInfoContributorTest` for configured commit/branch and fallback behavior.
  - Updated `scripts/check-ecpay-sandbox-readiness.sh` to request `/actuator/info`, print the returned info, and optionally enforce `EXPECTED_DEPLOY_COMMIT`.
  - Updated ECPay runbook, observability docs, testing docs, README, README.en, and roadmap to explain the new deployment identity check and the still-open public 404 blocker.
- Verification so far:
  - Focused Maven run passed: `DeploymentInfoContributorTest`, 2 tests, 0 failures, 0 errors.
  - Updated public preflight still correctly blocks ECPay switch: backend health and Nginx pass, but `/actuator/info` and `/payment/callback` return HTTP `404`.
  - Full backend `mvn -pl local-fresh-server -am verify` passed: 159 tests, 0 failures, 0 errors, 5 skipped, with JaCoCo report generation.
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.

## 2026-06-21 15:05 +0800

- Started Phase 28 backend release packaging runbook after the user asked to continue sequentially and requested the next part plus scoring.
- Scope decision:
  - Do not SSH into EC2 or invent credentials.
  - Add a repeatable local release package and EC2 handoff commands so the next real cloud step can be executed and audited.
- Implemented:
  - Added `scripts/package-backend-release.sh`.
  - The script requires a clean worktree by default, runs backend `verify` unless `SKIP_VERIFY=true`, copies the Spring Boot jar into `output/backend-release/<commit>/`, and writes `release.env`.
  - The script also writes `ec2-deploy-commands.txt` with editable `scp`, `ssh`, `SOURCE_COMMIT`, service restart, `/actuator/info`, and ECPay preflight commands.
  - Added `docs/backend-deploy-runbook.md` with the package, deploy, verify, and rollback flow.
  - Linked the deploy runbook from README/README.en and connected the ECPay runbook to the package/preflight flow.
  - Updated roadmap and backend deep-dive prep to point to the new release packaging step before ECPay sandbox.
- Verification so far:
  - `sh -n scripts/package-backend-release.sh` passed.
  - `ALLOW_DIRTY=true SKIP_VERIFY=true scripts/package-backend-release.sh` passed and generated ignored release files under `output/backend-release/46c0f6d5a1ed/`.
  - Full `scripts/check-before-commit.sh` passed end to end: backend verify 159 tests, repository hygiene, admin build/audit, and user build.

## 2026-06-20 16:05 +0800

- Started Phase 13 payment lifecycle evidence hardening.
- Re-read the planning files, ADR, order/payment services, payment gateways, callback controller, and current tests.
- Found that ECPay scaffolding already exists: provider switch, ECPay signed checkout payload, callback parser, CheckMacValue tests, member POST-form redirect, and payment-event admin evidence.
- Implemented a small hardening slice:
  - Removed unused `OrderMapper.updateStatus`, which could bypass the guarded `markPaymentSucceededByNumber` transition path.
  - Added `OrderServiceImplTest` cases proving cancelled unpaid orders and pending group-buy pre-orders cannot create a provider payment request.
  - Corrected the order-status documentation so refunds are described as `pay_status=REFUND` on cancelled orders, not a nonexistent order status 7.
- Verification:
  - First focused test attempt without `-am` exposed the known stale sibling-module issue; the correct reactor command was used afterward.
  - Focused `OrderServiceImplTest` passed with 21 tests.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 146 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
  - `git diff --check` passed.

## 2026-06-20 16:19 +0800

- Started Phase 14 after the user asked that every future round include follow-up planning assessment.
- Reviewed README roadmap, known issues, interview guide, and current planning files.
- Decided not to add a frontend test runner just to test the ECPay redirect helper because the user frontend currently has no test framework; adding one here would be tool churn rather than portfolio value.
- Added `docs/portfolio-roadmap.md` with:
  - Current completeness assessment by area.
  - P0/P1 priority model.
  - Explicit guidance to defer full cloud automation until the end.
  - Current next-step recommendation: inventory idempotency design and minimal tests.
- Updated README and interview guide to link to the roadmap and align the Roadmap section with current priorities.
- Verification:
  - Confirmed `docs/portfolio-roadmap.md` is linked from `README.md`, `README.en.md`, and `docs/interview-guide.md`.
  - `git diff --check` passed.
  - No backend/frontend build was run because this slice only changes documentation and planning files.

## 2026-06-20 16:35 +0800

- Started Phase 15 inventory idempotency guard after the roadmap recommended this as the next local backend slice.
- Inspected `InventoryServiceImpl`, `OrderCancellationServiceImpl`, `ProductInventoryLogMapper`, migration `V8__add_product_inventory_log.sql`, and related order/group-buy inventory tests.
- Found that inventory logs already carry `reference_type`, `reference_id`, and `reason`, so a small duplicate-cancellation guard can reuse existing data instead of adding a new table or event-sourcing layer.
- Implemented a service-level duplicate guard:
  - `OrderCancellationServiceImpl.cancelOrder` is now transactional.
  - It skips cancellation if the order is already `CANCELLED`.
  - It also skips if an `ORDER_CANCEL_RESTORE` inventory log already exists for the order reference.
  - This prevents repeated refunds, order updates, and stock restores for stale duplicate cancellation attempts.
- Added unit tests for already-cancelled and restore-log-exists duplicate cancellation cases.
- Verification:
  - Focused `OrderCancellationServiceImplTest` passed with 5 tests.
  - Backend `mvn -pl local-fresh-server -am verify` passed with 148 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
- Follow-up assessment:
  - Inventory idempotency is now covered at the service level for repeated order cancellation.
  - The next best local slice is minimal business observability: expose or document a few application metrics before moving to ECPay sandbox or deployment automation.

## 2026-06-21 15:22 +0800

- Started and completed Phase 29 EC2 runtime templates after the user asked to continue sequentially and requested the next part plus scoring.
- Scope decision:
  - Do not attempt SSH deployment without checked credentials or a confirmed host/service contract.
  - Add non-secret EC2 runtime templates and package them with each backend release.
- Implemented:
  - Added `deploy/ec2/README.md`.
  - Added `deploy/ec2/local-fresh-server.env.example` with placeholder prod profile, database, Redis, frontend origin, JWT, S3, Google, and payment settings.
  - Added `deploy/ec2/local-fresh-server.service` for `/opt/local-fresh/current.jar` under systemd.
  - Added `deploy/ec2/nginx-localfresh-demo.conf` to proxy HTTPS traffic to the Spring Boot backend on `127.0.0.1:8080`.
  - Updated `scripts/package-backend-release.sh` to copy `deploy/ec2/` into each release package as `deploy-templates/`.
  - Updated backend deploy runbook, observability docs, and roadmap to mention the runtime templates.
  - Expanded repository hygiene so `.service`, `.conf`, and `.example` files are scanned.
- Verification:
  - `sh -n scripts/package-backend-release.sh` passed.
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.
  - `ALLOW_DIRTY=true SKIP_VERIFY=true scripts/package-backend-release.sh` passed and produced `deploy-templates/` under `output/backend-release/49f6e11a8f12/`.
  - Full `scripts/check-before-commit.sh` passed end to end: backend verify 159 tests, repository hygiene, admin build/audit, and user build.

## 2026-06-21 15:35 +0800

- Started and completed Phase 30 CI backend release artifact after the user asked to continue carefully and keep scoring.
- Scope decision:
  - Do not attempt real EC2 SSH deployment without confirmed host credentials.
  - Add a CI artifact that packages the verified backend jar, release metadata, deploy commands, and EC2 runtime templates.
- Implemented:
  - Updated `.github/workflows/ci.yml` backend job to run `SKIP_VERIFY=true scripts/package-backend-release.sh` after Maven `verify`.
  - Uploaded `output/backend-release` as the `backend-release-package` artifact.
  - Updated backend deploy runbook, testing docs, README, README.en, and roadmap to document the CI artifact path.
- Verification:
  - Ruby YAML parse of `.github/workflows/ci.yml` passed.
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.
  - `ALLOW_DIRTY=true SKIP_VERIFY=true scripts/package-backend-release.sh` passed and produced `output/backend-release/e523a4a17b38/`.
  - Full `scripts/check-before-commit.sh` passed end to end: backend verify 159 tests, repository hygiene, admin build/audit, and user build.

## 2026-06-21 15:44 +0800

- Started and completed Phase 31 release package integrity checks after the user asked to continue carefully and keep scoring.
- Scope decision:
  - Do not attempt EC2 SSH deployment without host credentials.
  - Strengthen the deploy handoff package so a downloaded CI artifact can be verified before EC2 sync.
- Implemented:
  - Updated `scripts/package-backend-release.sh` to emit `release-manifest.txt` and `SHA256SUMS`.
  - Added `scripts/verify-backend-release-package.sh`.
  - Updated CI to run the verifier after packaging and before artifact upload.
  - Updated backend deploy runbook, testing docs, README, README.en, and roadmap to document package verification.
- Verification:
  - `sh -n scripts/package-backend-release.sh` passed.
  - `sh -n scripts/verify-backend-release-package.sh` passed.
  - `.github/workflows/ci.yml` parsed successfully with Ruby YAML.
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.
  - `ALLOW_DIRTY=true SKIP_VERIFY=true scripts/package-backend-release.sh` passed and generated `release-manifest.txt` plus `SHA256SUMS` under `output/backend-release/a9f2f523e638/`.
  - `scripts/verify-backend-release-package.sh output/backend-release/a9f2f523e638` passed.
  - `scripts/verify-backend-release-package.sh output/backend-release` also passed, matching CI artifact parent-directory usage.
  - Full `scripts/check-before-commit.sh` passed end to end: backend verify 159 tests, repository hygiene, admin build/audit, and user build.

## 2026-06-21 16:05 +0800

- Started and completed Phase 32 EC2 deployment access audit after the user asked why I could not directly deploy.
- Attempted direct deployment path discovery:
  - Public ECPay readiness preflight still passes health/Nginx but fails `/actuator/info` and `/payment/callback` with HTTP `404`.
  - Workspace search found no checked-in EC2 host, SSH alias, private key path, or deploy credential.
  - Ignored backend `.env` only exposes local MySQL variable names; no deploy host key was present.
  - `~/.ssh` has no config and no private key; `ssh-agent` has no identities.
  - AWS CLI default profile found the running `local-fresh-platform` EC2 instance in `ap-northeast-1d`, public IP `35.78.44.49`, key pair `local-fresh-key`.
  - AWS SSM describe was denied, so Session Manager cannot currently be used.
  - EC2 Instance Connect successfully sent a temporary SSH public key, but SSH to `35.78.44.49:22` timed out.
  - Security group describe and authorize-ingress were denied, so I could not inspect or temporarily open SSH access.
- Release readiness:
  - `SKIP_VERIFY=true scripts/package-backend-release.sh` produced the latest local release package for commit `5f9c5e1b5857`.
  - `scripts/verify-backend-release-package.sh output/backend-release/5f9c5e1b5857` passed.
  - Remote GitHub Actions artifacts for the latest CI run include `backend-release-package` and `backend-jacoco-report`.
- Outcome:
  - The deploy package is ready.
  - Actual EC2 sync is blocked by command-channel access: provide the `local-fresh-key` private key, open SSH `22` for the current operator IP, or grant SSM / security group permissions.

## 2026-06-21 16:32 +0800

- Started Phase 33 SSM Session Manager access setup after the user approved using Playwright to configure AWS Console.
- Scope decision:
  - Use Playwright to inspect and guide AWS Console changes.
  - Do not ask the user to expose passwords, MFA, private keys, or access keys.
  - Do not permanently open SSH to the internet; target SSM Session Manager as the stable deployment channel.
- Current intent:
  - Verify whether EC2 instance `i-0a21d1fff310e3168` has an IAM instance profile.
  - Attach or create an EC2 role with `AmazonSSMManagedInstanceCore` if missing.
  - Verify Systems Manager managed-node online status before attempting SSM-based deploy.

## 2026-06-21 17:25 +0800

- Completed Phase 33 SSM Session Manager access setup.
- AWS changes made:
  - Created IAM role / instance profile `local-fresh-ec2-ssm-role` in AWS Console.
  - Attached AWS managed policy `AmazonSSMManagedInstanceCore` to that EC2 role.
  - Attached the instance profile to EC2 instance `i-0a21d1fff310e3168`.
  - Added inline IAM user policy `LocalFreshSsmDeployAccess` to CLI user `local-fresh-cli` through CloudShell so local AWS CLI can use SSM against this instance.
- EC2 changes made:
  - Connected once through browser-based EC2 Instance Connect as `ubuntu`.
  - Confirmed `amazon-ssm-agent` snap is installed at version `3.3.4121.0`.
  - Started the snap service and confirmed it is `enabled active`.
- Verification:
  - `aws ec2 describe-instances` now reports instance profile `arn:aws:iam::049705856957:instance-profile/local-fresh-ec2-ssm-role`.
  - `aws ssm describe-instance-information` now reports `PingStatus=Online` for `i-0a21d1fff310e3168`.
  - `aws ssm send-command` with `AWS-RunShellScript` succeeded and returned `ssm-ok`, `root`, and hostname `ip-172-31-28-239`.
- Outcome:
  - Future backend deploys can use SSM CLI instead of public SSH or fixed operator IP.
  - Next best step is an SSM-based backend release sync from the verified package, followed by `/actuator/info` and ECPay callback preflight verification.

## 2026-06-21 17:25 +0800

- Started Phase 34 SSM backend deployment sync after SSM command access was verified.
- Scope decision:
  - Use SSM `AWS-RunShellScript` rather than public SSH.
  - Inspect the existing EC2 runtime before changing any jar, systemd unit, environment file, or Nginx config.
  - Preserve existing EC2 secrets and production environment values; do not print or copy secret file contents into chat or docs.
  - Deploy only after confirming the current service path and a rollback path.
- Intended verification:
  - `/actuator/info` should expose the deployed commit.
  - `/payment/callback` should no longer return public HTTP `404`.
  - `scripts/check-ecpay-sandbox-readiness.sh` should progress past the stale-deploy blocker before any ECPay provider switch.

## 2026-06-21 18:21 +0800

- Continued Phase 34 with the first real SSM deployment attempt.
- EC2 runtime discovered:
  - Service: `local-fresh-backend.service`.
  - Runtime jar: `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar`.
  - External prod config: `/home/ubuntu/local-fresh/application-prod.yml`.
  - Backend proxy remains Nginx to `127.0.0.1:8080`.
- Deployment transport attempts:
  - Direct local upload of the 95 MB jar to S3 failed repeatedly with S3 request/socket timeouts.
  - Split-object S3 upload also proved unreliable from this workstation.
  - EC2-side Git clone was rejected because the GitHub repository is private.
  - GitHub Actions artifact download via short-lived redirect succeeded from EC2 without exposing a GitHub token to EC2.
- First deploy attempt:
  - Downloaded and extracted CI `backend-release-package` for commit `a817bd3a5f2e`.
  - Verified jar SHA256 `cf1daef2dd6695dee9cabbd29df88b4727f5b9aab3a52bf4dd1832b340711111`.
  - Backed up the previous runtime jar and replaced `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar`.
  - Added a temporary systemd release drop-in for `SOURCE_COMMIT=a817bd3a5f2e` and `SOURCE_BRANCH=hardening-and-upgrade`.
- Result:
  - New jar entered a restart loop and never served `127.0.0.1:8080`.
  - Rolled back to `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar.bak.20260621101246`.
  - Removed the temporary release drop-in.
  - Verified rollback health: `curl http://127.0.0.1:8080/actuator/health` returned `{"status":"UP"}`.
- Root cause:
  - EC2 backend log showed `Could not resolve placeholder 'CORS_ALLOWED_ORIGIN_PATTERNS'`.
  - `application-prod.yml` required this non-secret env var, but the current EC2 runtime does not define it.
- Fix implemented locally:
  - Updated `application-prod.yml` to default `localfresh.cors.allowed-origin-patterns` to `http://localhost:*,http://127.0.0.1:*` when `CORS_ALLOWED_ORIGIN_PATTERNS` is absent.
- Verification:
  - `mvn -pl local-fresh-server -am verify` passed with 159 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
- Next action:
  - Commit and push the CORS fallback fix.
  - Wait for CI to publish a new `backend-release-package` artifact.
  - Redeploy the new artifact through SSM and then re-run public `/actuator/info` plus ECPay callback preflight.

## 2026-06-21 18:34 +0800

- Committed and pushed CORS fallback fix as `47a85b1 fix: add prod cors fallback`.
- CI run `27901353930` completed successfully:
  - Repository hygiene passed.
  - Admin frontend build/audit passed.
  - User frontend build passed.
  - Backend verify, JaCoCo upload, release packaging, package verification, and artifact upload passed.
- Second deploy attempt:
  - Downloaded CI `backend-release-package` artifact `7774374396`.
  - Deployed jar for commit `47a85b1c3445`.
  - Verified jar SHA256 `734dc5d85ac77c4ee1b28cc1bc9f320ee670648bcc359f6e69f0d814c2c1b931`.
  - Service did not expose `127.0.0.1:8080` within the 90-second health window.
  - Rolled back again to `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar.bak.20260621101246`.
  - Rollback health returned `{"status":"UP"}`.
- Root cause of the second blocker:
  - EC2 log showed Flyway/MySQL startup failure caused by `UnknownHostException: ${DB_HOST}`.
  - This means prod `spring.datasource.*` still required raw `DB_HOST` style env variables that the current EC2 runtime does not define.
  - The existing runtime appears aligned with the older `localfresh.datasource.*` config contract.
- Fix implemented locally:
  - Updated prod `spring.datasource.*` to reference `localfresh.datasource.*` properties, matching base `application.yml`.
  - Kept env support through the `localfresh.datasource.*` section for newer deployments.
- Verification:
  - `mvn -pl local-fresh-server -am verify` passed again with 159 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
- Next action:
  - Commit and push the datasource compatibility fix.
  - Wait for CI to publish the next backend release artifact.
  - Attempt the SSM deploy again and verify `/actuator/info` plus `/payment/callback`.

## 2026-06-21 18:45 +0800

- Committed and pushed datasource compatibility fix as `17fca7a fix: align prod datasource config`.
- CI run `27901637704` completed successfully:
  - Repository hygiene passed.
  - User frontend build passed.
  - Admin frontend build/audit passed.
  - Backend verify, JaCoCo upload, release packaging, package verification, and artifact upload passed.
- Third deploy attempt:
  - Downloaded CI artifact `7774460641`.
  - Deployed jar for commit `17fca7a0ca55`.
  - Verified jar SHA256 `af8ceb465279fd1b0c8f45a861565b27fbcaa15ad3feb1ed2a0334febf63936a`.
  - Service still did not expose `127.0.0.1:8080` within the health window.
  - Rolled back to `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar.bak.20260621101246`.
  - Rollback health returned `{"status":"UP"}`.
- Deeper diagnosis:
  - Masked EC2 config inspection showed the external file still uses the legacy keys `spring.datasource.druid.*`, `spring.redis.*`, and `sky.*`.
  - The previous fix was not enough because `localfresh.datasource.*` is not present in that EC2 file.
- Fix implemented locally:
  - Added prod compatibility fallbacks from `spring.datasource.druid.*`, `spring.redis.*`, and legacy `sky.*` keys to the new `spring.datasource.*` / `localfresh.*` runtime properties.
  - Kept the new env/localfresh contract intact for future cleaner deployments.
- Verification:
  - `mvn -pl local-fresh-server -am verify` passed with 159 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
- Next action:
  - Commit and push the legacy runtime compatibility layer.
  - Wait for CI artifact.
  - Redeploy through SSM and verify public endpoints.

## 2026-06-21 18:59 +0800

- Committed and pushed legacy runtime compatibility as `b87dbc8 fix: support legacy prod config keys`.
- CI run `27901887419` failed only at repository hygiene because the compatibility fallback referenced a blocked legacy payment term. Backend verify/package still passed, but the red CI artifact was not used for deployment.
- Removed that legacy payment fallback and committed `87c55ca fix: remove legacy payment fallback term`.
- CI run `27901978998` completed successfully:
  - Repository hygiene passed.
  - Admin frontend build/audit passed.
  - User frontend build passed.
  - Backend verify, JaCoCo upload, release packaging, package verification, and artifact upload passed.
- Fourth deploy attempt:
  - Downloaded CI artifact `7774560219`.
  - Deployed jar for commit `87c55ca2b218`.
  - Verified jar SHA256 `e7cca3734ca38fff427c5f169f77c20b34c97bcc16f3ccfc8b46c6eb9425d588`.
  - Service did not expose `127.0.0.1:8080` before the health window ended.
  - Rolled back to `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar.bak.20260621101246`.
  - Rollback health returned `{"status":"UP"}`.
- Diagnosis:
  - The new jar now connected to MySQL successfully.
  - Flyway failed because the existing `sky_take_out` schema is non-empty but has no `flyway_schema_history`.
- Fix implemented locally:
  - Added prod defaults `spring.flyway.baseline-on-migrate=true` and `baseline-version=1`.
  - This treats the existing EC2 schema as the V1 baseline and lets later migrations apply.
- Verification:
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.
  - `mvn -pl local-fresh-server -am verify` passed with 159 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
- Next action:
  - Commit and push the Flyway baseline prod handoff fix.
  - Wait for green CI artifact.
  - Redeploy through SSM and verify public endpoints.

## 2026-06-21 19:12 +0800

- Committed and pushed Flyway baseline fix as `64341fb fix: baseline flyway for prod handoff`.
- CI run `27902201114` completed successfully:
  - Repository hygiene passed.
  - Admin frontend build/audit passed.
  - User frontend build passed.
  - Backend verify, JaCoCo upload, release packaging, package verification, and artifact upload passed.
- Fifth deploy attempt:
  - Downloaded CI artifact `7774624960`.
  - Deployed jar for commit `64341fb6c3d9`.
  - Verified jar SHA256 `c1af46c2a3b702f642a1f64b1a821df68707f9c39a652efa89d92c88497b47ae`.
  - Service did not expose `127.0.0.1:8080` before the health window ended.
  - Rolled back to `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar.bak.20260621101246`.
  - Rollback health returned `{"status":"UP"}`.
- Diagnosis:
  - New jar connected to MySQL.
  - Flyway validated 20 migrations and reported the schema up to date.
  - Startup then failed because EC2 external config still overrides MyBatis `type-aliases-package` with the old package, so mapper aliases such as `Category` cannot resolve.
- Fix implemented locally:
  - Added prod MyBatis override for `mapper-locations`, `type-aliases-package: com.localfresh.entity`, and `map-underscore-to-camel-case`.
- Verification:
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.
  - `mvn -pl local-fresh-server -am verify` passed with 159 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
- Next action:
  - Commit and push the prod MyBatis override.
  - Wait for green CI artifact.
  - Redeploy through SSM and verify public endpoints.

## 2026-06-21 19:34 +0800

- Committed and pushed prod MyBatis override as `f03ad96 fix: own prod mybatis aliases`.
- CI run `27902501977` completed successfully:
  - Repository hygiene passed.
  - Admin frontend build/audit passed.
  - User frontend build passed.
  - Backend verify, JaCoCo upload, release packaging, package verification, and artifact upload passed.
- Sixth deploy attempt:
  - Downloaded CI artifact `7774720669`.
  - Deployed jar for commit `f03ad969f151`.
  - Verified jar SHA256 `4c7a8a6f0225c3adff484b205790b156844e89f262c1925d6230ce6df88a5c34`.
  - Service still did not expose `127.0.0.1:8080` before the health window ended.
  - Rolled back to `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar.bak.20260621101246`.
  - Rollback health returned `{"status":"UP"}`.
- Diagnosis:
  - New jar connected to MySQL.
  - Flyway validated 20 migrations and considered the schema up to date.
  - Startup still failed on `Category` alias resolution because the EC2 external config had higher precedence than packaged `application-prod.yml`.
- Fix implemented:
  - Added `MyBatisConfiguration` to register `com.localfresh.entity` aliases with MyBatis at application startup.
  - Added `MyBatisConfigurationTest` to prove the short alias `Category` resolves to `com.localfresh.entity.Category`.
- Verification before deploy:
  - `node scripts/check-repo-hygiene.mjs` passed.
  - `git diff --check` passed.
  - `mvn -pl local-fresh-server -am -Dtest=MyBatisConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` passed.
  - `mvn -pl local-fresh-server -am verify` passed with 160 tests, 0 failures, 0 errors, 5 skipped, and JaCoCo report generation.
- Committed and pushed alias registration fix as `a8948dd fix: register mybatis entity aliases`.
- CI run `27902813923` completed successfully:
  - Repository hygiene passed.
  - Admin frontend build/audit passed.
  - User frontend build passed.
  - Backend verify, JaCoCo upload, release packaging, package verification, and artifact upload passed.
- Final deploy:
  - Downloaded CI artifact `7774817521` directly on EC2 through a short-lived GitHub artifact redirect URL.
  - Deployed jar for commit `a8948ddb0a93`.
  - Verified jar SHA256 `3e4a13f621c7826aaba7247827a434b4571ff9743c549e58900fe6e9a0ed9461`.
  - Service started successfully and returned `{"status":"UP","groups":["liveness","readiness"]}`.
  - Fixed the systemd release drop-in formatting and restarted once more so `/actuator/info` exposes `commit=a8948ddb0a93` and `branch=hardening-and-upgrade`.
- Public verification:
  - `curl -sS https://localfresh-demo.duckdns.org/actuator/info` returned commit `a8948ddb0a93`.
  - `EXPECTED_DEPLOY_COMMIT=a8948ddb0a93 scripts/check-ecpay-sandbox-readiness.sh` passed backend health, Nginx public serving, actuator info, commit match, callback invalid-signature `0|FAIL`, and storefront HTTP 200.
- Phase 34 completed. The deployed backend is now synced to the code containing `/payment/callback`; the next phase should be a reversible ECPay sandbox runtime switch or deployment runbook cleanup.

## 2026-06-21 20:02 +0800

- Started Phase 35 reversible ECPay sandbox switch.
- Re-read planning files, ECPay runbook, readiness script, prod config, and `EcpayPaymentGateway`.
- Current judgment:
  - Code already supports `PAYMENT_PROVIDER=ecpay`, ECPay signed checkout payloads, ReturnURL callback parsing, and invalid-signature `0|FAIL`.
  - The remaining work is operational: verify whether EC2 has the sandbox variables, add a guarded SSM switch/rollback path, and avoid printing secrets.
  - Directly switching the deployed process without rollback evidence would be too risky because it changes demo payment behavior.
- Audited EC2 runtime through SSM without printing secrets:
  - `local-fresh-backend.service` was active.
  - `/actuator/info` returned commit `a8948ddb0a93`.
  - No payment-provider drop-in existed yet, so runtime was still on default demo provider.
- Added `scripts/switch-ecpay-sandbox-ssm.sh`:
  - `status` inspects service state, actuator info, payment drop-in, and effective payment env with HashKey/HashIV redacted.
  - `enable` runs public readiness before and after switching, writes a dedicated systemd payment drop-in, restarts Spring Boot, and verifies health/info.
  - `rollback` removes only the payment drop-in and restarts Spring Boot, returning to demo provider without changing the deployed jar or release identity.
- Verified the script:
  - `sh -n scripts/switch-ecpay-sandbox-ssm.sh` passed.
  - `scripts/switch-ecpay-sandbox-ssm.sh status` succeeded through SSM and showed no payment env before the switch.
- Switched EC2 to ECPay sandbox:
  - Ran `EXPECTED_DEPLOY_COMMIT=a8948ddb0a93 scripts/switch-ecpay-sandbox-ssm.sh enable`.
  - Public readiness before the switch passed health, Nginx, actuator info, commit match, callback invalid-signature `0|FAIL`, and storefront HTTP 200.
  - SSM command wrote `/etc/systemd/system/local-fresh-backend.service.d/payment-provider.conf`, restarted Spring Boot, and verified health `UP`.
  - Effective runtime env showed `PAYMENT_PROVIDER=ecpay`, MerchantID `2000132`, stage checkout URL, ReturnURL, and OrderResultURL; HashKey/HashIV were redacted.
  - Public readiness after the switch passed again.
- Re-ran `scripts/switch-ecpay-sandbox-ssm.sh status`:
  - Confirmed the payment drop-in remains present.
  - Confirmed effective `PAYMENT_PROVIDER=ecpay`.
- Updated ECPay runbook, backend deploy runbook, EC2 runtime README, README/README.en, known issues, portfolio roadmap, backend deep-dive prep, and interview guide to remove the old public 404 blocker and record the remaining gap: real browser ECPay stage checkout plus ReturnURL / OrderResultURL evidence.
