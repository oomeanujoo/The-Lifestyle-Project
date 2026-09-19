package com.thelifestyle.travel.domain;

public record RoadRouteOutcome(RoadRoute route, String error) {
    public static RoadRouteOutcome ok(RoadRoute route) {
        return new RoadRouteOutcome(route, null);
    }

    public static RoadRouteOutcome error(String error) {
        return new RoadRouteOutcome(null, error);
    }
}
