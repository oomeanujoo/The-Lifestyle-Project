package com.thelifestyle.travel.domain;

// A real road-route result from a routing provider (OSRM demo, §21 phase
// road-route) — never a straight-line approximation. `geometry` is the
// provider's own encoded polyline, carried through only if the provider
// actually returned one (never fabricated when absent). `source`/
// `capturedAt` are honest provenance/freshness, same convention as
// RoutePoint. This is genuinely optional data: RouteRecommendationResult
// keeps its existing Haversine `directDistanceKm`/`distanceLabel`
// regardless of whether a road route was ever requested or succeeded.
public record RoadRoute(double distanceKm, double durationMinutes, String geometry, String source, String capturedAt) {}
