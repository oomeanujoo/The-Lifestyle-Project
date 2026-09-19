# <Agent name> result: <short title>

Status of assignment: <DONE | BLOCKED — matches the status line in the task file>

<!--
  Copy alongside the task file: agent-tasks/<agent-name>-result.md.
  Write this file incrementally WHILE working, not only at the end — if the
  task turns out to take longer than one sitting, whatever is written here
  so far is the only signal anyone else has that real progress is
  happening. An empty or stale result file next to a task stuck
  IN_PROGRESS is exactly the "silently taking too long" failure this
  template exists to prevent — see docs/agent-coordination.md's "Task
  sizing and timeboxing" section.
-->

## Summary

<Two or three sentences — what changed and why, for someone who has not
read the task file.>

## What was inspected before building

<What you read/checked first, and what you decided to reuse rather than
rebuild — the equivalent of "don't build a second thing that already has
this shape.">

## Changes

<Files changed, grouped by what they do, not a raw diff. Note any endpoint
or contract change explicitly — the other workers and the director need to
know without reading your diff.>

## Verification

<The real check that was run — a test suite, a live request, a build — and
its actual result. "Looks correct" is not verification.>

## Remaining blockers / flagged for judgment

<Anything genuinely blocking DONE (use this section plus Status: BLOCKED in
the task file), and separately, anything you deliberately chose one way but
want the director/user to weigh in on. Distinguish the two — a blocker
stops the task; a flagged judgment call doesn't.>
