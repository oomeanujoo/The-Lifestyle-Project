# ADR-006: Integration owns an AI provenance schema

Status: accepted, 2026-09-16.

## Context

Integration originally held external API adapters only and had no database. Once AI suggestions need provider, model, prompt, source, verification, and user acceptance history, that metadata needs durable ownership without crossing into Travel or Property's domain tables.

## Decision

Use a third `integration` schema and restricted `integration_app` role in the existing PostgreSQL `postgres` database. Integration owns its Flyway migrations and stores successful place suggestions in `integration.ai_suggestion` as `UNVERIFIED` and `DRAFT`. Referenced data uses JSONB; no separate NoSQL store is introduced. Travel and Property continue to own only their own schemas and must call Integration's API for AI results. No new database is created on this machine.

## Consequences

Integration now requires PostgreSQL for startup and draft persistence, while Ollama remains optional. A stopped Ollama process cannot block non-AI workflows. User acceptance is a separate future operation; saving a draft never changes plan data. Database migration and backend startup still require verification after the local Gradle loopback error is resolved.
