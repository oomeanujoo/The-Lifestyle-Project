package com.thelifestyle.travel.domain;

public record ItineraryDayOutcome(ItineraryDay day, String error) {
    public static ItineraryDayOutcome ok(ItineraryDay day) {
        return new ItineraryDayOutcome(day, null);
    }

    public static ItineraryDayOutcome error(String error) {
        return new ItineraryDayOutcome(null, error);
    }
}
