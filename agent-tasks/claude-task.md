# Claude assignment (office laptop)

Status: DONE

## Goal

Make Integration the only writer/reader API for shared masters and make Travel
route recommendations use real saved places. Keep AI-produced master ideas as
reviewable drafts until accepted by a user.

## File ownership

`integration-service/`, `travel-service/`, applicable backend tests, and
`agent-tasks/claude-task.md` / `claude-result.md` only. Do not edit the UI,
Compose, `.env`, SQL scripts, or Cline's task/result files.

## Work

1. Inspect the existing `/api/integration/v1/masters/lifestyle` and
   `/api/travel/v1/routes` contracts. Work in parallel with Cline and preserve
   the current single-search UI contract and Swagger visibility. Record any
   proposed endpoint change in `claude-result.md` for coordination rather than
   editing Cline's files. Finish deterministic direct/via ranking
   from verified coordinates; never fabricate prices, travel times, or roads.
2. Switch GeoNames Indian postal writes/reads/status from
   `integration.city_pincode` to the already-created
   `lifestyle_master.city_pincode`. Do not drop the old table; Codex will
   migrate and remove it after verification.
3. Make provider I/O failures visible as FAILED/PARTIAL outcomes while keeping
   previously saved masters. For data without a verified provider (including
   visa rules and missing locality hierarchy), AI may create a proposal with
   provenance and verification status, but it must stay DRAFT until explicit
   user acceptance. Never silently promote AI text to an authoritative master.
4. Run focused backend checks available on the office laptop. If Java 25 is
   unavailable, report that limitation and leave execution to the local PC.

Do not use Git remotes, commit, push, or change office machine settings. Record
endpoint contracts, file changes, test results, and remaining blockers in
`claude-result.md`.
