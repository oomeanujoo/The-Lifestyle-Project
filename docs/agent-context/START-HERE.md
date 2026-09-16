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

1. `AGENTS.md`
2. This file
3. [Current state](CURRENT-STATE.md)
4. [Next steps](NEXT-STEPS.md)
5. [Latest session handoff](SESSION-HANDOFF.md)

That five-file set is the recommended maximum initial read. Open the
[decision log](DECISIONS.md), relevant ADRs, architecture documents,
or source directories only when the task needs them. Use targeted searches
and avoid generated directories. Confirm current Git status before proposing
changes.
