package com.thelifestyle.travel.domain;

import java.util.List;

// The answer to a real-road-route request: the same resolved
// origin/destination/via and Haversine straight-line comparison
// RouteRecommendationResult already provides, plus a real road route when
// one was actually obtained. `roadRoute` is null whenever a real route
// wasn't available for any reason (provider disabled, timeout, no route,
// rate limited) — `roadRouteUnavailableReason` then explains why, and the
// straight-line fields are always present regardless, so this endpoint
// never returns nothing useful. `fuelCostEstimate` is only ever present
// alongside a real `roadRoute` — a cost estimate needs a real distance to
// estimate from.
public record RoadJourneyResult(
    RoutePoint origin,
    RoutePoint destination,
    RoutePoint via,
    List<RoutePoint> vias,
    List<RouteCandidate> suggestedViaCandidates,
    double straightLineDistanceKm,
    String straightLineLabel,
    RoadRoute roadRoute,
    String roadRouteUnavailableReason,
    FuelCostEstimate fuelCostEstimate) {}
