# Product & UI concept: entry experience and folder-style navigation

**Status: proposal, not built.** This captures an idea from a planning conversation so it isn't lost, not a committed design. Nothing here is implemented in `lifestyle-web` yet, and it should not be started in code before the [open questions](#7-open-questions-before-building) below are answered. See the linked decision-log entry in [TECHNICAL_ARCHITECTURE.md](../TECHNICAL_ARCHITECTURE.md#11-decision-log-append-only).

## 1. Entry experience

**Correction (2026-09-16):** an earlier version of this section described a maroon/red color scheme. `lifestyle-web` already has a real, established brand identity — the deep teal-green "L." logomark and palette below — and it should be **kept as-is**, not replaced. This section now matches that existing identity instead of inventing a new one.

A gate screen appears before the app proper, styled with the existing brand rather than a new one:

- The existing **"L."** logomark — a rounded square in the deep teal-green already used for the header brand mark (`#143f40`/`#174b44`), set in the existing serif treatment (Playfair Display/Georgia) — reused here at a larger size as the centerpiece, not redesigned.
- Wordmark **"The Lifestyle"** in the existing type pairing: Playfair Display serif for the display heading, DM Sans for supporting text — the same combination already used across the app's hero and placeholder sections.
- The existing palette carries the whole screen: deep teal-green (`#143f40`, `#174b44`) for the logomark and primary text, mid-green accents (`#1c685c`, `#21745b`, `#397266`) for interactive elements, warm cream (`#f8f6f1`) or soft sage (`#eaf0e8`) as background — not the app's usual white card surface, to make the gate feel distinct from the main app.
- A short tagline and a single **Enter** action, gated behind an **18+ confirmation**. Keep the whole screen **minimal and modern**: generous whitespace, one clear focal point (the logomark), no more than one or two lines of supporting copy, a single primary button — avoid decorative clutter.
- **Important distinction to keep in mind before building this:** this is a *tone-setting* UI gate, not a security or legal age-verification mechanism. It should never be described (to a user or in docs) as actual age verification — that would need a real, verified process and is out of scope. It's closer to how some drinks/lifestyle brand sites use an age gate as branding, not compliance.

**Style direction for everything below:** minimal and modern, not literally skeuomorphic — reuse the existing green/cream palette, card radii and shadow style already in `styles.css` (see the `.card` and `.hero` rules) rather than introducing new colors, gradients, or textures. The folder metaphor should read through icon shape and interaction, not through heavy decoration.

## 2. Post-entry navigation: the "desktop" metaphor

After Enter, the user lands on a desktop-like canvas with two top-level folders: **Travel** and **Property** — mirroring the project's two bounded contexts (see [docs/product-vision.md](product-vision.md)).

- **Interaction model:** double-click to open a folder (matching familiar desktop-OS convention), single click to select/highlight.
- **Inside the Travel folder:** one folder per trip, named with an arrow-based shorthand for the route — e.g. `Pune → Gwalior → Pune` for a round trip, or `Pune → Gwalior` for one-way.
- **Inside a trip folder:** a cover photo of a representative landmark (e.g. Gwalior Fort) centered with the trip title overlaid, and the day-by-day itinerary laid out below it.

## 3. Route and transport-mode modeling

A single trip can hold **multiple candidate routes** before one is chosen — e.g. "via Mumbai" and "via Delhi" — each with its own sequence of legs and transport modes (air / train / road / waterway).

This maps directly onto domain concepts already sketched in [docs/domain-model.md](domain-model.md):

- `TripPlan` — the trip itself (owns everything below).
- `Destination` (ordered) — the waypoints on a given route.
- `TravelLeg` with a `TransportMode` value — one hop between two waypoints, carrying its mode.
- `BorderCrossing` — relevant for international trips like Dubai.

A route alternative is **not** a separate trip — it's an alternative ordered set of `TravelLeg`s attached to the same `TripPlan`, shown as tabs or cards inside one trip folder, until the user picks one.

## 4. Worked example A — Dubai trip

Illustrative only — all prices/durations below are placeholders, not live data, consistent with the project's rule that AI/estimates are never authoritative (see [docs/ai-strategy.md](ai-strategy.md)).

| Day | Focus | Notes |
|---|---|---|
| 1 | Arrival, check-in, evening at Dubai Marina | Flight leg; visa requirement needs a verified source before this becomes real plan data |
| 2 | Burj Khalifa + Dubai Mall | Book Burj Khalifa slot in advance |
| 3 | Old Dubai: Gold Souk, Spice Souk, abra ride across the Creek | Good half-day, low cost |
| 4 | Desert safari (afternoon/evening) + free morning | Popular booked activity, treat price as a manual/import entry, not AI-invented |
| 5 | Optional Abu Dhabi day trip (Sheikh Zayed Mosque) | ~1.5 hr each way |
| 6 | Departure | |

Folder name: `Pune → Dubai → Pune` (round trip) or split as needed if a multi-city extension is added later.

## 5. Worked example B — Pune → Gwalior, two route alternatives

**Route 1 — via Mumbai**

| Leg | Mode | Notes |
|---|---|---|
| Pune → Mumbai | Road or train | Short hop, frequent departures |
| Mumbai → Gwalior | Flight or train | Flight is faster; train (e.g. via existing long-distance routes) is cheaper but longer |

**Route 2 — via Delhi**

| Leg | Mode | Notes |
|---|---|---|
| Pune → Delhi | Flight | Fastest way to cover the long north-bound leg |
| Delhi → Gwalior | Train or road | Gwalior sits on a well-served Delhi–Agra–Gwalior rail corridor |

Both routes are candidate `TravelLeg` sequences on one `TripPlan`; the UI would let the user compare them side by side (duration, mode, rough cost tier) before picking one. If the trip is a round trip, the folder is named `Pune → Gwalior → Pune`; the return leg can reuse either route or mix modes.

## 6. How this fits the existing architecture

- Entirely a **Travel-context and frontend-shell** concern — it does not touch Property or either backend's domain model beyond what [docs/domain-model.md](domain-model.md) already anticipates.
- It **is** a new information-architecture pattern (desktop/folder metaphor with double-click navigation) that is different from the more conventional "trip list / trip detail" screen flow implied by the current planned use of React Router in [TECHNICAL_ARCHITECTURE.md](../TECHNICAL_ARCHITECTURE.md). That difference is exactly why this stays a proposal, not a plan, until the open questions below are settled — [docs/product-vision.md](product-vision.md) would need a matching update before Phase 2 UI work starts down this path.

## 7. Open questions before building

- Is the desktop metaphor meant **literally** (draggable icons, window chrome, real double-click-to-open behavior) or **stylistically** (folder-shaped cards that just happen to need a click, styled to look like a desktop)? The literal version is a substantially bigger frontend build than a normal list/detail UI.
- Confirm the age gate is purely cosmetic/tone-setting and never represented as real age verification.
- Where do trip cover photos come from — user-uploaded, or a free/licensed image source? If sourced automatically, licensing and attribution need to follow the same provenance rules as [docs/free-data-sources.md](free-data-sources.md).
- A folder-desktop metaphor is harder to make accessible and responsive on small screens than a list view — should a simpler list/detail view exist alongside it (e.g. for mobile), or is desktop-only acceptable for now given this is a personal, local-first project?

## Status

**A first working version is implemented (2026-09-16)** in `lifestyle-web`: `EntryGate.tsx` (age-gate, wraps the app in `main.tsx`), a generic `FolderCard.tsx` (double-click to open, single-click to select — reused for the Dashboard's Travel/Property folders and for individual trip folders), `features/travel/TravelFolder.tsx` and `TripDetail.tsx` with the two worked examples above as static placeholder data (`tripData.ts`). The open question on literal-vs-stylized desktop metaphor (§7) is resolved pragmatically for now: **stylized**, not literal draggable icons/window chrome — double-click semantics and folder visuals, built with standard CSS/React, not an actual desktop-window simulation. Cover photos are still a styled gradient placeholder with a text label, not real images — that sourcing/licensing question is still open. Verified via `npm run lint`, `npm test`, `npm run build`, and the Vite dev server serving all new modules without errors; not verified with an actual screenshot (no browser-automation tool available in this environment). Logged in the [decision log](../TECHNICAL_ARCHITECTURE_DECISION_LOG.md).
