# Onboarding a new agent — the checklist

This is the concrete, copy-from-here version of the routing/registry design
in `docs/agent-coordination.md` and `agent-tasks/config/agents.yaml`. Follow
it whenever a new AI tool (a different CLI, a new IDE extension, a CI bot)
needs to join this coordination system.

1. **Register it.** Add an entry to `agent-tasks/config/agents.yaml` — name,
   role, cost tier, environment, capabilities, and its own task/result file
   paths. `proven_on` starts empty; that's expected, fill it in as it
   actually completes work.
2. **Give it a task file.** Copy `task-template.md` (this folder) to
   `agent-tasks/<agent-name>-task.md`. Every task file uses the same shape —
   Status line, Goal, File ownership, Work, and a note on what to record in
   the result file — so no agent needs a bespoke parser or a human to
   remember a one-off format.
3. **Give it a result file location.** `result-template.md` is the shape;
   the file itself (`agent-tasks/<agent-name>-result.md`) gets created by
   the agent when it actually reports, not by whoever onboards it.
4. **Give it an entry point it actually reads on startup.** The pattern
   already in use: `CLAUDE.md` for Claude, `.clinerules` for Cline — a short
   file that points at `docs/agent-coordination.md` for the shared protocol
   and at its own task file for the current assignment, and says nothing
   else. Don't duplicate protocol text into the new entry point; link to it.
5. **Add a routing row.** `docs/agent-coordination.md`'s "Routing work and
   measuring capability" table needs one new row: what kind of work goes to
   this agent, and when to escalate away from it. An agent with no routing
   criterion just accumulates whatever task text happens to mention it —
   the same "orphaned agent" failure mode a reference framework (AIDLC, see
   `docs/agent-coordination.md`'s design-lineage note) documents for agents
   that are wired up but never actually invoked by anything.
6. **Start it at WAITING or READY, never IN_PROGRESS.** The director (or
   whoever assigns the first task) decides which — READY only once the task
   is genuinely ready to be picked up, matching the existing status machine.

That's the whole checklist. Nothing here requires touching another agent's
task file, result file, or owned source paths — the new agent's entire
footprint is one YAML entry, two new files, and one new table row.
