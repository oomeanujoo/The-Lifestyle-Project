package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.RoadRoute;
import com.thelifestyle.travel.domain.RoutePoint;

import java.util.List;

// A real road-routing provider (OSRM demo, §21 phase road-route) — called
// in origin, [via...], destination order. Throws RoadRouteProviderException
// on any genuine failure (timeout, no route, rate limit, invalid
// coordinates) rather than returning a value indistinguishable from
// success; isConfigured() is a plain config check, not a live call.
public interface RoadRouteProvider {
    boolean isConfigured();
    RoadRoute route(List<RoutePoint> waypoints);
}
