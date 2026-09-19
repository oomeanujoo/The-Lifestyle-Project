package com.thelifestyle.travel.adapter.in.web;

import java.util.List;

// distanceLabel is repeated on every response — deliberately, so a caller
// that only looks at directDistanceKm/detourKm still sees, inline, that
// these are approximate straight-line measures, never a real road route,
// fare, travel time, or schedule (see RouteRecommendationUseCase).
public record RouteRecommendationResponse(
    RoutePointResponse origin,
    RoutePointResponse destination,
    double directDistanceKm,
    String distanceLabel,
    RouteCandidateResponse requestedVia,
    List<RouteCandidateResponse> suggestedViaCandidates) {}
