# Claude next result: backend AI master engine

Status of assignment: DONE (see `claude-next-task.md`)

## Summary

Ran real backend checks with a real, downloaded JDK 25 (see the "Java 25
note" in `claude-result.md` from the previous task — same mechanism, still
in place). Started after `claude-task.md` was DONE, independent of Cline,
as instructed.

## 1. Inspected before building

- Master refresh pipeline: `MasterDataRefreshUseCase` (city/currency/Indian
  pincode refresh, FAILED/PARTIAL outcomes, `master_refresh_log`).
- AI provider router: `AiProviderRouter` (Groq → Mistral → Ollama fallback,
  resilience-guarded, previously only `suggestPlaces()`).
- Coded-master CRUD: `CodedMasterRepository`/`CodedMasterType` — already a
  generic, safe, FK-free upsert path for `transport_mode`/`bhk_type`/
  `service_addon` (§10's one-repository-for-three-tables pattern).
- Provenance/draft precedent: `integration.ai_suggestion` (place
  suggestions only) and `integration.master_proposal` (built in the
  previous task — generic proposal table, already DRAFT/ACCEPTED/REJECTED).
- Schema: `lifestyle_master.bhk_type`/`transport_mode`/`service_addon`
  already exist; `integration.master_proposal` already exists with no
  DB-level restriction on which `master_type` values it accepts.

**Reused rather than rebuilt**: `CodedMasterRepository`, `CodedMasterType`,
`integration.master_proposal` (no new migration needed — see item 6 below),
`MasterRefreshOutcome` (reused directly as the acquisition outcome type,
same SUCCESS/PARTIAL/FAILED shape logged to the same
`master_refresh_log` table).

## 2. Versioned prompt definitions — one file per master type

New: `domain/PromptDefinition.java` (masterType, version, purpose, scope,
requiredFields, fieldFormats, allowedValues, codeFormatRegex,
evidenceRequirements, exampleResponse — every field the task asked for) +
`adapter/out/ai/PromptRegistry.java` (loads every `classpath:prompts/*.json`
at startup, keeps the highest version per master type, fails startup
loudly on a malformed file rather than silently running short).

Five prompt files, `src/main/resources/prompts/{masterType}.v1.json`, for
every master type Integration currently supports:
- `bhk-type.v1.json` — fixed `allowedValues` (STUDIO/1RK/1BHK.../5BHK_PLUS).
  **Fully wired end to end**, per the task's own instruction.
- `transport-mode.v1.json` — fixed `allowedValues` (FLIGHT/TRAIN/BUS/CAR/
  FERRY/METRO). Works through the same code path as bhk_type already;
  not yet requested to be called.
- `service-addon.v1.json` — `codeFormatRegex` only (no fixed list — the
  domain is genuinely open-ended, and the file says so).
- `city.v1.json`, `currency.v1.json` — **documentation-only**, explicitly
  marked as not wired to any acquisition endpoint in their own `purpose`
  field. These two masters already have real, verified providers
  (GeoNames, Frankfurter) and must keep using those, never an AI estimate.

## 3 & 4. Complete flow for `bhk_type`, reusable for the other two coded masters

New: `application/MasterAcquisitionUseCase.java` (`acquire(CodedMasterType)`),
`application/CodedMasterCandidate.java` (untrusted-JSON candidate shape),
`adapter/in/web/MasterAcquisitionController.java` — three endpoints:
`POST /api/integration/v1/masters/lifestyle/{bhk-types,transport-modes,
service-addons}/acquire`.

Per call: builds the prompt from its `PromptDefinition`, sends it via a new
`AiProviderRouter.complete(prompt)` (added alongside the existing
`suggestPlaces()`, same fallback/resilience behavior — required adding
`String complete(String prompt)` to the `AiProvider` port and all three
implementations), parses the reply as **untrusted JSON**, then per
candidate: rejects missing/blank fields, rejects a code that doesn't match
the prompt's `allowedValues`/`codeFormatRegex` (this check — done in Java
against a fixed, human-authored rule — is what "verified" means here;
never the model's say-so or provider agreement, per the task's own
instruction), rejects a duplicate against both existing real master rows
*and* already-pending DRAFT proposals (including a same-batch duplicate
within one AI response). Everything that survives becomes a DRAFT
`master_proposal` with `verification_status='VERIFIED'` and full
provenance (provider/model/prompt version/generation time). Produces a
`MasterRefreshOutcome` (SUCCESS/PARTIAL/FAILED) and logs it to the same
`master_refresh_log` table other refreshes use.

**Acceptance, extended in `MasterProposalUseCase.accept()`**: for a
`bhk_type`/`transport_mode`/`service_addon` proposal, accepting it now
**also writes the real `lifestyle_master` row** via
`CodedMasterRepository.upsert(...)` — attempted *before* flipping the
proposal's status, so a failure (duplicate code slipped through, malformed
data) leaves it DRAFT rather than ACCEPTED-with-nothing-to-show. For
`visa_requirement`/the locality hierarchy, acceptance still only flips the
proposal's own status — unchanged from the previous task, and explained
again (with the "why the asymmetry") in the decision log.

## 5. Refresh policy per master (documented in TECHNICAL_ARCHITECTURE.md §17.5)

- **Source priority**: AI provider fallback order unchanged
  (Groq → Mistral → Ollama, `AiProviderRouter`'s existing order).
- **Cadence**: manual trigger only (`POST .../acquire`) — no scheduler
  added; not asked for, and an unattended AI call creating proposals with
  no one reviewing them would just be silent accumulation.
- **Normalization**: `code` trimmed and upper-cased before any check.
- **Deduplication**: against real master rows and pending DRAFT proposals,
  including within one AI response batch.
- **Validation**: required fields present; code matches the prompt's fixed
  rule.
- **Failure behavior**: FAILED outcome on no provider configured, all
  providers failing, or unparsable AI output — never a silently-empty
  SUCCESS. Previously-saved masters and previously-created proposals are
  never touched by a failed or partial run (each proposal insert is
  independent, matching how `MasterDataRefreshUseCase` already behaves for
  city/currency).
- **Provenance**: provider name, model, prompt version, and a one-sentence
  evidence field, on every proposal.

## 6. Migration

**None needed.** `lifestyle_master.bhk_type`/`transport_mode`/
`service_addon` already existed (Codex's earlier schema work), and
`integration.master_proposal` (built in the previous task, this service's
own Flyway migration `V4__master_proposal.sql`) already accepts any
`master_type` string with no DB-level restriction — the type restriction
lives in Java (`MasterProposalUseCase`'s `UNVERIFIED_MASTER_TYPES` set),
which I extended in code, not schema. No migration proposal to hand you
this time.

## Also fixed along the way

`ObjectMapper` (needed to parse AI JSON output and stored `proposedData`)
had no autoconfigured bean in this Spring Boot 4.1.1 setup — the exact
same modularization gap already found and fixed for `RestClient.Builder`
in the previous task. Fixed the same way: new `config/JacksonConfig.java`
with an explicit `@Bean ObjectMapper`. Caught immediately by the real
Spring context-load test (`IntegrationApplicationTests`), not discovered
later.

## Test results (real JDK 25)

- `./gradlew test` (integration-service): **39/39 passed**, 0 failures —
  includes a real Spring context load (proves all 5 prompt JSON files
  parse correctly at startup, not just that the Java compiles).
- New test classes: `MasterAcquisitionUseCaseTest` (6 cases — success,
  no-provider-configured, unparsable-AI-output, unsupported-code-rejected,
  duplicate-against-real-master, duplicate-against-pending-proposal).
  `MasterProposalUseCaseTest` rewritten (was 5 cases, now 7 — added the
  two coded-master accept/reject-on-duplicate cases).
- `./gradlew bootJar`: real jar produced; confirmed all 5 prompt files are
  packaged at `BOOT-INF/classes/prompts/*.json`.

## Endpoints added

- `POST /api/integration/v1/masters/lifestyle/bhk-types/acquire`
- `POST /api/integration/v1/masters/lifestyle/transport-modes/acquire`
- `POST /api/integration/v1/masters/lifestyle/service-addons/acquire`

(Reused from the previous task, behavior extended: `POST .../proposals/
{id}/accept` now also writes the real master row for these three types.)

## Remaining blockers / things flagged for your judgment, not bugs

1. `transport_mode`/`service_addon` acquisition is implemented and tested
   but not yet triggered by anything — no scheduler, no UI button. Fine
   per "there is no review UI," but worth confirming manual-trigger-only
   is the intended long-term cadence, not just the MVP one.
2. `service_addon` has no fixed `allowedValues` list (open-ended domain by
   nature) — verification there is format-only. If you want a closed list
   for it too eventually, that's a one-file prompt-definition change, not
   a code change.
