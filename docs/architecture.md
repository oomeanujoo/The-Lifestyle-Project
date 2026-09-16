# Architecture

`lifestyle-web` is a React SPA. Its Vite development proxy sends `/api/travel/**` to port 8081 and `/api/property/**` to 8082. Each Spring Boot service has its own domain, application use cases, inbound web adapters, outbound persistence/provider adapters and configuration. Domain classes have no Spring or JPA imports; REST DTOs and persistence entities remain adapter concerns. The Phase 1 info controller explicitly maps a domain status record to a web response DTO.

One PostgreSQL 18 instance contains `travel`, `property`, and `integration` schemas in the existing `postgres` database. Separate login roles own those schemas. Each service owns its Flyway history; Integration also stores AI draft provenance. No service reads another service's schema. Cross-context workflows call APIs; there is no shared Java domain library or cross-schema foreign key.

## Future contracts

Integration owns the current `AiProvider` port and tries local Ollama before optional Groq and Mistral. Its place-suggestion endpoint records successful generations as `DRAFT` with provider, model, prompt version, generation time, referenced data, and verification status. An absent AI process must never block service startup. Travel and Property do not yet call this endpoint; accepting a draft into plan data remains future work.

Ollama is an optional Compose `ai` profile pinned to a fixed image tag. The default Compose start excludes it and never downloads a model. This machine's chosen model is `qwen3:4b-instruct`, selected explicitly for its 16 GB RAM class. See [AI capability roadmap](ai-capability-roadmap.md) and [local resource profiles](local-resource-profile.md).

Each context will own a `PriceProvider` port returning price quotes with source, currency, observed time and confidence. Manual entry and import adapters come before live provider integrations. Refresh policy will be configurable as 15 days, 30 days or manual only. A scheduler is deferred.

Normalized prices and provenance will use typed PostgreSQL columns, with `jsonb` for variable external-provider metadata. Snapshots remain append-only. Money calculations, percentages, sorting and route scores are deterministic Java operations, never LLM output. See [data strategy](data-strategy.md).

Travel PDF export will render a versioned HTML itinerary template and convert it with a free Java library such as OpenHTMLtoPDF. A future `GET /api/travel/v1/trips/{id}/itinerary.pdf` returns `application/pdf` with an explicit snapshot timestamp. The document includes overview, route sequence, daily activities, transport and border crossings, accommodation, budget, booking references, emergency information, notes and checklist. No PDF code is in Phase 1.

The frontend's production API origin will be supplied by deployment configuration or same-origin reverse proxy. Vite proxy targets are environment variables for development only.
