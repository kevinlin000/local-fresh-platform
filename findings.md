# Portfolio Upgrade Findings

## Context Read

- `CONTEXT.md` positions the project as `菜籃日 Cailan Day`, a Java backend portfolio with Vue frontends and AWS deployment evidence.
- ADR 0001 explicitly prioritizes practical backend fundamentals, complete flow, transaction correctness, Redis/Redisson usage, deployment evidence, and interview explainability.

## Current Direction

- Highest leverage next step is realistic demo data: a polished UI still looks weak if every page is empty.
- Avoid broad architecture refactors unless they reduce real friction or strengthen the portfolio narrative.

## Phase 1 Schema/Seed Findings

- Backend uses Flyway migrations under `local-fresh-server/src/main/resources/db/migration`.
- `application.yml` enables Flyway from `classpath:db/migration`.
- Docker Compose previously mounted the migration directory into MySQL init scripts as well. That becomes unsafe once Flyway reaches `V10`, because MySQL container init runs filenames lexicographically rather than by Flyway version ordering.
- Existing migrations include `V5__taiwan_localization.sql` and `V6__add_product_images.sql`, so the project already has a Taiwan-localized product/catalog seed base. The next gap is likely demo journey data: members, addresses, carts, orders, group buys, and a clear reset/load story.
- `V5__taiwan_localization.sql` deletes cart, order, order detail, group-buy participant, group-buy, product, gift box, and category demo data, then inserts eight categories, about thirty products, and six gift boxes.
- Because V5 resets order/cart/group-buy tables, a later migration is the right place for journey seed data.
- Fixed direction: MySQL container should only provide the database service; Spring Boot/Flyway should own schema and seed ordering.
- Order status values are `1=待付款`, `2=待確認`, `3=已確認`, `4=配送中`, `5=已完成`, `6=已取消`, `8=揪團中`.
- Member order UI displays `orders` rows plus `order_detail` rows, and group-buy UI displays `group_buy`, `group_buy_participant`, member names, and product/order-derived fields.
- `V6__add_product_images.sql` still referenced a legacy course-era S3 bucket. This was replaced because migrations are part of the portfolio surface.
- Stable image approach: use `/demo-assets/...` static frontend assets in both user and admin builds. This removes external bucket coupling and keeps local/prod demos deterministic.
- `application.yml` still had legacy package logging keys; those were namespace residue and should point at the current `com.localfresh.*` packages.
- Fresh Docker verification caught a real demo issue: the pending-payment seeded order was older than the scheduled timeout threshold and was auto-canceled on startup. The seed now keeps that order recent.
- `.env.example` had an outdated database name that did not match `application-dev.yml`; this would break fresh local setup unless corrected.

## Phase 2-4 Findings

- Full `app.use(ElementPlus)` made the frontends look small-project-like in build output: user/admin Element Plus JS chunks were about 780-966 kB before optimization.
- Admin can use `unplugin-vue-components` with `ElementPlusResolver`; user could not safely install those plugins because npm 11 repeatedly failed on the existing user dependency tree, so user uses manual component registration instead.
- After optimization, the largest Element Plus-related JS chunks are about 266 kB in admin and 202 kB in user.
- Removing the legacy datasource pool was safe because the app only used standard datasource URL/username/password settings. Spring Boot now starts with HikariCP.
- Fastjson was used only for simple JSON serialization/parsing and test response parsing. Replacing it with Jackson removes an avoidable legacy dependency without changing behavior.
- Flyway migration files are part of the portfolio surface, but existing migration checksums still matter. V5 comment-only wording changes were reverted to preserve checksum stability; V10 is a new demo seed migration and was re-applied locally after its final seed timing was adjusted.

## Phase 6 Browser Acceptance Findings

- User login page loads and mock login works for `試用會員 A`.
- First blocker: after login, the user home page shows `0 / 0` available products even though demo catalog seed data exists. Network history showed category requests succeeding, but the product list was not visibly populated.
- Root cause: `HomeView` treated `activeCategoryId = null` as "nothing to load" during initial mount, even though `null` represents the valid "全部商品" query.
- Fixed and verified in browser: homepage now requests `/api/user/product/list`, renders `29 / 29` available products, and has no console errors.
- Member flow now works end to end in browser: login, product list, product detail, add to cart, checkout, order submit, simulated payment, order history, and admin confirmation.
- Checkout exposed a local-dev blocker: `delivery.range-check-enabled` was effectively enabled in dev, so order submission depended on live Google Maps geocoding and failed with `店家地址解析失敗` in a fresh demo. Dev config now disables range checks by default while production-style config can still enable it explicitly.
- Checkout also exposed an environment/documentation problem: starting only `local-fresh-server` without first installing sibling modules can run against a stale local `local-fresh-common` artifact and fail with `NoClassDefFoundError: com/localfresh/utils/JsonUtil`. README and perf docs now document the required `mvn install -DskipTests` step before running the server module directly.
- User frontend now unwraps backend error payload messages before surfacing Axios failures, so business-rule failures are readable instead of generic HTTP status strings.
- Cart checkout had an Element Plus radio deprecation warning caused by using `label` as value. Switching to `value` removes the warning.
- Admin order confirmation had a real Vue event-handler issue: dismissing or racing the Element Plus confirm dialog could leave an unhandled `cancel` error in console and no API call. The order action flows now treat cancel/close as normal no-ops and catch API errors with a user-visible message.
- Admin order confirmation was verified in browser: `PUT /api/order/confirm` returned 200, the submitted member order changed from `待確認` to `已確認`, statistics updated, and console stayed clean.
- Admin mobile layout was broken by `body { min-width: 1180px; }` plus the fixed sidebar grid: the 390px viewport showed clipped blank cards. A mobile layout rule now turns the sidebar into a top horizontal nav, removes the global min-width, and lets admin content render normally on narrow screens.
- Browser acceptance covered user desktop flow, user mobile home/orders screenshots, admin desktop dashboard/orders/products/reports, and admin mobile dashboard/products screenshots. Console remained clean after the fixes.
- Admin Vite/Rolldown build emitted third-party `INVALID_ANNOTATION` warnings from `@vueuse/core`; these are now filtered narrowly through Rolldown's `onLog` hook only for that dependency/code combination, leaving other warnings visible.
- Final verification for this slice: user build passes, admin build passes cleanly, backend `mvn test` passes with 100 tests, 0 failures, 0 errors, and 5 skipped manual/external tests.
- A hard legacy-term scan found no remaining matches for the old course/project name, old login-provider terms, old map/cloud-provider names, or related romanized variants in tracked source/docs outside build artifacts.

## Phase 7 Frontend Visual Findings

- The member frontend is functionally credible, but the visual language still reads like a generic generated storefront: large marketing hero, card-in-card layout, many pill-like labels, soft cream/green surfaces everywhere, and slogan-heavy copy.
- The highest-impact member-side improvement is to make the first screen shopping-first: delivery context, search, categories, product count, product grid, and group-buy as a supporting commerce module rather than a separate marketing card.
- The admin frontend should feel more like a daily operations console than a branded landing page. The current dashboard uses large decorative cards and warm gradients; a stronger portfolio signal is a quieter surface with dense metrics, clear priority queues, subtle borders, and less ornament.
- Design research used for this pass: Material Design's emphasis on grid, spacing, responsive behavior, and depth; Human Interface Guidelines' emphasis on consistency and learnability; Nielsen-style heuristics for system status, real-world language, and consistency; and the aesthetic-usability effect, which supports treating visual polish as part of perceived usability rather than decoration.
- Implementation constraint: keep the existing Vue/Element Plus stack, avoid adding new UI dependencies, and improve only the visible structure, spacing, color system, and copy needed for a stronger portfolio demo.
- The first visual pass improved the member home page, but browser inspection exposed a misleading `0 款直送箱` header fact because gift boxes are only loaded when the gift-box tab is active. The header now uses a non-numeric "產地直送箱" fact instead of implying loaded data that does not exist yet.
- Admin product management exposed a concrete usability issue at 1280px desktop width: the wide operation column clipped actions off the right edge. Product table columns are now narrower and row actions wrap, so edit, status, inventory adjustment, inventory logs, and delete remain visible.
- Final Phase 7 browser inspection covered member home, product detail, cart, orders, admin dashboard, admin products, admin orders, and mobile member/admin layouts with clean console output.

## Phase 8 Portfolio Evidence Findings

- The project implementation is ahead of the documentation: `README.md` still describes the old hero-led storefront and only includes member-side screenshots, while Phase 7 moved the UI toward a shopping-first storefront and operations-console admin.
- The highest-value next documentation work is not adding more claims; it is replacing stale screenshots, adding admin proof, and giving reviewers a guided demo path.
- The best evidence package for this project is a small set of curated screenshots plus a spoken demo route. Raw feature lists are less persuasive than showing the full loop: storefront browsing, product decision, group-buy progress, cart, orders, admin dashboard, products, and fulfillment.
- Known limitations should stay visible rather than hidden. For this portfolio, payment mock, no visual-regression suite, single-instance AWS deployment, and dev-disabled delivery range checks are acceptable if they are framed as deliberate next steps.
- Interview preparation should separate three stories:
  - Product story: local fresh-produce storefront, cart/order flow, and group-buy free-shipping mechanism.
  - Backend story: Spring Boot modularity, transaction boundaries, Redisson lock, Redis/Testcontainers evidence, Flyway, JWT/ThreadLocal cleanup, OAuth, and deployment readiness.
  - Full-stack story: Vue 3 member/admin surfaces, Vite builds, API proxy integration, responsive polish, and real browser acceptance evidence.

## Phase 9 Real-Image Evidence Findings

- The user's real PNG food images live at `/Users/kevinlintingwei/Desktop/local-fresh-images`.
- Commit `61d7edd` restored these images into both frontends under `public/demo-assets` and added Flyway `V15__use_real_food_images.sql`.
- A complete evidence refresh should verify screenshots now show the PNG food images, not the earlier SVG placeholders.
- Because product and gift-box list APIs are cached through Redis, browser acceptance should account for possible stale `product_*` and `giftbox_*` keys before screenshot capture.
- Browser acceptance found that `GB-DEMO-ACTIVE` had already expired and been marked failed because the original seed used `NOW() + 18 hours` on June 17. The demo journey needs a refresh migration so reviewer-facing screenshots do not decay over time.
- The fix is a follow-up migration instead of editing V10/V15, preserving Flyway checksum safety while restoring the reviewer-facing active group-buy journey.
- The screenshots now use 1280x720 viewports and show the restored PNG product imagery in the storefront/product/cart/group-buy evidence.
