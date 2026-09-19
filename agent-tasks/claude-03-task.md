# Claude third assignment: complete the shared master refresh slice

Status: DONE

Start after `claude-next-task.md` is DONE. Work independently of Cline's UI
and local checks.

## Goal

Make `integration-service` the effective single owner of every current
`lifestyle_master` table. The Settings refresh must return an honest outcome
for each supported master without a 500, use real provider data where a
verified source exists, and never present AI guesses as authoritative rows.

## File ownership

`integration-service/`, its focused tests, `TECHNICAL_ARCHITECTURE.md`,
`TECHNICAL_ARCHITECTURE_DECISION_LOG.md`, this task file, and
`agent-tasks/claude-03-result.md`. Do not edit UI, Travel, Property, Compose,
`.env`, local database state, Cline files, or Git state.

## Work

1. Inspect existing central-master migrations, refresh code, provider
   responses, prompt registry, status API, and `claude-next-result.md`.
   Preserve the single-owner contract; remove application-code reads/writes
   to duplicate master tables only when the central API supplies them.
2. Remove arbitrary small refresh caps: import all valid currencies returned
   by Frankfurter, and expand configured Indian city coverage beyond the
   current seven using GeoNames with bounded paging/rate limits. Aim for at
   least 100 distinct, sourced Indian cities in a successful local refresh;
   report provider limits honestly rather than inventing rows. Make Indian
   pincode refresh use the central table and report failures precisely.
3. Keep transport mode, BHK type, and add-on masters as explicit accepted
   values with provenance. The AI acquisition path may propose additional
   values during refresh, but proposals stay DRAFT until explicit acceptance.
   For visa and locality hierarchy, return a clear UNSUPPORTED or
   UNVERIFIED status until an authoritative source and schema mapping exist;
   do not claim a successful populated master from a prompt alone.
4. Make one refresh response cover every current master category with counts,
   source, last success, and failure reason. Preserve last-known-good rows.
   Add focused tests for partial provider failure, duplicate data, prompt
   output rejection, and no-500 behavior. Keep endpoints in Swagger.
5. Update current architecture to match implemented behavior and record any
   significant decision in the separate decision log. Report exact API
   contract, migrations needed for Codex, tests, and remaining gaps in
   `agent-tasks/claude-03-result.md`. Mark DONE only for verified work.

Office laptop restrictions in `AGENTS.md` apply: no Git remote, commit, push,
machine settings, or files outside the synced project.
