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

## Phase 10 Backend Boundary Test Findings

- The backend already has strong order-lifecycle coverage around payment request/callback handling, cancellation/refund metadata, allowed state transitions, and audit-log persistence.
- `GroupBuyExpirationServiceTest` already proves the scheduled expiry path with real Redis/Testcontainers behavior: expired active groups are failed, pending group-buy orders are canceled, refund intent is logged, and a held Redisson lock prevents duplicate processing.
- The smaller gap is explicit fulfillment guard coverage for missing orders. Admin confirm/delivery/complete currently share `OrderStatusTransitionPolicy` and return `ORDER_STATUS_ERROR` when the order is missing; member reminder returns `ORDER_NOT_FOUND` for missing or non-owned orders.
- This is a good interview-readiness slice because it documents behavior at service boundaries without changing the production flow or adding architecture.

## Phase 11 Product Admin Boundary Findings

- `ProductServiceImplTest` previously covered manual inventory adjustment audit logging, but did not pin down product deletion guard behavior or product-disable side effects.
- Product deletion is an important admin boundary because active products must not be deleted and products referenced by gift boxes must stay protected.
- Product disable has a business side effect: related gift boxes are disabled together, preventing admins from selling a box that contains an unavailable product.
- These tests strengthen the inventory/catalog story without changing runtime behavior or introducing new abstractions.

## Phase 12 Frontend First-Impression Findings

- External reference direction:
  - Mature grocery commerce patterns emphasize search, categories, fast replenishment shelves, and making the next cart action obvious.
  - Mature admin tools emphasize actionable metrics, status queues, filters, and dense but calm operational surfaces rather than decorative hero content.
  - Dashboard design research supports showing metrics in a way that leads to the next operational action, not just displaying numbers.
- Current UI is not weak for a Java backend portfolio, but a few first-impression issues remain:
  - The member home page is structured well, but the first product grid can surface a scattered mix such as seafood and condiments before everyday meal-building items.
  - The group-buy detail page reads more like a data detail page than a consumer decision page.
  - The admin dashboard visual system is acceptable, but seeded dashboard activity can show all-zero business metrics, making the demo look empty.
- The right scope is a targeted polish pass: recommendation ordering and quick shelves on the member home page, stronger progress/CTA hierarchy on group-buy detail, and a Flyway refresh for today dashboard activity.

## Phase 13 Payment Lifecycle Findings

- The project already has more payment readiness than the next-step discussion assumed:
  - `PaymentGateway` abstracts provider behavior.
  - `DemoPaymentGateway` supports signed local callbacks and immediate demo success.
  - `EcpayPaymentGateway` builds signed ECPay stage checkout payloads and verifies callback CheckMacValue.
  - `/payment/callback` accepts provider form callbacks and records payment events through `OrderPaymentServiceImpl`.
  - The member frontend can convert an ECPay response into a hidden POST form redirect.
- Because this is already in place, the highest-value local slice is not another payment abstraction. It is lifecycle hardening: prove cancelled and pending group-buy orders cannot create a provider payment request, remove stale mapper methods that bypass the guarded update path, and align docs with the real model.
- The architecture doc had a stale `退款(7)` order-status node. Runtime code does not define status 7; refunds are represented as `pay_status = REFUND` on a cancelled order plus payment/refund event evidence.

## Phase 14 Roadmap Findings

- The user explicitly wants every future round to include a follow-up planning assessment.
- README already had a Roadmap section, but it mixed completed milestones, active ideas, and cloud/CD topics without a clear priority model.
- A durable roadmap document is useful because it separates interview-readiness from true production completeness. This prevents the project from chasing impossible "100%" scope while still showing mature judgment.
- Current best next implementation slice after this roadmap is inventory idempotency design/testing, not cloud deployment. ECPay sandbox and CD remain valuable but should come after core local flow and evidence stay stable.

## Phase 15 Inventory Idempotency Findings

- `InventoryServiceImpl.restoreProduct` increases stock every time it is called and writes a positive `product_inventory_log` row.
- Normal controller/service flows usually prevent a second cancellation through order status policy, but a stale service call or retry can still call `OrderCancellationServiceImpl.cancelOrder` with an old paid order object.
- `product_inventory_log` already records `reference_type`, `reference_id`, and `reason`, which is enough for a lightweight service-level duplicate-cancellation guard without adding a new schema concept.
- The pragmatic slice is to make order cancellation transactional and skip the cancellation workflow if the order is already cancelled or if an `ORDER_CANCEL_RESTORE` log already exists for that order.

## Phase 16 Minimal Business Observability Findings

- `local-fresh-server` already depends on Spring Boot Actuator, so the right portfolio-sized observability slice is to add business counters rather than a new monitoring stack.
- `/actuator/**` is outside the existing `/admin/**` and `/user/**` JWT interceptor scopes. Exposing `metrics` is useful for local review, but production exposure should remain configurable through `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`.
- The highest-signal counters for the current domain are payment callback results, duplicate/applied order cancellations, and group-buy transitions. These connect directly to the most interviewable backend risks: payment correctness, idempotency, and group-buy lifecycle health.
- Prometheus, Grafana, alerting, and tracing are valid later work, but adding them now would be more infrastructure than the project needs for this phase.

## Phase 17 ECPay Sandbox Readiness Findings

- Public preflight on 2026-06-20 reached `https://localfresh-demo.duckdns.org/actuator/health` and confirmed Nginx, but `/payment/callback` returned HTTP 404. The deployed EC2 backend is not yet on the version containing the callback endpoint.
- Because the public callback endpoint is missing, `PAYMENT_PROVIDER=ecpay` must not be enabled on the deployed demo yet; ECPay ReturnURL would fail before Spring Boot can verify CheckMacValue or write payment events.
- The remaining local readiness gap is provider-selection proof: when `localfresh.payment.provider=ecpay` is set, Spring should select `EcpayPaymentGateway` instead of the demo gateway and generate a signed checkout payload with the deployed ReturnURL and OrderResultURL.

## Phase 18 Local UI Smoke Precheck Findings

- The two frontends do not currently have Playwright or a shared E2E test framework. Adding one only for a small smoke check would increase tool churn.
- `npx --yes playwright --version` failed under the current sandbox due DNS/network access to `registry.npmjs.org`, so a Playwright-based smoke runner is not a reliable local gate in this environment.
- The most useful low-cost smoke gate is dependency-free Node: verify local services, auth, and core data APIs before manual browser acceptance and screenshot refresh.
- This smoke precheck is deliberately not a visual-regression replacement. It catches service/proxy/auth/demo-data failures quickly, while human Playwright/browser inspection remains the visual quality gate.

## Phase 19 Browser UI Smoke Findings

- After explicit approval to use Playwright, the right scope is a small root-level browser smoke instead of separate frontend E2E frameworks.
- The browser smoke should stay focused on demo-critical routes: member mock login, storefront home, member orders, admin login, dashboard, orders, and products.
- This complements `scripts/check-ui-smoke-local.mjs`: the Node script proves services/auth/data APIs are alive, while Playwright proves the real browser can render and navigate the critical UI surfaces.
- The first Playwright run found selector ambiguity, not product breakage: repeated labels such as "全部商品" and "待確認" need role- or first-match selectors in smoke tests.

## Phase 20 Fullstack Product-Grade UX Findings

- Raising the target to fullstack roles changes the scoring standard: backend evidence is no longer enough; member commerce flow, admin operations ergonomics, responsive behavior, and repeatable browser proof need to feel deliberate.
- Initial browser inspection of the member login page shows it is visually acceptable, but still spends the first viewport on brand/login rather than communicating the product's commerce loop. This is acceptable for now because the higher-impact fullstack signal is the shopping and admin workflow after login.
- Member home audit found the most important commerce gap: product cards looked polished but still behaved like a gallery. They had no direct add-to-cart action and weak decision cues beyond category and price.
- Mobile home audit showed the first purchasable product was pushed far below the first viewport by hero, recommendation, and fulfillment panels. The fix is to make the first viewport carry a clear "start shopping" action and compress mobile recommendation density without hiding product evidence.
- Product detail audit showed the purchase actions were present, but the page did not answer enough buyer trust questions. Adding delivery, storage, and group-buy assurance cards makes the page feel closer to a real grocery product page.

## Phase 21 Admin Operations Polish Findings

- Admin dashboard already had useful operations data: pending orders, low-stock products, and offline products. The higher-value gap was not another dashboard widget; it was making the core order/product list pages behave like work queues instead of plain CRUD tables.
- Orders page needed stronger fulfillment hierarchy: pending confirmation, confirmed orders, and in-delivery orders should be clickable operational queues, and each row should expose the next fulfillment judgment before the operator opens details.
- Products page needed a similar operational model: stock health, content completeness, sale status, and replenishment actions should be visible on the list page so admins can spot problems without opening every product.
- Browser inspection caught a concrete layout issue after the first order-page pass: the table still overflowed the desktop content width at 1580px because the combined column widths reached 1400px inside a 1235px content area. Column widths were reduced to keep the main table inside the card.
- Mobile admin remains table-heavy by nature, but the new summary cards were tightened so the page keeps useful queue controls above the fold instead of pushing the table too far down.

## Phase 22 Backend Deep-Dive Prep Findings

- The repository already had strong backend evidence, but it was spread across README, architecture, testing, observability, perf docs, and implementation tests. Kevin needs a self-facing prep note to rehearse that story, not a public document that looks written for interviewers.
- The best next local slice is candidate prep material, not another feature. It should explicitly connect core risks to implementation and tests: order lifecycle, payment callback idempotency, group-buy concurrency, inventory consistency, observability, security boundaries, and capacity limits.
- The prep note should be honest about residual risks. For Shopee / Binance / Google-style interviews, overclaiming production completeness is weaker than showing precise tradeoffs and a credible next-step plan.
- Current strongest evidence anchors:
  - `OrderStatusTransitionPolicy` plus order/payment/cancellation/fulfillment service tests.
  - `OrderPaymentServiceImpl` guarded update plus payment-event and callback tests.
  - `GroupBuyServiceImpl` Redisson lock, DB unique participant constraint, Redis/Testcontainers tests, and JMeter evidence.
  - `OrderCancellationServiceImpl` duplicate guard and inventory logs.
  - Actuator business counters and admin operation/payment event views.

## Phase 23 Backend Design Diagram Findings

- Adding ER diagrams is useful, but the public README should not become an interviewer-facing design packet. The right place for deeper diagrams is `docs/architecture.md`, with `docs/backend-deep-dive-prep.md` explaining how Kevin should use them in conversation.
- The existing architecture document already had a core ER and state machine, but it underrepresented the backend evidence tables added later: `payment_event`, `product_inventory_log`, and `admin_operation_log`.
- The most interviewable diagram sequence is:
  - group-buy/core ER to explain data ownership.
  - order state machine to explain legal status transitions.
  - payment callback sequence to explain provider callback idempotency.
  - payment/inventory/admin evidence ER to explain auditability and troubleshooting.
- These diagrams should be presented as implementation evidence and tradeoff discussion, not as a claim that the project is production-complete.

## Phase 24 Inventory Restore DB Idempotency Findings

- A direct unique key on `(reference_type, reference_id, reason, product_id)` would be too blunt without changing restore behavior first: a single order can include the same underlying product through a direct item and a gift box component.
- The safer boundary is a nullable `product_inventory_log.idempotency_key`, generated only for `*_CANCEL_RESTORE` events. Normal reserve logs and manual adjustments keep `NULL`, so repeated legitimate stock movements are not blocked.
- Order cancellation now aggregates restored quantities by product before calling `InventoryService.restoreProduct`, so one cancelled order writes at most one restore log per product and can safely use a per-order/per-product idempotency key.
- This is still a single-service consistency model, but it is stronger than service precheck alone because duplicate restore keys are now rejected by the database.

## Phase 25 CI Quality Gate Findings

- The repo already had GitHub Actions, but backend CI only ran `mvn test`. That missed the JaCoCo report generation path that the README and interview docs now cite as backend evidence.
- A portfolio-quality gate should stay practical: backend `verify`, admin build/audit, user build, and lightweight repository hygiene. It should not try to boot the whole fullstack demo or run browser smoke without explicit service orchestration.
- Repository hygiene is useful because this project has curated README screenshots and a history of course-template terminology cleanup. A small script can cheaply assert screenshot references and guard against old platform terms returning.
- Existing repo files had some trailing whitespace in older docs/admin files, so adding a global whitespace gate required a small mechanical cleanup first.
- Re-running the full local gate exposed a real local flake: Mockito inline mock maker could not self-attach under the Homebrew JDK after repeated full test runs. Switching test resources to `mock-maker-subclass` is a better fit because the current tests do not use final/static mocking and CI should not depend on JVM attach behavior.
- Admin `npm audit --omit=dev` was already part of CI and failed on `form-data@4.0.5` through `axios`. Updating the admin lockfile to `form-data@4.0.6` keeps the audit gate meaningful instead of leaving CI red.
- The first remote CI run after hardening exposed a CI-only ordering bug: `actions/setup-node` cannot use pnpm cache before pnpm is available through Corepack. The correct fix is to remove that cache shortcut for the user frontend job and let Corepack provide the pinned pnpm version used by the actual install/build steps.

## Phase 26 ECPay Callback Contract Findings

- The 2026-06-21 public ECPay preflight still reaches `https://localfresh-demo.duckdns.org/actuator/health` through Nginx, but `POST /payment/callback` returns HTTP `404`. The deployed backend is still not synced to the version containing the callback endpoint.
- Because ReturnURL would currently miss Spring Boot entirely, it is still incorrect to switch the deployed process to `PAYMENT_PROVIDER=ecpay`.
- The local application contract was weaker than the unit-test inventory suggested: existing tests covered the controller with a mocked gateway and the ECPay gateway parser independently, but not the real ECPay gateway wired through the callback controller.
- `PaymentCallbackControllerEcpayContractTest` closes that gap without requiring a database or real sandbox credentials: a signed ECPay form payload returns `1|OK` and reaches `OrderPaymentService`, while an invalid `CheckMacValue` returns `0|FAIL` and does not call payment handling.
- The remaining blocker is operational deployment sync, not backend parsing logic. After EC2 is redeployed, the same public preflight should change from HTTP `404` to HTTP `200` with body `0|FAIL`.

## Phase 27 Deployment Version Observability Findings

- The repository does not currently include an EC2 deployment script or checked-in SSH/systemd workflow, so the safe local slice is to make deployment sync verifiable rather than attempting an opaque cloud change.
- The updated public preflight shows `/actuator/info` also returns HTTP `404` on the deployed API. This means the public environment cannot yet prove which backend commit is running.
- A lightweight Actuator `InfoContributor` is enough for this portfolio stage: it exposes only non-sensitive deployment identity such as application name, active profiles, commit, branch, and the payment callback path.
- `SOURCE_COMMIT` and `SOURCE_BRANCH` give the EC2 process a simple deployment identity without adding image build automation, ECR, or a new deployment platform.
- The ECPay readiness script should support `EXPECTED_DEPLOY_COMMIT` so a future deploy cannot accidentally pass health checks while running an older backend.

## Phase 28 Backend Release Packaging Findings

- Because there is still no checked-in EC2 credential or systemd deployment contract, the right next slice is a release package handoff rather than an opaque "deploy" command.
- A backend release should be built from a clean worktree, run backend `verify` by default, copy the repackaged Spring Boot jar into an ignored output directory, and record the commit/branch metadata that `/actuator/info` expects.
- The release package should produce editable EC2 commands instead of executing SSH automatically. This avoids hiding hostnames, service names, or secrets in the repo while still making the manual deployment repeatable.
- `scripts/package-backend-release.sh` dry run confirmed it can reuse the existing jar and write `release.env` plus `ec2-deploy-commands.txt` under `output/backend-release/<commit>/`, which is ignored by git.

## Phase 29 EC2 Runtime Template Findings

- The repository had an EC2 deploy runbook, but no checked-in runtime contract for systemd, Nginx, or the production environment variable surface.
- The safe portfolio-sized improvement is to version non-secret templates under `deploy/ec2/`, not real EC2 hostnames, SSH paths, DB passwords, JWT secrets, AWS keys, or ECPay credentials.
- The runtime contract should match the code paths already implemented: Spring Boot runs with the `prod` profile, Nginx proxies HTTPS traffic to `127.0.0.1:8080`, and `SOURCE_COMMIT` / `SOURCE_BRANCH` are set so `/actuator/info` can prove the deployed version.
- The release package should include these templates alongside the jar and deploy commands so a future EC2 sync has one reviewable artifact instead of scattered notes.
- Repository hygiene should scan `.service`, `.conf`, and `.example` files so deployment templates cannot silently reintroduce legacy terminology or trailing whitespace.

## Phase 30 CI Backend Release Artifact Findings

- The backend CI job already runs `mvn -pl local-fresh-server -am verify`, so it creates the repackaged Spring Boot jar needed by the release packaging script.
- Running `scripts/package-backend-release.sh` again with full verification inside the same CI job would duplicate the Maven gate. The better CI path is `SKIP_VERIFY=true` after the verified jar exists.
- The release artifact should be uploaded from `output/backend-release`, not from `target/`, because it carries deploy metadata, EC2 deploy commands, and `deploy-templates/` in addition to the jar.
- This is still not full CD. It is a safer intermediate step: every green backend CI run now has a deployable package that can be downloaded and manually synced to EC2.

## Phase 31 Release Package Integrity Findings

- A deployable artifact is more credible if it can prove both identity and file integrity before copying to EC2.
- The package already has `release.env`, but that is not enough to catch a truncated jar, missing runtime template, or artifact extracted from the wrong directory.
- The pragmatic integrity layer is a human-readable `release-manifest.txt`, a portable `SHA256SUMS`, and a small verifier script that checks required files, template presence, commit/jar consistency, callback path metadata, and checksums.
- The verifier should accept both the exact commit package directory and the parent `output/backend-release` directory, because GitHub artifact extraction often creates a parent folder before the commit folder.
- CI should run the verifier before uploading the artifact so `backend-release-package` is not just built, but structurally validated.
