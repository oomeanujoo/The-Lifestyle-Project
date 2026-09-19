package com.thelifestyle.travel.domain;

import java.util.List;

// The full answer to a route-recommendation request: the direct
// origin-destination distance, the specific via the caller asked about (if
// any), and a ranked list of other known cities that add the least detour.
// `distanceLabel` is deliberately part of the payload, not just a comment
// here — every distance in this result is an approximate straight-line
// (great-circle) measure computed locally; this is never a real road
// route, and no fare, travel time, or schedule is invented or included.
public record RouteRecommendationResult(
    RoutePoint origin,
    RoutePoint destination,
    double directDistanceKm,
    String distanceLabel,
    RouteCandidate requestedVia,
    List<RouteCandidate> suggestedViaCandidates) {}
