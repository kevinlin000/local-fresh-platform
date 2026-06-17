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

## Constraints

- Do not add microservices, CQRS, event sourcing, Kubernetes, or speculative infrastructure.
- Do not expose secrets or real demo credentials.
- Treat ignored local configs as local only unless explicitly requested.
- Prefer codebase conventions over new abstractions.
