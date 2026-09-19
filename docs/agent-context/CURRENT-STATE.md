# Current state

**2026-09-16 update from the D:\ office-laptop session (Java 8, cannot compile — separate machine from the paragraph below, synced via Mega into the same repo):** Built a Settings page (`lifestyle-web` `/settings`) plus a scoped master-data refresh feature (city + currency only, via GeoNames/Frankfurter through `integration-service`), reading the real `V2__domain_tables.sql` this other machine's session had already created before writing any new code against it. Added one additive migration per service (`V3__master_refresh_log.sql`), `MasterRefreshUseCase` + `IntegrationServiceClient` + JDBC repositories in `travel-service`/`property-service`, and a `FrankfurterClient` in `integration-service`. Frontend verified (`npm run lint`/`test`/`build` all pass); backend written but **not compiled** — same Java-25-toolchain gap as everything else built on this machine, reconfirmed via `./gradlew compileJava --offline` on all three services after these changes (still fails cleanly on the missing toolchain only). Full detail in `TECHNICAL_ARCHITECTURE_DECISION_LOG.md` (and `TECHNICAL_ARCHITECTURE.md` §20). Did not touch property's area/locality/pincode masters (data.gov.in's response schema is unverified) or attempt a transactions refresh (no transaction-creation UI/backend exists to pull from yet).

**2026-09-16 update for this Windows machine:** Portable Java 25 and Node 24 are installed outside the repository. Native PostgreSQL 18 has the existing `postgres` database with `travel`, `property`, and `integration` schemas and restricted service roles; no new database was created. The three services now point to their own schemas, and Integration has an AI draft-provenance migration plus an Ollama-first adapter. `ollama list` confirmed `qwen3:4b-instruct` (2.5 GB, ID `0edcdef34593`) for the 16 GB RAM profile, and a small `/api/chat` prompt returned `READY`. Only the optional Ollama container was then stopped to free RAM; the model persists in its Docker volume. Travel's test attempt stopped before tests ran with Gradle's `Unable to establish loopback connection` (`PipeImpl` / `SocketException: Invalid argument: connect`); Property and Integration tests have not run since. The historical snapshot below describes an older environment and is superseded by this paragraph. No Git staging, commit, or push occurred.

Snapshot: 2026-09-16 (re-verified same day; a prior snapshot earlier the
same day contained stale claims — see the note at the bottom). Recheck
facts that may change between sessions; do not trust this file blindly.

- Phase: Phase 0/1 boundary — environment inspection and local development
  setup. Phase 2 product functionality has not begun.
- Repository: one folder containing `lifestyle-web`, `travel-service`,
  `property-service`, `integration-service`, `docs`, and `scripts`. **It is
  not currently a Git repository** (`git status` fails with "not a git
  repository"). No commits, no remote, exist yet.
- Frontend: `npm ci`, lint, one test, and production build passed on
  2026-09-16 against locally installed Node **20.19.3**. The target LTS is
  Node 24; the installed version is neither 22 nor 24 — reinstall before
  relying on Node-24-only behavior.
- Backends: all three Gradle wrappers report 9.7.1. Locally installed Java is
  **1.8.0_481** (Java 8); a separate **17.0.6** JDK also exists on this
  machine at `D:\jdk-17.0.6`, but neither satisfies the `build.gradle`
  toolchain requirement (`JavaLanguageVersion.of(25)`). Backend
  build/startup, migrations, health, and info endpoints remain unverified
  under this toolchain — confirmed 2026-09-16 via `./gradlew compileJava
  --offline` on all three services, which fails cleanly with "Cannot find a
  Java installation ... matching {languageVersion=25}" and does **not**
  attempt to download one (no toolchain download repository configured, and
  none should be added on this machine per the no-system-changes rule).
- **New backend code added 2026-09-16, written but not compiled here** (same
  toolchain gap above): a `PlaceSearchUseCase` + `PlaceRepository` port +
  `InMemoryPlaceRepository` adapter + `PlaceSearchController`
  (`GET /api/{travel|property}/v1/places/search?q=`) in `travel-service` and
  `property-service` — the backend half of the DB-first/AI-fallback search
  feature designed in `TECHNICAL_ARCHITECTURE.md` §18, mirroring
  `lifestyle-web`'s `lib/placeSearch.ts` field-for-field. `InMemoryPlaceRepository`
  is a temporary in-process placeholder (no JPA, no real DB touched)
  standing in for the future Postgres query — swapping it for a JPA adapter
  later requires no change above the port interface. When local search
  finds nothing, both services return only the honest `AI_FALLBACK_NOTICE`
  string — neither calls the network itself. Verify by compiling on a
  machine with JDK 25 before trusting these classes compile/run.
- **Third microservice, `integration-service`, added 2026-09-16** (same
  toolchain gap — also unverified to compile), at the repo root alongside
  `travel-service`/`property-service`, same Gradle/Spring Boot 4.1.1/Java-25
  conventions and hexagonal layering, port `8083`
  (`GET /api/integration/v1/info`). Deliberately has **no**
  `adapter/out/persistence` and **no** JPA/Flyway/Postgres/H2 dependency —
  it owns no bounded-context schema, unlike the other two. Holds every
  outbound external-API call: `GroqAiProvider`/`MistralAiProvider`/
  `AiProviderRouter` (real chat-completions calls via `RestClient`,
  Resilience4j `RateLimiter`/`CircuitBreaker` from the framework-core
  modules constructed by hand, `resilience4j-ratelimiter`/
  `resilience4j-circuitbreaker` v`2.2.0` — unverified to resolve, since
  `--offline` fails on the toolchain step before reaching dependency
  resolution), exposed via `PlaceSuggestionUseCase` →
  `GET /api/integration/v1/ai/place-suggestions?q=`; `GeoNamesClient` and
  `DataGovInClient`, each reading its credential from an environment
  variable and returning an empty result (never throwing) when unset,
  exposed via manual-trigger endpoints in `MasterLookupController` since
  there's no scheduled refresh job to wire them into yet.
  **`travel-service`/`property-service` do not call `integration-service`
  over HTTP yet** — designed, not built. This code previously lived inside
  both domain services directly (an earlier 2026-09-16 pass); it was
  reverted out of them and rebuilt here instead — see
  `TECHNICAL_ARCHITECTURE_DECISION_LOG.md` for why. Required environment variables
  (names only — see `TECHNICAL_ARCHITECTURE.md` §22 for the full table,
  values live only in your own `LOCAL-ACCESS.md`): `GEONAMES_USERNAME`,
  `DATA_GOV_IN_API_KEY`, `DATA_GOV_IN_RESOURCE_ID` (already defaults to the
  registered pincode dataset), `GROQ_API_KEY`, `MISTRAL_API_KEY`,
  `GROQ_MODEL`/`MISTRAL_MODEL` (already default to a current model),
  `AI_REQUESTS_PER_MINUTE` (defaults to `20`). None are hardcoded anywhere
  in this repo.
- Docker: the `docker` CLI is not on PATH in this environment; the daemon
  status is therefore unknown/unavailable. PostgreSQL and the optional
  Ollama container cannot currently be started here.
- Database and AI: not started or connected. Ollama is optional; no model
  was downloaded.
- **Database temporarily disabled in both services (2026-09-16), at the
  user's request, so they run locally without PostgreSQL.** First attempt
  used `spring.autoconfigure.exclude` class names from Spring Boot 3.x
  (`org.springframework.boot.autoconfigure.jdbc.*` etc.) — these were
  **silently ignored** and the app still failed with "Cannot load driver
  class: org.h2.Driver", because Spring Boot 4.1 moved autoconfiguration
  classes into per-module packages (confirmed via Spring's own 4.1.0
  Javadoc). Fixed by exclud­ing the real classes —
  `org.springframework.boot.jdbc.autoconfigure.{DataSourceAutoConfiguration,
  DataSourceTransactionManagerAutoConfiguration,
  DataSourceInitializationAutoConfiguration}`,
  `org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration`,
  `org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration`,
  `org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration` —
  and added `runtimeOnly 'com.h2database:h2'` to both `build.gradle` files as
  a safety net so a future missed exclude degrades to a harmless in-memory DB
  instead of a hard failure. Still unverified end-to-end on this machine
  (Java 8 here, project needs 25 — `gradlew` can't even resolve a toolchain).
  Safe only because neither service has real persistence code yet (no JPA
  entities/repositories exist). `application-test.yaml` in both services
  still points at Postgres — dead config, not profile-scoped by the exclude
  list. **Revert** once Phase 2 adds real persistence code or Postgres is
  available again to verify against.
- Environment detail and memory measurements:
  [environment audit](../environment-audit.md) (re-verify before trusting;
  not re-checked in this pass).

**Correction note (2026-09-16, later same day):** an earlier version of this
file and of [SESSION-HANDOFF.md](SESSION-HANDOFF.md) claimed a Git `origin`
remote, a `.gitignore`, and a `.clinerules` file had been added, and that
Java 17 / Node 22 were installed. None of that matched the actual workspace
when re-checked: no `.git` directory exists, `.gitignore` and `.clinerules`
were absent, and the installed toolchain is Java 8 / Node 20.19.3. Treat any
single session's handoff as a claim to verify, not a fact to build on.

Do not turn an unverified item into a success claim without a new check.
