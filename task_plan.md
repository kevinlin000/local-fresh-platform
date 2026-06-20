# Portfolio Upgrade Plan

## Objective

Raise `local-fresh-platform` to an interview-ready Java backend/fullstack portfolio project while respecting ADR 0001: practical fundamentals over heavyweight architecture.

## Success Criteria

- Demo data makes the user/admin journeys look like a real local fresh-produce platform, not an empty demo.
- User and admin frontends have consistent Taiwan-localized copy and no legacy course/platform terminology.
- Frontend production builds avoid avoidable large-entry warnings through defensible code splitting.
- Backend tests and frontend builds pass after each meaningful slice.
- README/docs remain aligned with implemented behavior and demo evidence.

## Phases

| Phase | Status | Goal |
| --- | --- | --- |
| 1. Demo data and local seed story | completed | Inspect schema/migrations, add realistic seed data, document how to load it safely. |
| 2. Admin surface polish | completed | Make admin order/product dashboard copy and visible UI consistent with the polished member side. |
| 3. Frontend bundle hygiene | completed | Split obvious vendor chunks and verify build warnings are reduced or documented. |
| 4. Evidence pass | completed | Run tests/builds/scans, update README/docs with accurate verification notes. |
| 5. Commit checkpoint | completed | Commit a clean, reviewable slice with summary and residual risks. |
| 6. Browser acceptance pass | completed | Run backend/user/admin locally, inspect real desktop/mobile flows, fix high-impact UI/UX or flow blockers, and record evidence. |
| 7. Frontend product-grade visual pass | completed | Reduce template-like UI, make the member storefront feel like a real fresh-produce shopping flow, and make the admin surface feel like an operational tool. |
| 8. Portfolio evidence and interview package | completed | Refresh screenshots and documentation so the repository presents the latest product-grade UI, backend strengths, and interview demo path clearly. |
| 9. Real-image evidence refresh | completed | Regenerate screenshot evidence and documentation checks after restoring the user's real food PNG assets. |
| 10. Backend boundary test hardening | completed | Add focused service-level guard tests and align testing documentation with existing group-buy expiration coverage. |
| 11. Product admin boundary coverage | completed | Add focused product-management tests for deletion guards, gift-box status cascading, and cache invalidation side effects. |
| 12. Frontend first-impression polish | completed | Reduce AI/demo feel by improving grocery merchandising, group-buy decision hierarchy, and admin dashboard demo activity. |
| 13. Payment lifecycle evidence hardening | completed | Tighten order/payment lifecycle guards, remove stale bypasses, and align docs with the implemented refund model. |
| 14. Portfolio roadmap and next-step assessment | completed | Add a durable completeness matrix and next-priority roadmap so each future slice has an explicit evaluation basis. |
| 15. Inventory idempotency guard | completed | Add a small service-level duplicate-cancellation guard so repeated cancellation cannot refund or restore stock twice. |
| 16. Minimal business observability | completed | Add Actuator-backed business counters for payment callbacks, cancellation idempotency, and group-buy transitions, then document how to inspect them. |
| 17. ECPay sandbox readiness gate | completed | Add a repeatable provider-switch readiness test, run the public preflight, and document the current blocker before any sandbox switch. |
| 18. Local UI smoke precheck | completed | Add a dependency-free local smoke script for storefront/admin dev servers, auth APIs, and core demo data APIs. |
| 19. Browser UI smoke test | completed | Add a Playwright browser smoke that exercises member and admin critical routes against local services. |
| 20. Fullstack product-grade UX pass | completed | Audit the member and admin UI with a higher fullstack hiring bar, then implement a focused polish slice with repeatable browser evidence. |
| 21. Admin operations polish | completed | Make admin orders/products feel like operational work queues with risk signals, filters, and repeatable browser verification. |
| 22. Backend deep-dive prep notes | completed | Consolidate correctness, concurrency, payment, inventory, observability, and residual-risk evidence into Kevin-facing interview prep notes. |
| 23. Backend design diagram evidence | completed | Add ER, state, and payment callback diagrams that support backend interview deep dives without turning the public README into a pitch deck. |
| 24. Inventory restore DB idempotency | completed | Add a nullable inventory-log idempotency key, aggregate same-product restores, and prove duplicate restore keys are rejected by the database. |

## Constraints

- Do not add microservices, CQRS, event sourcing, Kubernetes, or speculative infrastructure.
- Do not expose secrets or real demo credentials.
- Treat ignored local configs as local only unless explicitly requested.
- Prefer codebase conventions over new abstractions.
