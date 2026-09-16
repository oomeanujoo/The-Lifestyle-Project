# ADR-003: One PostgreSQL instance, isolated schemas

Status: accepted. One local PostgreSQL database minimizes setup while roles `travel_app` and `property_app` own only their respective schemas. Each service uses Flyway with its own history table. The shared instance is an infrastructure convenience, not shared data ownership. Cross-schema foreign keys and direct queries are prohibited. **2026-09-16 extension:** [ADR-006](ADR-006-integration-provenance-schema.md) adds an `integration_app` role and `integration` schema for AI/provider provenance in the same existing database.
