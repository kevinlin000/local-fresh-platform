# ADR 0001: Keep Local Fresh Platform Focused on Java Backend Portfolio Value

## Status

Accepted

## Context

This repo is a job-seeking Java backend portfolio project. It started from the Sky Take Out course project and has been adapted into a Taiwan local fresh-produce ecommerce platform with group-buy mechanics, AWS deployment, and performance evidence.

The project needs to be easy to explain in interviews. Its value comes from complete business flow, backend correctness, deployment evidence, and practical concurrency handling.

## Decision

Keep the project focused on practical Java backend fundamentals:

- Spring Boot backend implementation
- MySQL schema and transaction correctness
- Redis/Redisson usage for cache and group-buy concurrency
- Vue frontends as supporting product surfaces
- AWS deployment and performance evidence

Avoid adding heavyweight architecture patterns unless they directly improve the portfolio story and can be defended in an interview.

## Consequences

- Prefer small, concrete improvements over broad rewrites.
- Use tests, docs, screenshots, and performance evidence to support claims.
- Avoid unneeded microservices, Kubernetes, CQRS, event sourcing, or speculative infrastructure.
