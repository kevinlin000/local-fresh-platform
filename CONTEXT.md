# Local Fresh Platform Context

## Project Positioning

`local-fresh-platform` is a Java backend portfolio project for job seeking. It is presented as `菜籃日 Cailan Day`: a Taiwan local fresh-produce B2C ecommerce platform with group-buy free-shipping mechanics.

The project should demonstrate solid Java backend fundamentals rather than excessive architecture novelty.

## Core Narrative

The product combines:

- Local fresh-produce browsing and ordering
- Cart and checkout flow
- Shipping address management
- Group-buy flow where users invite others to reach free-shipping conditions
- Merchant/admin operations for products, orders, categories, and store state
- Deployed demo evidence on AWS

## Architecture

- Backend: Java 17, Spring Boot 3.5, MyBatis, PageHelper, Flyway, JWT, Druid, Actuator
- User frontend: Vue 3 + Vite
- Admin frontend: Vue 3 + Vite + TypeScript + Element Plus
- Data stores: MySQL 8, Redis 7
- Distributed coordination: Redisson
- Deployment: AWS EC2, S3, CloudFront, DuckDNS, Nginx, Docker
- External services: Google OAuth 2.0, Google Maps API

## Important Domain Terms

- `Member`: end user who browses products, creates orders, and joins group buys
- `Product`: sellable fresh-produce item or box
- `Cart`: member's pending purchase list before checkout
- `Order`: member-specific purchase record
- `GroupBuy`: group-buy activity with a target participant count and expiry
- `GroupBuyParticipant`: link between a member and a group-buy activity; points to the member's pre-order
- `Pre-order`: order created during group-buy participation before the group succeeds
- `Store`: merchant-side operational entity for order handling and product operations
- `Admin`: management-side user for operational maintenance

## Engineering Priorities

- Keep changes practical and interview-defensible.
- Prefer clarity, testability, and evidence over adding features.
- Preserve the group-buy concurrency story: Redisson lock, transaction boundary, unique participant constraint, and performance evidence.
- When touching public portfolio claims, keep README, screenshots, and implementation aligned.
- When touching database behavior, inspect schema and migration history first.

## Boundaries

- Do not over-engineer this into microservices, CQRS, event sourcing, or Kubernetes without explicit instruction.
- Do not publish agent-generated issues or PRDs to GitHub by default.
- Do not expose demo credentials or secrets in docs.
