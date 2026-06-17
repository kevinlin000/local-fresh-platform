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
