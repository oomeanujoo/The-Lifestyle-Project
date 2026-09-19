package com.thelifestyle.travel.domain;

public record RoadJourneyOutcome(RoadJourneyResult result, String error) {
    public static RoadJourneyOutcome ok(RoadJourneyResult result) {
        return new RoadJourneyOutcome(result, null);
    }

    public static RoadJourneyOutcome error(String error) {
        return new RoadJourneyOutcome(null, error);
    }
}
