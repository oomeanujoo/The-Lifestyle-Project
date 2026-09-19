# The Lifestyle

An AI-first, local-first workspace for planning travel and comparing places to live. Phase 1 establishes four independent applications, database ownership and a responsive navigation shell. Planning records, live prices, AI generation and PDF export are future work.

## Current local setup

This Windows machine has portable Java 25 and Node 24 outside the repository. The existing native PostgreSQL 18 instance uses its existing `postgres` database with separate `travel`, `property`, and `integration` schemas. Gradle tests currently stop before execution in the agent terminal because Java cannot establish a loopback connection to its single-use daemon; rerun them in a normal project terminal before treating the backend as verified.

## Architecture

| App | Stack | URL |
|---|---|---|
| `lifestyle-web` | React, TypeScript, Vite | http://localhost:5173 |
| `travel-service` | Java 25, Spring Boot, Gradle | http://localhost:8081/api/travel/v1/info |
| `property-service` | Java 25, Spring Boot, Gradle | http://localhost:8082/api/property/v1/info |
| `integration-service` | Java 25, Spring Boot, Gradle | http://localhost:8083/api/integration/v1/info |
| PostgreSQL | 18, existing `postgres` database, three schemas | localhost:5432 |

All three services expose `/actuator/health` and Swagger UI at `/swagger-ui.html`. The frontend proxies `/api/travel/**` and `/api/property/**` during development. Each service owns one schema and its Flyway migrations. Integration owns external API adapters and persists AI draft provenance; Travel and Property own domain data. See [architecture](docs/architecture.md), [AI capability roadmap](docs/ai-capability-roadmap.md), and [data strategy](docs/data-strategy.md).

## Repository tree

```text
.
├── AGENTS.md                 ├── compose.yaml
├── .env.example              ├── README.md
├── docs/                     │   ├── product-vision.md
│   ├── architecture.md       │   ├── domain-model.md
│   ├── ai-strategy.md        │   ├── ai-capability-roadmap.md
│   ├── data-strategy.md      │   ├── local-resource-profile.md
│   ├── free-data-sources.md  │   ├── roadmap.md
│   ├── technology-versions.md
│   └── decisions/ADR-001..005
├── lifestyle-web/            ├── travel-service/
├── property-service/         ├── integration-service/
└── scripts/
```

## Prerequisites

Java 25 LTS JDK, Docker Desktop or Docker Engine with Compose, Node.js 24 LTS and npm. `.java-version` and `.nvmrc` record the runtime majors; do not replace them with a newer non-LTS release. Git Bash/Linux is needed for the shell scripts. The checked-in Gradle wrappers download Gradle 9.7.1 on first run. No paid API, account, cloud resource or Ollama installation is needed. The UI is React/TypeScript source code; no low-code or proprietary builder is used.

## Start locally

From the repository root:

On this Windows machine, PostgreSQL is already running natively; skip the `docker compose up -d postgres` line. The Compose commands below are for a fresh isolated environment.

```sh
cp .env.example .env
docker compose up -d postgres
```

Run each process in a separate terminal:

```sh
cd travel-service && ./gradlew bootRun
cd property-service && ./gradlew bootRun
cd integration-service && ./gradlew bootRun
cd lifestyle-web && npm install && npm run dev
```

Run those last four commands from the repository root in separate terminals. On PowerShell use `Copy-Item .env.example .env`, `docker compose up -d postgres`, `cd travel-service; .\gradlew.bat bootRun`, then analogous commands in new terminals for Property, Integration and web. `scripts/start-local.sh` starts PostgreSQL and prints process commands; `scripts/stop-local.sh` stops PostgreSQL without deleting data.

Check `http://localhost:5173`, all three info URLs above, and `http://localhost:8081/actuator/health` / `http://localhost:8082/actuator/health` / `http://localhost:8083/actuator/health`.

## Full Compose stack with automatic rebuilds

From PowerShell at the repository root, run `./scripts/watch-compose.ps1` to build and start PostgreSQL and all four application containers, then watch for saved source changes. Use `./scripts/watch-compose.ps1 -WithAi` to include the optional Ollama container. Compose rebuilds and recreates only the affected application container when its files change. Keep this terminal open while developing; closing it stops the watcher. PostgreSQL data stays in its existing volume, and the default command leaves Ollama optional. The first build may take several minutes; later builds reuse Docker's Gradle cache.

External provider values belong in the ignored `.env`, not in Git. `GEONAMES_USERNAME` enables city lookups; Frankfurter needs no key. `DATA_GOV_IN_API_KEY`, `GROQ_API_KEY`, and `MISTRAL_API_KEY` enable their respective optional providers. The master refresh currently imports cities and exchange rates; data.gov.in is a sample lookup until its dataset schema has been verified for the property hierarchy. The Settings page reports each refresh result and error.

## Verify

```sh
cd travel-service && ./gradlew test
cd property-service && ./gradlew test
cd integration-service && ./gradlew test
cd lifestyle-web && npm ci && npm run lint && npm test && npm run build
```

Or `./scripts/verify-all.sh` from the root after PostgreSQL is up. PowerShell equivalents use `.\gradlew.bat test` in each service and the same npm commands. Confirm migrations and schema ownership with:

```sh
docker compose exec postgres psql -U lifestyle -d postgres -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname IN ('travel','property','integration') ORDER BY 1,2;"
```

Expected after successful service startup: each schema has its own `flyway_schema_history`; Travel and Property have `schema_marker`, and Integration has `ai_suggestion`.

## Database and environment

On this machine, use the existing native PostgreSQL `postgres` database; do not start a second Compose PostgreSQL container on the same port. The three restricted roles and schemas already exist. On a fresh isolated Compose volume, `scripts/init-db.sh` creates them in its `postgres` database. Each service uses its own role and JDBC `currentSchema`; Flyway migrations stay in the owning service. `.env.example` documents local defaults. Export matching `TRAVEL_DB_PASSWORD`, `PROPERTY_DB_PASSWORD`, and `INTEGRATION_DB_PASSWORD` for shell service processes if you override them. The frontend may use `lifestyle-web/.env.local` for Vite proxy targets.

Changing initial database role passwords in `.env` after a volume exists does not update existing roles. For an existing database, alter roles intentionally or recreate the local volume after backing up any data.

## Optional local AI and resource limits

The non-AI application uses only PostgreSQL; Ollama is excluded from the default Compose profile. Start the optional, version-pinned container with `docker compose --profile ai up -d ollama`. This does **not** download a model. On this 16 GB machine, the chosen model is `qwen3:4b-instruct`; pull it explicitly with `docker compose exec ollama ollama pull qwen3:4b-instruct`. Stop it with `docker compose --profile ai stop ollama`. Integration tries Ollama first at `OLLAMA_BASE_URL` (default `http://127.0.0.1:11434`), then configured cloud providers. The first request after the model unloads can take several minutes on this CPU; Compose allows a 10 minute model load and keeps the model in memory for 10 minutes after use. Successful suggestions remain unverified DRAFT records in `integration.ai_suggestion`. Travel, Property, and the web app continue working while Ollama is stopped.

Each service's `bootRun` defaults to a maximum Java heap of `512m`. Override in its shell with `TRAVEL_JVM_MAX_HEAP`, `PROPERTY_JVM_MAX_HEAP` or `INTEGRATION_JVM_MAX_HEAP` (PowerShell example: `$env:TRAVEL_JVM_MAX_HEAP='768m'`). For a packaged JAR, set `JAVA_TOOL_OPTIONS=-Xmx512m` or pass `-Xmx` to `java`; the Gradle `bootRun` setting applies only to development execution. These are heap limits, not total process-memory limits. Run `./scripts/system-info.sh` on Linux/Git Bash or `.\scripts\system-info.ps1` on PowerShell for a read-only OS, CPU, RAM, disk and tool-version report.

## Dependency upgrades

Upgrade deliberately, one stack at a time. Check the upstream stable release and Java/Node compatibility, change explicit versions and Docker image tags, regenerate `lifestyle-web/package-lock.json` when npm packages change, run relevant tests/builds, and update [technology versions](docs/technology-versions.md) with the reason and date. Prefer LTS runtimes over a newer non-LTS major. Do not use floating `latest` image tags, preview releases, or a paid dependency without explicit approval. Review migrations and release notes before changing PostgreSQL or Spring Boot.

## Troubleshooting

- `Cannot connect to Docker daemon`: start Docker Desktop/Engine; verify `docker info`.
- Java toolchain missing: install JDK 25 and set `JAVA_HOME`; `java -version` should report 25.
- Gradle wrapper cannot download: allow access to `services.gradle.org` or provide a configured cache. Do not substitute incompatible Gradle 8.x.
- npm install fails: check access to the npm registry and Node 24. `npm ci` requires the generated lockfile.
- Port in use: override `TRAVEL_PORT`, `PROPERTY_PORT`, `INTEGRATION_PORT`, `POSTGRES_PORT` or Vite's port/config as appropriate.
- Service starts before PostgreSQL: wait for `docker compose ps` to show healthy, then restart the service.
- Backend status shows unavailable in the UI: verify its info endpoint directly, then check Vite proxy target environment variables.

## Scope and next phase

The foundation includes schema ownership, health/info endpoints, the web shell, optional Ollama, and an Integration AI suggestion draft endpoint. Travel/property CRUD, routing, live price providers, scheduling, PDF generation, authentication, hosted deployment, and user acceptance of AI drafts remain future work. `integration.ai_suggestion` uses JSONB for referenced data; MongoDB, Redis, Kafka, Elasticsearch, MinIO, pgvector, and PostGIS are not installed. Next: verify migrations and backend tests, then build a vertical CRUD slice per domain.
