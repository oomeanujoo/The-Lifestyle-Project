# Latest session handoff

- Agent: Claude (Sonnet 5, Claude Code)
- Date/time: 2026-09-16
- User request: review the workspace; make sure any AI coding tool (Copilot,
  Cline, Cursor, Codex, Claude) can pick up project context from checked-in
  files instead of re-scanning the whole repo; keep `TECHNICAL_ARCHITECTURE.md`
  and this context set accurate. Standing rule: never commit or publish an
  Artifact without separate explicit permission, even if a plan says to.
- Work completed: added tool-specific pointer files so every tool's native
  entry point funnels to the same source of truth (`AGENTS.md` →
  `docs/agent-context/START-HERE.md`) instead of duplicating rules:
  `.github/copilot-instructions.md` (Copilot) and `.cursor/rules/project.mdc`
  (Cursor) were newly created; `.clinerules` (Cline) was re-created after
  being found missing despite a prior handoff claiming it existed.
  Re-verified environment facts and corrected stale claims in
  [CURRENT-STATE.md](CURRENT-STATE.md) (see its correction note).
- Files changed: `.github/copilot-instructions.md` (new),
  `.cursor/rules/project.mdc` (new), `.clinerules` (re-created),
  `docs/agent-context/CURRENT-STATE.md`.
- Commands: `git status`/`git remote -v` (confirmed not a Git repo),
  `java -version`, `node -v`, `docker --version` / `docker info`.
- Tests: none; only documentation and tool-config pointer files changed.
- Findings/blockers: the previous handoff entry (agent: Codex, same date)
  claimed a Git `origin` remote, `.gitignore`, and `.clinerules` had been
  added and that Java 17 / Node 22 were installed. None of that was true
  when re-checked in this session — no `.git` directory, no `.gitignore`,
  no `.clinerules`, and the actual toolchain is Java 8 (1.8.0_481) / Node
  20.19.3. The Docker CLI is not on PATH here either. Flag this to the user:
  either that prior work was done in a different environment/session that
  didn't persist here, or it was never actually applied.
- Exact next action: ask the user whether Git initialization, a
  `.gitignore`, and the Java 25 / Node 24 toolchain upgrade should now be
  set up for real — none of it exists yet. No commit, push, or Artifact was
  created in this session, per standing instruction.
- Git state: no `.git` directory; not a repository.
