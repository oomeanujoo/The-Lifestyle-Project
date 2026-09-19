# Cline assignment

Status: CANCELLED

Handoff reason (2026-09-19): The user still sees the obsolete
`/route-planning/places` 404 and hardcoded Travel cards. Claude is taking over
the Travel UI correction. Stop this task and acknowledge before further
`lifestyle-web/` edits; preserve all work already done.

## Goal

Make the local Travel UI show one clear route search and make Settings show the
shared master state clearly. Diagnose the browser's duplicate calls and stale
404 request without changing Java or SQL.

## File ownership

`lifestyle-web/` and `agent-tasks/cline-task.md` / `cline-result.md` only.
Use `AGENT_PROMPTS.md` for local Compose commands after the applicable approval.

## Work

1. Inspect current UI files. Keep a single From/To search with optional Via
   suggestion; remove the old `/route-planning/places` request and any duplicate
   route-planner presentation. Use the current `/api/travel/v1/routes/...` API.
2. In Settings, present every master type, count, last refresh result, and
   errors. Explain display currency as a separate UI preference.
3. Identify whether duplicate requests are two different endpoints, React
   development Strict Mode, or an actual duplicate effect. Fix only a real
   duplicate side effect; keep error and loading states honest.
4. Run relevant UI checks and report results. Rebuild and inspect local
   containers only after the required approval. Never start optional Ollama
   for this task and never delete the PostgreSQL volume.
5. Work in parallel with Claude. Keep the existing endpoint contract; if a
   backend change is needed, describe it in `cline-result.md` for coordination.
   Do not wait for Claude to finish before completing independent UI work.

Do not edit backend, database scripts, credentials, Git state, or the office
agent's source files. Put concise findings and commands/results in
`cline-result.md`.
