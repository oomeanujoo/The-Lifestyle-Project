# Claude urgent takeover: one working Travel route search

Status: DONE
Priority: immediate, before any further READY Claude task

## Evidence and goal

The user sees GET `/api/travel/v1/route-planning/places` and POST
`/api/travel/v1/route-planning/places/refresh` fail with 404. The Travel page
still shows two hardcoded illustrative trip cards and a second place search.
Current source has a free-text `RoutePlanner` calling
`/api/travel/v1/routes/city-suggestions` and `/routes/recommend`, but
`TravelFolderPage.tsx` still renders the illustrative section. The live UI
may also be serving an old build. Deliver a single dynamic route search with
free-text From, To, and optional Via, with backend suggestions and a visible
real result or honest provider failure.

## Coordination and ownership

Read `agent-tasks/cline-task.md` and any Cline result first. Cline's task is
CANCELLED and its later UI tasks are WAITING. The user relayed Cline's explicit
acknowledgement of cancellation and confirmation that it will make no further
`lifestyle-web/` edits under that task. You may now own only
`lifestyle-web/`, this task, and `agent-tasks/claude-takeover-result.md`.
Preserve useful Cline changes; do not overwrite unreviewed work. Office laptop
restrictions in `AGENTS.md` apply: no Docker, local database, Git remote,
commit, push, machine settings, or files outside the synced project.

## Work and acceptance

1. Remove the illustrative-trip cards, their static fare/time facts, and the
   extra trip-search box from the Travel landing page. Keep one prominent
   route search; do not show a second `Route planner demo` panel.
2. Verify From, To, and Via accept free text and offer suggestions from the
   actual `/api/travel/v1/routes/city-suggestions` endpoint. Suggestions are
   central city master matches, not hardcoded frontend values. Ensure no UI
   code calls `/route-planning/places` or `/places/refresh`.
3. Wire the single search to the actual Travel route endpoint. Prefer the
   new `/api/travel/v1/routes/recommend-road` contract in
   `agent-tasks/claude-04-result.md`; show road distance, driving duration,
   source, and clearly labelled fuel estimate when available. When the road
   provider is unavailable, label the straight-line fallback honestly.
4. Add focused tests for the three free-text fields, suggestion request,
   direct and via search, provider error, and absence of illustrative cards.
   Run approved frontend lint/test/build only. Record exact files, tests,
   request URLs, and the local rebuild command for Codex in the result file.
5. Mark DONE only after checks pass. Do not claim a live browser fix from the
   office laptop; Codex owns the local Compose rebuild and browser verification.

Timebox: first report a concrete diagnosis and file plan within 30 minutes
of starting; aim to finish the focused change within 90 minutes. Report a
blocker immediately rather than waiting out the timebox.
