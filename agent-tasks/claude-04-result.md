# Claude fourth result: sourced road journey for the Travel demo

Status of assignment: DONE (see `claude-04-task.md`)

## Summary

Added a real road-route provider (OSRM's free public demo) behind a new
`RoadRouteProvider` port in travel-service, with a deterministic fuel-cost
estimate and an honest fallback to the existing straight-line comparison
whenever a real route isn't available. Real JDK 25 used throughout — see
the Java 25 note in `claude-result.md`.

## 1. Provider adapter — configurable, opt-in, low-volume by design

`OsrmRoadRouteProvider` (new, `adapter/out/routing/`) calls
`{OSRM_BASE_URL}/route/v1/driving/{lon,lat;...}` — OSRM's public demo
server, no key, explicitly no SLA. Disabled by default (`OSRM_ENABLED=
false`); no new Docker service, no unpinned image. This is the first
genuinely external HTTP call travel-service makes on its own — every prior
outbound call went through integration-service (the single owner of
`lifestyle_master`); OSRM is a real-time routing query on arbitrary
coordinates, not master data, so it doesn't fit that model and is called
directly, behind its own port.

**A real bug caught before shipping**: the first draft gave this adapter
its own `SimpleClientHttpRequestFactory` for a dedicated timeout, mutating
the shared, singleton `RestClient.Builder` bean (`RestClientConfig`) in the
process — since that bean is shared, this would have silently applied
OSRM's short demo timeout to `IntegrationMasterDataClient` too, depending
on Spring's construction order. Fixed by dropping the custom factory and
matching `GeoNamesClient`/`FrankfurterClient`'s exact pattern (inject the
shared builder, call `.build()`, nothing else) — also what made this class
properly testable with `MockRestServiceServer`.

## 2. Resolution and calling order

`RouteRecommendationUseCase.recommendRoad(origin, destination, via)` reuses
the exact same city/pincode resolution `recommend()` already has
(`toPoint()`/`resolveVia()`/`hasCoordinates()`, no duplicated logic) —
same real `lifestyle_master.city`/staged-pincode data, same honest errors
for an unknown place. Calls the provider in origin → via → destination
order. Every failure category is validated and classified explicitly:
missing/invalid coordinates, OSRM's own "no route" response, HTTP 429
(rate limited), any other provider/HTTP error, and timeouts — via
`RoadRouteProviderException.reason()` (`NOT_CONFIGURED`/
`INVALID_COORDINATES`/`NO_ROUTE`/`RATE_LIMITED`/`PROVIDER_ERROR`/
`TIMEOUT`).

## 3. Fuel-cost estimate — deterministic Java, clearly labeled

`FuelCostEstimate.compute()`: `distanceKm / vehicleKmPerLitre *
fuelPricePerLitre`, from explicit config (`FUEL_PRICE_PER_LITRE`,
`VEHICLE_KM_PER_LITRE`, `FUEL_COST_CURRENCY`) — never AI, never a live
price. Only computed when a real road route was obtained (needs a real
distance). Its own `label` field states in the API response itself that it
excludes tolls/parking/live prices and isn't a total trip cost. Ticket
fares (bus/train/flight) remain unavailable — no real fare provider exists.

## 4. Contract — additive, not breaking

New endpoint: `GET /api/travel/v1/routes/recommend-road?origin=&
destination=&via=`. Existing `/routes/recommend` is completely unchanged —
kept separate deliberately, since Cline's own in-flight task owns the
single-search UI contract against that exact response shape; changing it
mid-flight risked breaking parallel work. Response
(`RoadJourneyResponse`): resolved origin/destination/via, the always-present
straight-line comparison, and — only when a real route was obtained —
`roadRoute` (distanceKm/durationMinutes/geometry/source/capturedAt) and
`fuelCostEstimate`; otherwise `roadRouteUnavailableReason` explains why.
Both endpoints are ordinary `@RestController`s, so both are Swagger-visible
automatically, no hand-written docs.

## Test results (real JDK 25)

- `./gradlew test`: **50/50 passed**, 0 failures (up from 40 — 5 use-case
  tests for `recommendRoad` covering direct/via journeys, provider-disabled
  fallback, provider-throws fallback, and cost calculation; 6 adapter tests
  for `OsrmRoadRouteProvider` covering success, no-route, rate-limited,
  server-error, invalid-coordinates-without-a-network-call, and
  disabled-without-a-network-call).
- `./gradlew bootJar`: real jar produced.

## Migrations needed for Codex

**None.** No schema change — this is application-layer-only (a new outbound
adapter, use-case method, config, and endpoint).

## Live verification for Cline/Codex

Once `OSRM_ENABLED=true` is set on a machine with real network access:

```
curl "http://localhost:8081/api/travel/v1/routes/recommend-road?origin=Mumbai&destination=Pune"
curl "http://localhost:8081/api/travel/v1/routes/recommend-road?origin=Mumbai&destination=Gwalior&via=Pune"
```

Expect a real `roadRoute.distanceKm` noticeably larger than the straight-
line `straightLineDistanceKm` for the same pair (a real road is never
shorter than a great-circle line), and a `fuelCostEstimate.amount`
consistent with the configured `FUEL_PRICE_PER_LITRE`/`VEHICLE_KM_PER_LITRE`.
With `OSRM_ENABLED=false` (the default), expect `roadRoute: null` and a
populated `roadRouteUnavailableReason` on every call — confirms the
fallback path without needing network access.

## Remaining gaps (not blockers, flagged for judgment)

- OSRM demo's real availability/rate limits weren't tested live from this
  machine (no outbound network access here) — the classification logic is
  tested against fixed fixtures, not a real OSRM response. Worth a live
  check once this is exercised where the network actually works.
- No caching of road-route results — every request calls OSRM fresh. Given
  the low-volume, personal-demo framing, this seemed like the right
  simplicity tradeoff rather than adding a cache layer for a request volume
  this low.
