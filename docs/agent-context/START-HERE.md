# Start here

The Lifestyle is a local-first planning platform for travel and
property/cost-of-living decisions. The current work is Phase 0:
environment inspection and a safe, memory-efficient development setup.
Phase 2 product functionality has not begun.

## Repository map

- `lifestyle-web/`: React, TypeScript, Vite
- `travel-service/`: Java/Spring Boot Travel API
- `property-service/`: Java/Spring Boot Property API
- `docs/decisions/`: architecture decision records
- `scripts/`: local development and read-only reporting scripts

The target runtimes are Java 25 LTS and Node 24 LTS. Both backends use
Gradle 9.7.1 wrappers. PostgreSQL is the planned local database; Ollama
is optional and must not be required for non-AI use.

## Reading order for a new task

For multi-agent work, use [AI agent coordination](../agent-coordination.md)
as the map of roles and task files. Read it when coordinating agents, not as
part of every agent's initial context.

1. `AGENTS.md`
2. This file
3. [Current state](CURRENT-STATE.md)
4. [Next steps](NEXT-STEPS.md)
5. [Latest session handoff](SESSION-HANDOFF.md)

That five-file set is the recommended maximum initial read. Open the
[compact ADR index](DECISIONS.md), relevant ADRs, `TECHNICAL_ARCHITECTURE.md`,
or source directories only when the task needs them. Use targeted searches
and avoid generated directories. Confirm current Git status before proposing
changes.

`TECHNICAL_ARCHITECTURE_DECISION_LOG.md` (repo root) is a separate, much
longer file — the full narrative history of *why* things were built a
certain way, split out of `TECHNICAL_ARCHITECTURE.md` on 2026-09-19 because
it had grown to roughly 42% of that document's size. It is never part of
routine context for a new task; open it only when a specific past
decision's reasoning is actually needed. Do not confuse it with the
compact ADR index above — they serve different purposes and neither
duplicates the other.
