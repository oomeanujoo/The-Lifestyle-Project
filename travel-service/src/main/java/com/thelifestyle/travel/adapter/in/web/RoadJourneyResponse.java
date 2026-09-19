package com.thelifestyle.travel.adapter.in.web;

import java.util.List;

// `roadRoute`/`fuelCostEstimate` are null together whenever a real road
// route wasn't available — `roadRouteUnavailableReason` then explains why.
// `straightLineDistanceKm`/`straightLineLabel` are always present, the
// same deterministic Haversine fallback `/routes/recommend` already
// returns — this endpoint never returns nothing useful.
public record RoadJourneyResponse(
    RoutePointResponse origin,
    RoutePointResponse destination,
    RoutePointResponse via,
    List<RoutePointResponse> vias,
    List<RouteCandidateResponse> suggestedViaCandidates,
    double straightLineDistanceKm,
    String straightLineLabel,
    RoadRouteResponse roadRoute,
    String roadRouteUnavailableReason,
    FuelCostEstimateResponse fuelCostEstimate) {}
