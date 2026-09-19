# Agent task prompts — copy the whole block, paste it as your message to Cline

Every block below is written as a complete, ready-to-send message — copy
it exactly as-is into Cline's chat. Each one already tells Cline the
exact command, exactly which services are involved, and exactly what to
paste back. Cline should never need to decide anything or interpret
whether something "looks right" — that judgment happens afterward, by
whoever reads what Cline pastes back.

---

## Prompt: Restart and rebuild everything (use this one most of the time)

> Please rebuild and restart the entire application from scratch, in this
> exact order. Run each command exactly as written, waiting for each one
> to finish before running the next, and paste back the complete, raw
> terminal output of every single command — do not skip any, do not
> summarize any, do not say whether something "looks right."
>
> Step 1 — stop everything currently running:
>
> `docker compose down`
>
> Step 2 — rebuild and start all six services fresh (the six services are
> `postgres`, `ollama`, `lifestyle-web`, `travel-service`,
> `property-service`, and `integration-service` — you do not need to name
> them individually, this one command rebuilds and starts all of them,
> in the correct dependency order, automatically):
>
> `docker compose up -d --build`
>
> Step 3 — confirm every one of the six services is actually running and
> healthy:
>
> `docker compose ps`
>
> Paste back the raw output of all three commands, in order, exactly as
> printed on screen. If any service shows as unhealthy, restarting, or
> exited in the Step 3 output, paste that too — don't leave it out even
> if it looks like a problem.

---

## Prompt: Start everything (without rebuilding — use only if you're sure nothing changed)

> Run this exact command:
>
> `docker compose up -d`
>
> This starts all six services defined in compose.yaml: `postgres`,
> `ollama`, `lifestyle-web`, `travel-service`, `property-service`, and
> `integration-service`. Startup order is handled automatically — do not
> start them individually or in any particular order yourself. Paste back
> the complete, unedited terminal output. Do not summarize it or say
> whether it worked — just paste exactly what printed.

---

## Prompt: Check what's currently running

> Run this exact command:
>
> `docker compose ps`
>
> Paste back every row of the output exactly as printed, including the
> status column for each of the six services. Do not summarize or
> describe it — paste the raw table.

---

## Prompt: Stop everything

> Run this exact command:
>
> `docker compose down`
>
> Paste back the complete, unedited terminal output.

---

## Prompt: Restart one specific service

> Run this exact command, replacing `SERVICE_NAME` with the exact name I
> give you (one of: `postgres`, `ollama`, `lifestyle-web`,
> `travel-service`, `property-service`, `integration-service`):
>
> `docker compose up -d --build SERVICE_NAME`
>
> Paste back the complete, unedited terminal output.

---

## Prompt: Full clean restart after a compose.yaml change

> `docker compose watch` does not notice edits to compose.yaml itself —
> only a real restart applies them. Run these two commands, in this
> order:
>
> `docker compose down`
>
> `docker compose up -d`
>
> Paste back the complete, unedited output of both commands.

---

## Prompt: Start Compose Watch (auto-rebuild on code changes)

> Run this exact command:
>
> `docker compose watch`
>
> This runs in the foreground — do not close the terminal it's running
> in, or the auto-rebuild stops working entirely. Paste back the first
> screen of output once it starts running.

---

## Prompt: View recent logs for one service

> Run this exact command, replacing `SERVICE_NAME` with the exact name I
> give you:
>
> `docker compose logs --tail 100 SERVICE_NAME`
>
> Paste back the complete, unedited output.

---

## Prompt: Push everything to GitHub (checks ignores + LOCAL_ACCESS.md first — run this yourself, not Cline)

> This one is for you to run yourself in your own terminal, not to hand to
> Cline — it's a one-time/occasional push, not a repeated dev-loop task,
> and the first run needs you to type in your own GitHub remote URL.
>
> Step 1 — from inside the project's root folder (the folder that
> directly contains `compose.yaml` — on any machine, `cd` there first,
> since the path differs per machine), check whether this is already a
> git repository:
>
> `git rev-parse --is-inside-work-tree`
>
> If that prints `true`, skip to Step 3. If it errors out ("not a git
> repository"), continue to Step 2.
>
> Step 2 — initialize the repository (only needed once, ever):
>
> `git init`
>
> `git branch -M main`
>
> Step 3 — confirm `.gitignore` at the project root already excludes
> secrets before anything is staged. Run:
>
> `cat .gitignore`
>
> Confirm the output includes, at minimum: `.env`, `LOCAL_ACCESS.md`,
> `node_modules/`, `**/build/`, `**/.gradle/`. If any of those lines are
> missing, STOP and add them to `.gitignore` before continuing — do not
> stage anything until they're present.
>
> Step 4 — see exactly what would be staged, and read it carefully:
>
> `git status`
>
> Look at every file listed. If you see `.env`, `LOCAL_ACCESS.md`, or
> anything else that looks like it could contain a real password or API
> key, STOP — do not proceed — fix `.gitignore` first (Step 3) and re-run
> this step. Only continue once the listed files are all source code,
> docs, or config you actually intend to publish.
>
> Step 5 — stage and commit:
>
> `git add -A`
>
> `git status` (again — confirm the staged file list one more time before
> committing; this is your last checkpoint to catch a secret before it's
> permanently in history)
>
> `git commit -m "Update project"`
>
> Step 6 — connect the remote (only needed the first time ever, or if the
> remote isn't set yet — check first with `git remote -v`; if it already
> shows a URL, skip this step):
>
> `git remote add origin YOUR_GITHUB_REPO_URL_HERE`
>
> (Replace `YOUR_GITHUB_REPO_URL_HERE` with the actual HTTPS or SSH URL of
> the GitHub repo you created — e.g.
> `https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git`. Record this URL
> in `LOCAL_ACCESS.md` under "GitHub remote URL" so you don't have to look
> it up again next time.)
>
> Step 7 — push:
>
> `git push -u origin main`
>
> Paste back the complete, unedited output of every command above, in
> order — including Step 4's file list — so it can be double-checked
> before anything goes live.

---

## Prompt: Run a specific web request and report back

> Run this exact command, exactly as written:
>
> `curl -s -w "\nHTTP %{http_code}\n" URL_GOES_HERE`
>
> (If I give you a POST request instead, use this form:
> `curl -s -w "\nHTTP %{http_code}\n" -X POST URL_GOES_HERE -H "Content-Type: application/json" -d 'BODY_GOES_HERE'`)
>
> Paste back the entire raw output exactly as printed, including the
> `HTTP nnn` line at the end. Do not summarize the response and do not
> say whether it looks correct — just paste it exactly.
