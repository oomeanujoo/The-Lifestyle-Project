# Cline next assignment: Settings refresh smoke test

Status: WAITING

Paused while Claude owns the urgent Travel UI takeover. Do not start this
task until Codex explicitly returns it to READY.

Start after `cline-task.md` is DONE. Work independently of Claude's backend
next task; do not wait for it.

## Goal

Verify only the Settings refresh button against the local running stack. Do
not build a review UI or add frontend master-authoring controls.

## File ownership

`agent-tasks/cline-next-task.md` and `agent-tasks/cline-next-result.md` only.
This is an inspection task; do not edit application code, SQL, Compose, `.env`,
credentials, or Git state.

## Work

1. After the current UI task is complete and the relevant containers are
   rebuilt with approval, click Settings refresh once. Record HTTP status,
   visible result, and whether the page shows an honest per-master error.
2. Read Integration logs for that one request and the refresh-status endpoint.
   Report the first concrete failing master and exact error category, without
   exposing keys. Do not repeatedly retry failed external providers.
3. Record the result in `agent-tasks/cline-next-result.md` and mark DONE. If
   this check needs backend or database changes, report the request to Codex;
   do not edit those files.

Follow `AGENTS.md` approvals. Do not commit or push without separate approval.
