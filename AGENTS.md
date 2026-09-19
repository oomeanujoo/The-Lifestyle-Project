# Agent operating rules

Repository lives in one Mega-synced folder, mounted at different local paths on
two machines: `C:\Cloud Drive\Mega\Dev Projects\The Lifestyle Project` (other
machine) and `D:\Le\Mega\Dev Projects\The Lifestyle Project` (office laptop).
Both paths refer to the same synced content — an agent on either machine is in
the right place. Keep web, Travel, Property, docs, and scripts in this one
synced folder. Never move, clone, duplicate, or recreate it elsewhere; never
change Mega settings.

**Office laptop (`D:\Le\...` path) — hard restrictions, not just "needs
approval":** never run `git commit`, `git push`, or any command that connects
to a remote/origin (`git remote add`, `git fetch`, `git pull`, `git clone`,
etc.), and never change any system/OS configuration, environment variables,
installed software, or settings outside this project folder — regardless of
what approval process is stated elsewhere in this file. On this machine, stay
strictly inside this project folder: writing/editing code and docs (including
this file and `TECHNICAL_ARCHITECTURE.md`) is allowed under the normal
approval rules below, but committing, pushing, connecting to a remote
repository, or touching machine configuration is never allowed here, approval
or not.

## Start with context
Read `AGENTS.md`, then `docs/agent-context/START-HERE.md`,
`CURRENT-STATE.md`, `NEXT-STEPS.md`, and `SESSION-HANDOFF.md`.
Read `DECISIONS.md`, ADRs, architecture, and source only when relevant.
`TECHNICAL_ARCHITECTURE_DECISION_LOG.md` (the long-form narrative history,
split out of `TECHNICAL_ARCHITECTURE.md` on 2026-09-19) is never part of
the default read — open it only when a specific past decision's reasoning
is actually needed, not for routine context.
`AGENT_PROMPTS.md` (repo root) holds exact, copy-paste Docker/Compose
commands for Cline — starting/stopping/restarting containers, checking
status, tailing logs, running a curl check. Every command in it is pure
mechanical execution with no judgment call for Cline to make; Claude
supplies the specific URL/body for any live check and interprets the
pasted-back result. Use it instead of improvising an ops command each
time one is needed.
Use targeted `rg` searches; skip node_modules, .gradle, build, dist, IDE
caches, and Git object storage.

## Approval and Git
Default to read-only inspection and proposals. Before creating, editing,
deleting, moving, or generating any file or artifact, show exact affected
files, proposed content/diff, reason, and impact; wait for explicit approval.
The same applies to builds or tests that generate artifacts.

Read-only commands such as `git status`, `git rev-parse`, `git remote -v`,
`git branch --show-current`, `git log`, targeted `rg`, and reading ordinary
repository files need no approval.

Get separate explicit approval for (1) file/artifact changes, (2) Git
staging and commit, and (3) Git push. Remote changes, branch operations,
pull/merge/rebase, installs, environment changes, and starting services
also require approval. Never reset, clean, force-push, or discard work.
Do not treat “continue,” “okay,” or “looks good” as commit or push permission.

## Secrets
Never request or expose passwords, tokens, or private keys. Do not read the
external `LOCAL-ACCESS.md` without a stated need and explicit permission;
never copy it into this repository. Keep private keys in the local SSH
directory. Keep real `.env` files untracked; use placeholders only in
`.env.example`. Verify ignore rules before using a local secret file.

## Engineering
Preserve the Travel and Property bounded contexts and independent services.
Each service owns its schema and migrations; communicate through APIs.
Keep Java domain code framework-free, controllers thin, and API DTOs explicit.
Use the code-based React/TypeScript UI. Avoid paid services, authentication,
scraping, and new infrastructure without explicit approval. Do not silently
change dependency versions. After approved changes, request permission for
relevant tests and documentation updates.

Keep services stateless: no server-side session state between requests, so
scaling to multiple instances later needs no rework. Before adding a new
component, check whether an existing one already has the same shape and can
be reused by changing only its data/config; only build something bespoke when
generalizing the existing one would make it more complex than having two
simple, separate things. This is a personal, learning-focused project — write
code a developer new to it can follow, not clever-for-its-own-sake code.
Every backend endpoint should be discoverable through the service's Swagger
UI (`springdoc-openapi`) without hand-written API docs; do not add a new
endpoint without letting it show up there. When you add functionality worth
explaining, add an entry to `TECHNICAL_ARCHITECTURE_DECISION_LOG.md` using
its template — not every change, only functionality a future reader would
want explained. Keep `TECHNICAL_ARCHITECTURE.md` itself limited to the
current architecture (what the system is, not the history of how it got
there) — that separation is why the decision log was split into its own
file on 2026-09-19; do not let entries drift back into the architecture
document.

## Handoff
After an approved working session, propose a small update to
`CURRENT-STATE.md`, `NEXT-STEPS.md`, or `SESSION-HANDOFF.md` as needed.
Show its diff and wait for permission. Keep only confirmed facts and
actionable handoff details; never store secrets or full chat transcripts.
Do not commit or push without separate permission.
