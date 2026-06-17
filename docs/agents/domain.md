# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- `CONTEXT.md` at the repo root for project positioning, domain language, and architecture boundaries
- `docs/adr/` for decisions that touch the area being changed
- `README.md` for public-facing portfolio narrative and demo evidence
- `docs/perf/README.md` when touching group-buy concurrency or performance claims

If these files do not cover the topic, proceed from code and note the documentation gap instead of inventing terminology.

## File structure

This is a single product repo with separated backend and frontend workspaces:

```
/
├── CONTEXT.md
├── docs/adr/
├── backend-environment/sky-take-out/
├── frontend-environment/sky-user-vue3/
└── frontend-environment/sky-admin-vue-ts/
```

## Use the glossary's vocabulary

Use the domain terms from `CONTEXT.md` when naming tests, refactors, issues, and explanations. This project is for Java backend interviews, so prefer precise backend language over vague product wording.

## Flag ADR conflicts

If output contradicts an existing ADR, surface it explicitly rather than silently overriding it.
