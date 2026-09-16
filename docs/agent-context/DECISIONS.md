# Compact decision log

Append confirmed decisions; link to detail rather than copying ADRs.

| Date | Decision | Reason and consequence | Detail |
|---|---|---|---|
| 2026-09-16 | One monorepo | Review related apps together; keep each runnable independently | [ADR-001](../decisions/ADR-001-monorepo.md) |
| 2026-09-16 | Separate Travel and Property services | Preserve bounded contexts and API ownership | [ADR-002](../decisions/ADR-002-microservices.md) |
| 2026-09-16 | One PostgreSQL instance with separate schemas | Simple local setup with distinct schema ownership | [ADR-003](../decisions/ADR-003-single-postgres-separate-schemas.md) |
| 2026-09-16 | Optional AI provider abstraction | Non-AI workflows must work without Ollama | [ADR-004](../decisions/ADR-004-ai-provider-abstraction.md) |
| 2026-09-16 | Append-only price snapshots | Preserve historical comparisons and provenance | [ADR-005](../decisions/ADR-005-price-snapshot-strategy.md) |
