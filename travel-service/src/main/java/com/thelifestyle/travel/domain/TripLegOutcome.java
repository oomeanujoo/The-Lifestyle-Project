package com.thelifestyle.travel.domain;

// Same result-or-error pattern as RouteRecommendationOutcome — a trip leg
// create can fail honestly (origin/destination/transport-mode name doesn't
// resolve against the refreshed masters) without that being an exception
// thrown through the use case for ordinary control flow.
public record TripLegOutcome(TripLeg leg, String error) {
    public static TripLegOutcome ok(TripLeg leg) {
        return new TripLegOutcome(leg, null);
    }

    public static TripLegOutcome error(String error) {
        return new TripLegOutcome(null, error);
    }
}
