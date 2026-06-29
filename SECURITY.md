# Security Policy

## Supported Versions

This repository is a portfolio demo project. The public demo is maintained on the `main` branch only.

| Version | Supported |
|---|---|
| `main` | Yes |
| Older commits / forks | No |

## Reporting a Vulnerability

Please do not test destructive attacks against the deployed demo, scrape private data, or publish exploit details before contacting the author.

To report a vulnerability:

1. Contact the author privately through the GitHub profile linked to this repository.
2. Include affected endpoint/page, reproduction steps, expected impact, and whether the issue affects the deployed demo or only local development.
3. Do not include secrets, real personal data, or payment credentials in public issues.

Expected response target: best effort within 7 days.

## Scope

In scope:

- Authentication or authorization bypass
- Cross-user data access
- Admin write access from the read-only demo account
- Payment callback tampering or idempotency issues
- Secret exposure in committed files

Out of scope:

- Load testing, denial-of-service testing, or automated scanning against the live demo
- Attacks requiring compromised local developer machines
- Social engineering
- Issues in third-party services outside this repository

## Demo Credentials

The public admin demo uses a read-only account. Full admin credentials are intentionally not published in the repository.

If the read-only account can create, update, delete, cancel, confirm, deliver, complete, or otherwise mutate operational data, treat it as a security issue.
