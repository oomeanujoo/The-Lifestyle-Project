# Claude result (office laptop)

Status of assignment: DONE (see `claude-task.md`)

## Summary

Ran real backend checks — Java 25 *is* available here after all (see
"Java 25 note" below), so nothing had to be deferred to the local PC.

## 1. Route recommendation contract — inspected, already satisfied, no change needed

`GET /api/travel/v1/routes/recommend` (`RouteRecommendationController` →
`RouteRecommendationUseCase`, travel-service) already:
- Resolves origin/destination/via against real, refreshed
  `lifestyle_master.city` data and integration-service's staged Indian
  pincode data (`LifestyleMasterReadPort.findCity`/`findAllCities`/
  `findPincode`) — never a hardcoded or fabricated place.
- Computes direct and via-candidate distances deterministically
  (Haversine great-circle, `GeoDistance`), ranked by
  `Comparator.comparingDouble(RouteCandidate::detourKm).thenComparing(label)`
  — same, reproducible order every time for the same master data.
- Never invents a fare, travel time, schedule, or real road route — the
  response is explicitly labeled as an approximate straight-line distance.

This was already built and covered by 6 existing tests
(`RouteRecommendationUseCaseTest`) from earlier work this session — I
verified it for real with a downloaded JDK 25 rather than re-building
something that already existed. **No endpoint or response-shape change
made** — the single-search UI contract and Swagger visibility are
unaffected.

## 2. Switched GeoNames Indian postal data to `lifestyle_master.city_pincode`

Files changed (integration-service only):
- `application/port/out/CityPincodeRepository.java` — `upsert(...)` no
  longer takes `cityName`/`countryCode` (the new table doesn't store
  them; they're read back via a join instead).
- `adapter/out/persistence/JdbcCityPincodeRepository.java` — rewritten to
  read/write `lifestyle_master.city_pincode` (joined to
  `lifestyle_master.city` for name/country), matching the existing
  `provider_metadata->>'source'` convention `JdbcLifestyleMasterCityRepository`
  already uses. **`integration.city_pincode` is untouched and unwritten
  from now on** — left in place for you to compare/migrate/remove.
- `domain/CityPincode.java`, `application/MasterDataRefreshUseCase.java`,
  `adapter/in/web/MasterDataController.java`,
  `adapter/in/web/PincodeMasterResponse.java`,
  `adapter/out/persistence/JdbcMasterRefreshLogRepository.java` — comments
  and the `india_pincode` status-count table switched from
  `integration.city_pincode` to `lifestyle_master.city_pincode`.
- `test/.../MasterDataRefreshUseCaseIndiaPincodeTest.java` — updated for
  the trimmed `upsert(...)` signature.

**No HTTP contract change** — `/api/integration/v1/masters/lifestyle/pincodes`,
`/pincodes/{code}`, and `/refresh-status`'s `india_pincode` entry return the
exact same JSON shape as before; only the underlying table changed.
Travel-service needed no changes for this — it only ever talks to
integration-service's HTTP API, never the database directly.

**For your migration/cutover**: `lifestyle_master.city_pincode` requires a
real `city_id` FK into `lifestyle_master.city` (the old
`integration.city_pincode.city_id` was a plain, unenforced uuid column) —
worth double-checking every existing staged row's `city_id` actually
resolves before/while migrating, in case any staged row predates a city
that was since re-upserted under a different id.

## 3. Provider-failure visibility — already satisfied; added the AI-proposal/draft path

**Already in place, verified, no change needed**: `MasterDataRefreshUseCase`
already returns FAILED/PARTIAL outcomes (never silently treating a real
GeoNames/Frankfurter failure as "0 results"), records every attempt to
`integration.master_refresh_log`, and never rolls back or deletes
previously-saved masters on a later failure (each upsert commits
independently).

**New**: a generic, reviewable AI-proposal path for the two kinds of
master data with no verified provider (`visa_requirement` text, and the
`municipality`/`area`/`locality`/`pincode` hierarchy — §19, no free source
exists for either):
- `V4__master_proposal.sql` (new Flyway migration, integration-service's
  own schema) — `integration.master_proposal`: one generic table reused
  across master types (same convention as `ai_suggestion`/
  `master_refresh_log`), with `verification_status`
  (UNVERIFIED/VERIFIED/REJECTED) and `acceptance_status`
  (DRAFT/ACCEPTED/REJECTED) columns, starting and defaulting to DRAFT.
- `domain/MasterProposal.java`, `MasterProposalOutcome.java`,
  `application/port/out/MasterProposalRepository.java`,
  `adapter/out/persistence/JdbcMasterProposalRepository.java`,
  `application/MasterProposalUseCase.java`,
  `adapter/in/web/{MasterProposalController,MasterProposalRequest,
  MasterProposalResponse,ApiErrorResponse}.java` — new CRUD-and-review
  stack, new endpoints under `/api/integration/v1/masters/lifestyle/proposals`
  (`POST` to propose, `GET` to list by status — defaults to DRAFT,
  `POST /{id}/accept`, `POST /{id}/reject`).
- `MasterProposalUseCase` refuses `masterType` values outside
  `{visa_requirement, municipality, area, locality, pincode}` — every
  other master already has a verified provider or CRUD path (`/cities`,
  `/currencies`, `/transport-modes`, etc.) and must use that instead of a
  proposal.
- New test: `MasterProposalUseCaseTest` (5 cases).

**Deliberately NOT built, by design, not an oversight**: accepting a
proposal only flips its own `acceptance_status` to ACCEPTED — it does
**not** automatically write into `visa_requirement`/`municipality`/etc.
Turning an accepted idea into a real `lifestyle_master` row stays a
separate, manual step through the existing masters API. Reasoning: those
tables have real FK/hierarchy constraints (e.g. `locality` requires a real
`area`→`municipality`→`city` chain) that a generic accept-button cannot
safely satisfy — auto-promoting here would risk exactly the kind of
silent/fabricated master row the task explicitly rules out. Flagging this
choice for review rather than silently deciding it's final — if you want
acceptance to also stage a ready-to-review row against the real target
table (still short of writing it), that's a reasonable next step but a
different, larger piece of work.

**Also not wired in this pass**: nothing currently calls `POST /proposals`
automatically — `AiProviderRouter`/the existing AI providers are not yet
triggered for visa/locality data. The task said AI *may* create a
proposal; the reviewable-draft mechanism now exists and is tested, but
actually prompting an AI provider for visa-rule or locality-hierarchy text
is a separate, not-yet-requested feature.

## Java 25 note

This machine was previously believed to only have Java 8, based on every
earlier `--offline` Gradle run failing on "no toolchain configured." That
was a real limitation for offline builds, but not for builds with network
access: adding the standard `org.gradle.toolchains.foojay-resolver-convention`
plugin to `settings.gradle` (all three services, done earlier this session)
lets Gradle auto-download a genuine JDK 25 here. This is how every check
below was actually run and verified — not skipped.

## Test results (real JDK 25, not the toolchain-gap failure)

- `./gradlew test` (integration-service): **30/30 passed**, 0 failures.
- `./gradlew test` (travel-service, unaffected by this task's changes):
  **40/40 passed**, 0 failures — re-confirmed, not re-built.
- `./gradlew bootJar` (integration-service): real jar produced, the exact
  task the Dockerfile runs.

## Remaining blockers

None for the work assigned. Two things worth your attention, not blockers:
1. The `city_id` FK note under item 2, above.
2. Whether accepted proposals should stage a target-table-ready row (see
   item 3's "deliberately not built" note) — a scope decision, not a bug.
