# Claude third result: complete the shared master refresh slice

Status of assignment: DONE (see `claude-03-task.md`)

## Summary

Closed the gap between "the Settings refresh button exists" and "it gives
single-owner-quality coverage." Real JDK 25 used throughout (see the Java 25
note in `claude-result.md`), so everything below is compiled and tested for
real, not just checked against the toolchain-only gap.

## 1. Inspected before building

Read `MasterDataRefreshUseCase`, `GeoNamesClient`/`FrankfurterClient`,
`MasterRefreshProperties`, the prompt registry/acquisition path built in the
previous task, `MasterDataController`'s `/refresh-status`, and
`claude-next-result.md`. Confirmed no duplicate master-table reads/writes
remain inside `integration-service` itself (the `integration.city_pincode`
→ `lifestyle_master.city_pincode` switch was already completed and verified
in the previous task; nothing else duplicates a central table here).

## 2. Removed the small hardcoded caps

- **City refresh** (`GeoNamesClient.searchByCountry` — new method): bulk,
  paged discovery via `searchJSON?country=IN&featureClass=P&orderby=
  population`, bounded by new `app.masters.india-city-target-count`
  (100)/`-page-size` (100)/`-max-pages` (5) config. Named `seed-cities`
  (Pune, Mumbai, Gwalior, Delhi, Dubai, Bengaluru) still looked up by exact
  name first, for backward compatibility and Dubai's non-Indian example.
  One page (100 results) is normally enough to hit the 100-city target in
  one GeoNames call; paging exists for when it isn't.
- **Currency refresh**: `FrankfurterClient.latest(base)` — dropped the
  `symbols` parameter entirely (Frankfurter's own documented way to return
  every currency it supports for a base). `app.masters.tracked-currencies`
  removed as dead config. `MasterLookupController`'s diagnostic passthrough
  endpoint updated to match.
- **Indian pincode refresh**: now reads Indian cities straight from
  `lifestyle_master.city` (the central table) instead of name-matching the
  old 6-city seed list, bounded by new `app.masters.india-pincode-city-
  limit` (default 20) — one `searchPostalCodes` call per city, so refreshing
  all 100+ discovered cities in one pass would otherwise burn most of an
  hourly GeoNames quota on a single click.

## 3. Coded masters and unsupported masters — mostly already correct

`transport_mode`/`bhk_type`/`service_addon` already had exactly the
required shape from the previous task: explicit CRUD, plus AI-drafted
proposals that stay DRAFT until an explicit accept call — and because these
three are flat/FK-free, accepting one does apply it to the real master
table (safe here specifically, unlike the hierarchical masters below). No
code change needed for this part.

**New**: `/refresh-status` now returns an explicit `UNSUPPORTED` status
(synthesized at read time, never written to `master_refresh_log` — there
was no real attempt to log) for `visa_requirement`/`municipality`/`area`/
`locality`/`pincode`, instead of a bare `null` that could be misread as
"just hasn't run yet." Every status row also now carries `source` (a short,
honest description of where that master's data comes from) and
`failureReason` (the real error text from the last FAILED/PARTIAL attempt).

## 4. One refresh response, no 500s

`/api/integration/v1/masters/lifestyle/refresh-status` now returns, per
master: `masterName`, `recordCount`, `lastRefreshedAt`, `lastStatus`,
`source`, `failureReason`. New tests added:

- `MasterDataRefreshUseCaseCityDiscoveryTest` (3 cases): paging across
  multiple GeoNames pages until the target is reached, stopping early when
  GeoNames genuinely runs out of results (not a bug), and — the concrete
  "no-500" proof — a Frankfurter response with zero rates never throws, it
  returns an honest `PARTIAL` outcome.
- `GeoNamesClientTest` (+3 cases for `searchByCountry`): success, explicit
  provider-error-shape failure, skip-when-unconfigured.
- Existing `MasterDataRefreshUseCaseRefreshAllTest`/
  `MasterDataRefreshUseCaseIndiaPincodeTest`/`FrankfurterClientTest` updated
  for the new signatures.

Swagger contract: `/refresh`, `/refresh-status`, `/refresh/india-geo` paths
unchanged; `MasterStatusResponse`'s new `source`/`failureReason` fields are
additive, not breaking.

## 5. Architecture and decision log

`TECHNICAL_ARCHITECTURE.md` §17.4 table updated (city/currency dependency
rows, `UNSUPPORTED` status noted for the five no-source masters) plus new
§17.5.1 summarizing this pass. Decision log entry added with the
reasoning for each of the three changes above.

## Test results (real JDK 25)

- `./gradlew test`: **45/45 passed**, 0 failures (up from 39 — 6 new tests:
  3 city-discovery/currency, 3 GeoNames `searchByCountry`).
- `./gradlew bootJar`: real jar produced.

## Migrations needed for Codex

**None.** No schema change — this pass only changed application-layer
refresh logic, config, and the status response shape (additive fields).

## Remaining gaps (unchanged from before, not new)

- `municipality`/`area`/`locality`/`pincode` still have no refresh code at
  all — the data.gov.in field-to-column mapping is still not implemented,
  now clearly reported as `UNSUPPORTED` instead of silently `null`.
- `refreshAll()` still does not auto-trigger AI acquisition for coded
  masters — that stays a separate, explicit `POST .../acquire` call by
  design (an AI provider call should never be a surprise side effect of a
  routine refresh click).
