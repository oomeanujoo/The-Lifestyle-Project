package com.thelifestyle.travel.domain;

// A candidate via-point for a route, with its two straight-line legs and
// the total detour it adds versus the direct origin-to-destination
// distance. Ranking is by detourKm ascending (smallest detour = closest to
// "on the way") — see RouteRecommendationUseCase. All distances are the
// same approximate straight-line (Haversine) measure as the direct route,
// never a real road distance.
public record RouteCandidate(RoutePoint via, double originToViaKm, double viaToDestinationKm, double totalKm, double detourKm) {}
