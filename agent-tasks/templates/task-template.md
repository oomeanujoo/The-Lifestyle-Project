# <Agent name> assignment: <short title>

Status: WAITING

<!--
  Fill in, then set Status to READY when this task can start. Copy this
  file to agent-tasks/<agent-name>-task.md (or <agent-name>-next-task.md
  for a queued second assignment) — never invent a different shape.
  See agent-tasks/config/agents.yaml for the full onboarding checklist.
-->

## Goal

<One or two sentences: the outcome, not the steps. What should be true when
this is DONE that isn't true now?>

**Scope check before assigning**: does this Goal bundle more than one
independent deliverable ("build X, then Y, then also look at Z")? If yes,
split it into this task plus one or more `-next-task.md` files instead of
one growing Work list — see `docs/agent-coordination.md`'s "Task sizing and
timeboxing" section. A task with 6+ Work items spanning unrelated concerns
is a sign it should have been two tasks.

## File ownership

<The exact paths this agent may create/edit — as narrow as the task allows,
never a whole service when one file will do. Always include this task's own
task file and result file. Explicitly list what NOT to touch if there's any
ambiguity (another agent's files, Compose, .env, SQL scripts, Git state).>

## Work

1. <Step one — inspect before building; name what to read first.>
2. <Step two — the actual change, as concretely as you can make it without
   dictating implementation details the agent is better placed to decide.>
3. <Step three — verification: what real check proves this works? A test
   run, not "looks right.">
4. Record files changed, endpoint/contract changes, test results, and
   remaining blockers in `<agent-name>-result.md` (or `-next-result.md`).
   Mark this task DONE only for genuinely completed work; use BLOCKED with
   the exact blocker otherwise.

**If this turns out bigger than it looked**: checkpoint the result file
after each independently-useful piece of work — don't hold everything until
the end. If the remaining work is genuinely larger than this task's scope,
split the remainder into a new task file yourself and mark this one DONE
for the part actually finished, or mark it BLOCKED with "task larger than
assigned scope" and hand the re-split back to the director. Never leave
this file `IN_PROGRESS` with no checkpoint recorded in the result file.

<Any hard constraints specific to this task — never touch a git remote,
never delete data, never change machine settings, etc. Most of these
already live in AGENTS.md; only repeat one here if it's easy to miss.>
