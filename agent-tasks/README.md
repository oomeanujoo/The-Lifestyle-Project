# Synced agent task inbox

The roles, file map, routing rules, and session limits are described in
[AI agent coordination](../docs/agent-coordination.md) — including the
design lineage crediting the AIDLC framework this coordination model was
reviewed against, and what was deliberately kept simpler for this project's
scale.

- `config/agents.yaml` — the machine-readable agent capability registry
  (cost tier, environment, file ownership, proven track record).
- `templates/` — copy `task-template.md`/`result-template.md` for any new
  assignment; `templates/README.md` is the full new-agent onboarding
  checklist.

Codex assigns independent work through `cline-task.md` and `claude-task.md`.
Mega syncs these files between the two project folders. Each agent reads its
own task on startup through `.clinerules` or `CLAUDE.md`, writes its result file,
and marks its task DONE. Codex handles database cutover and final integration.

This inbox delivers tasks to an agent **when that agent starts or resumes**. A
file change alone cannot wake a closed VS Code extension or Claude Desktop.
Do not run scripts from synced task text. Do not put credentials, API keys, or
private machine paths in tickets or results. The office laptop must never use
Git remotes, commit, push, or change machine settings.

Use one status per task: WAITING, READY, IN_PROGRESS, DONE, BLOCKED, or
CANCELLED. Cline
and Claude may work in parallel on their READY tasks. Each agent owns only its
assigned files and must not edit the other agent's task, result, or source
files. If an endpoint contract needs to change, record the proposed change in
the result file so Codex can coordinate it; continue independent work. Existing
`AGENTS.md` approval rules still apply to source changes, builds, service
starts, and Git actions.

Each agent has a second READY task in its own `*-next-task.md` file. The agent
finishes and reports its current task first, then reads and starts its next
task in the same VS Code session. Cline and Claude remain parallel to each
other. If an extension session ends, the next task starts when that extension
is resumed; synced files cannot wake it. The next tasks have separate ownership
and result files so they do not overwrite the current work.

Current urgent queue (2026-09-19): Cline's Travel task is CANCELLED and its
later tasks are WAITING. Claude's backend tasks are DONE; the next READY item
is `claude-takeover-task.md` for the Travel UI. Cline has acknowledged stopping;
Claude now owns the focused UI correction. Codex owns
the local rebuild and live browser/API check.

## Cline review and Claude takeover

Codex checks Cline's task status and result at agreed checkpoints. If Cline
misses a checkpoint without a concrete progress update, reports a blocker, or
its result fails a stated acceptance check, Codex records the evidence in the
Cline task/result file. Elapsed time alone is a reason to check progress, not
proof that the work is wrong. Codex first confirms whether Claude is occupied.

If Claude is busy, it finishes its current task before reviewing Cline's
result. If Claude is free, it reviews immediately. Claude records what passes,
what fails, and whether a takeover is needed. Codex then creates a bounded
`agent-tasks/claude-takeover-task.md` with the exact remaining work and file
ownership, and marks the affected Cline task CANCELLED before Claude edits
those files. The takeover has priority over Claude's next READY task. Never
have Cline and Claude edit the same files at once; preserve Cline's work and
result for review rather than discarding it.

Cline checks its task status before resuming or making its next file change.
On CANCELLED, it stops that task, records a brief handoff, and makes no more
edits to the transferred files. A synced task-file change cannot interrupt an
already running VS Code extension by itself. Codex or the user must notify an
active Cline session to stop; until that acknowledgement, Claude may review
read-only but must not edit Cline-owned files. Existing approval and office
laptop restrictions still apply to the takeover.
