# Cline third assignment: one Travel road-route UI slice

Status: WAITING

Paused while Claude owns the urgent Travel UI takeover. Do not start this
task until Codex explicitly returns it to READY.

Start only after `cline-next-task.md` is DONE. Keep this task small; do not
expand it into another Settings or master-data project.

## Goal

Make the existing single Travel search display one dynamic car journey from
origin to destination with an optional via city. Use the backend road-route
response when available. Show distance, estimated driving time, source, and
any user-supplied fuel-cost estimate clearly. Never label a fuel estimate as a
train/bus/flight fare or claim it includes tolls or live traffic.

## File ownership

`lifestyle-web/`, this task file, and `agent-tasks/cline-03-result.md` only.
Do not edit Java, SQL, Compose, credentials, or Git state.

## Work

1. Reuse the single route search from the current Cline task. Preserve its
   origin/destination/via controls; do not create a second planner screen.
2. Add a small typed API adapter for the road-route endpoint defined in
   `agent-tasks/claude-04-result.md`. If that result is not available yet,
   prepare the UI against the agreed request fields without fabricating live
   data, and record the remaining binding step.
3. Make provider failure visible and preserve the existing approximate
   straight-line comparison as explicitly labelled fallback information.
4. Run focused UI checks after approval, record the result, and mark DONE.

Follow `AGENTS.md` approval rules. Do not commit or push.
