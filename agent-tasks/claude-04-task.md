# Claude fourth assignment: sourced road journey for the Travel demo

Status: DONE

Start after `claude-03-task.md` is DONE. Keep the first vertical slice to car
travel for Mumbai/Pune to Gwalior, optionally through another city.

## Goal

Add a real road-route provider behind a Travel API port. Use verified master
coordinates for origin, destination, and via, then return provider road
distance and estimated driving duration. Retain the existing deterministic
straight-line comparison as an explicitly labelled fallback.

## File ownership

`travel-service/`, its focused tests, `TECHNICAL_ARCHITECTURE.md`,
`TECHNICAL_ARCHITECTURE_DECISION_LOG.md`, this task file, and
`agent-tasks/claude-04-result.md`. Do not edit UI, Integration, Property,
Compose, `.env`, local database state, Cline files, or Git state.

## Work

1. Use a configurable routing provider adapter. For the personal demo, the
   OSRM public demo can be tried with low request volume and clear source
   attribution; it has no availability guarantee. No new Docker service or
   unpinned image. Do not treat OSRM demo as an enterprise SLA.
2. Resolve city IDs/coordinates from Integration's central masters. Call the
   provider in origin, optional via, destination order. Validate response,
   timeout, no-route, rate-limit, and coordinate failures. Return road
   distance, estimated driving duration, data source/time, and route geometry
   only if provided. Do not invent roads or traffic conditions with AI.
3. If a cost is shown, compute a clearly labelled **fuel-cost estimate** in
   deterministic Java from explicit fuel price and vehicle efficiency inputs;
   exclude tolls, parking, and live prices unless a verified provider supplies
   them. Ticket fares for bus/train/flight remain unavailable until a real
   provider is integrated. AI may explain sourced results, not calculate them.
4. Keep current `/api/travel/v1/routes/recommend` behavior compatible or add
   a separate clearly named endpoint. Make the contract visible in Swagger.
   Add focused tests with fixed provider fixtures for direct and via journeys,
   failure fallback, and cost calculation.
5. Update current architecture and decision log as appropriate. Record exact
   endpoint/request/response contract, configuration names, tests, and live
   verification instructions for Cline/Codex in
   `agent-tasks/claude-04-result.md`. Mark DONE only for verified code.

Office laptop restrictions in `AGENTS.md` apply: no Git remote, commit, push,
machine settings, or files outside the synced project.
