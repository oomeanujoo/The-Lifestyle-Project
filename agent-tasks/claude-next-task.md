# Claude next assignment: backend AI master engine

Status: DONE

Start after `claude-task.md` is DONE. Work independently of Cline's local
verification task; do not wait for it.

## Goal

Make Integration responsible for prompt-driven master acquisition and refresh.
There is no review UI. Begin with one complete master type, such as BHK type,
then make the pattern reusable for other masters without building a large
generic framework. Keep Integration the sole master owner.

## File ownership

`integration-service/`, relevant backend tests,
`TECHNICAL_ARCHITECTURE.md`, `TECHNICAL_ARCHITECTURE_DECISION_LOG.md`, and
this task file / `agent-tasks/claude-next-result.md` only. Do not edit UI,
Compose, `.env`, SQL scripts, Cline files, or Git state.

## Work

1. Inspect the current master refresh pipeline, AI provider router, prompts,
   provenance table, APIs, and schema. Reuse existing components where they fit.
2. Create a versioned prompt definition file for each master type currently
   supported by Integration. Each definition must specify the purpose, scope,
   required fields, field formats, allowed values or code rules, evidence
   requirements, and an example of the structured response. Keep prompts in
   the backend repository and load them through one small prompt registry.
3. For one master type, implement a complete backend flow: request structured
   AI output, parse it as untrusted input, check required fields and types,
   normalize codes, reject duplicates and unsupported values, record provider,
   model, prompt version, references, generation time, and verification status,
   then produce a refresh outcome. A model's self-asserted citation is not
   independent verification. Multiple models agreeing is not proof.
4. Apply verified external-provider records through deterministic Java rules.
   Keep AI-only suggested records DRAFT until explicit user acceptance, as
   required by the existing project policy. Acceptance can be a backend API;
   it does not require a review UI. Never make an AI-only draft an active master
   automatically or expose it as an accepted dropdown value.
5. Give each master a refresh policy: source priority, cadence/manual trigger,
   normalization, deduplication, validation, failure behavior, and provenance.
   Preserve last-known-good rows if providers fail. Do not use AI to calculate
   transaction validity, prices, totals, percentages, sorting, or route scores;
   AI anomaly findings remain advisory.
6. If storage requires a migration, write the exact proposed migration and
   verification steps in `agent-tasks/claude-next-result.md` for Codex to apply;
   do not edit SQL outside your ownership. Update `TECHNICAL_ARCHITECTURE.md`
   with the implemented flow and remaining gaps. Put significant design
   reasoning in the decision log, not in the current-state architecture.
7. Run focused backend tests if the available JDK permits. Record endpoint
   contracts, files changed, tests, and blockers in
   `agent-tasks/claude-next-result.md`. Mark this task DONE when complete.

Follow `AGENTS.md` approvals. On the office laptop, never connect to a Git
remote, commit, push, or change machine settings.
