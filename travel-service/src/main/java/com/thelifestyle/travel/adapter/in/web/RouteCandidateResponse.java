package com.thelifestyle.travel.adapter.in.web;

// All *Km fields are the same approximate straight-line (Haversine) measure
// as the top-level directDistanceKm — see RouteRecommendationResponse.
public record RouteCandidateResponse(RoutePointResponse via, double originToViaKm, double viaToDestinationKm, double totalKm, double detourKm) {}
