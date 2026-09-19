# Claude urgent takeover result: one working Travel route search

Status of assignment: DONE (see `claude-takeover-task.md`)

## Diagnosis (confirmed before any edit)

`RoutePlanner.tsx`/`routePlanningApi.ts` were already correct and live —
despite the filename, they called the real `/api/travel/v1/routes/
city-suggestions` and `/routes/recommend` endpoints. **No code anywhere
called `/route-planning/places` or `/places/refresh`** (full source
search, confirmed clean again after this change). The 404s the user saw
are a stale running container built before that earlier fix — not a
source bug. `TravelFolderPage.tsx` was the real, confirmed problem: it
rendered `RoutePlanner` (correct) followed by a full "Illustrative trips"
section with a second `PlaceAutocomplete` search box and a static
`FactsBoard` — exactly the duplicate search + hardcoded cards described.

## Coordination

Read `cline-task.md` (CANCELLED, handoff reason recorded) before touching
anything. Per the takeover protocol, held all `lifestyle-web/` edits until
explicit confirmation that Cline's session had acknowledged stopping — the
user relayed that confirmation directly, and the task file was updated to
reflect it, before any file below was touched.

## Changes

- **`TravelFolderPage.tsx`** — rewritten to render only the page heading
  and `RoutePlanner`. Removed: the illustrative-trip card grid, static
  fare/time facts (`FactsBoard`/`buildFacts`), the second
  `PlaceAutocomplete` search box, and all their now-unused imports/state
  (`trips`, `placeFilter`, `visibleTrips`). `tripData.ts` itself is
  untouched — other pages (trip detail) may still read it; only this
  landing page's rendering changed.
- **`routePlanningApi.ts`** — added `recommendRoad()` + `RoadRoute`/
  `FuelCostEstimate`/`RoadJourney` types, calling the new
  `GET /routes/recommend-road` contract from `claude-04-result.md`.
- **`RoutePlanner.tsx`** — the single search now calls **both**
  `recommendRoad()` (primary result: real road distance/duration/source
  when available, or an honestly-labeled straight-line fallback with the
  reason why) and `recommendRoute()` (kept only for its ranked
  "suggested via cities, least detour" chips, which `recommend-road`
  doesn't provide — both resolve the same origin/destination against the
  same masters, so an unknown-city error surfaces identically from
  either). Result rendering shows road distance + driving duration +
  source + captured time + a clearly-labelled fuel-cost estimate when a
  real route came back; otherwise the approximate straight-line distance
  and its existing honest label, plus the specific unavailability reason.

## Tests added/updated

- `RoutePlanner.test.tsx` — rewritten: mocks both `recommendRoad`/
  `recommendRoute`; new cases for a real road route with fuel estimate
  displayed, and the honest straight-line fallback with its reason;
  existing cases (free-text inputs, live suggestions, via-candidate
  chips, error surfacing, pending-state button) updated for the dual call.
- `routePlanningApi.test.ts` — added 3 cases for `recommendRoad` (direct,
  with via, honest 400 error) mirroring the existing `recommendRoute`
  tests exactly.
- `TravelFolderPage.test.tsx` (new) — confirms exactly one route search
  renders, and that the illustrative cards / second search box are gone.

## Verification (real, run on this machine)

- `npx vitest run` — **35/35 passed** (up from 28).
- `npm run lint` — clean.
- `npm run build` — clean (`tsc --noEmit` + `vite build`, 348.38 kB bundle).

## Request URLs

- `GET /api/travel/v1/routes/city-suggestions?q=<prefix>`
- `GET /api/travel/v1/routes/recommend?origin=&destination=&via=`
- `GET /api/travel/v1/routes/recommend-road?origin=&destination=&via=`
(all called from `RoutePlanner`'s single search — no other endpoint is
called anywhere in the Travel UI)

## For Codex — local rebuild and live verification

Not claimed as fixed in a live browser from this machine, per the task's
own instruction. To actually confirm the 404/stale-illustrative-card
report is resolved, rebuild the `lifestyle-web` container so it picks up
this source (the "Restart and rebuild everything" prompt in
`AGENT_PROMPTS.md` covers this: `docker compose down` then `docker compose
up -d --build`), then open the Travel page and confirm: only one search
box, no illustrative cards, and no `/route-planning/*` requests in the
network tab. With `OSRM_ENABLED=false` (the current default), expect the
straight-line fallback with its unavailability reason on every search —
that's expected, not a bug; set `OSRM_ENABLED=true` to see a real road
route.

## Remaining gaps (not blockers, flagged for judgment)

- `PlaceAutocomplete`/`tripData.ts`/`FactsBoard`/`buildFacts` are now
  unused by this page but left in place — other pages (Property, trip
  detail views) may still use some of these; deleting them wasn't in this
  task's scope and risked breaking something outside `lifestyle-web`'s
  Travel folder that wasn't inspected here.
- No live "saved trips" list exists yet (still the deferred "save route as
  trip" gap from an earlier session) — the Travel landing page is now
  purely a search tool, with no way to browse previously-created trip
  plans from the UI. Out of scope for this takeover.
