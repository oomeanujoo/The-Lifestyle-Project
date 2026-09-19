package com.thelifestyle.travel.domain;

// Same pattern as integration-service's MasterRefreshOutcome — a plain
// result-or-error record instead of exceptions as control flow, so the
// controller can map "origin/destination/via not found" to a 400 without
// a try/catch around business logic.
public record RouteRecommendationOutcome(RouteRecommendationResult result, String error) {
    public static RouteRecommendationOutcome ok(RouteRecommendationResult result) {
        return new RouteRecommendationOutcome(result, null);
    }

    public static RouteRecommendationOutcome error(String error) {
        return new RouteRecommendationOutcome(null, error);
    }
}
