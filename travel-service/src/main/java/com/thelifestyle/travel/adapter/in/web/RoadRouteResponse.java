package com.thelifestyle.travel.adapter.in.web;

// `geometry` is the provider's own encoded polyline, present only when the
// provider actually returned one — never fabricated.
public record RoadRouteResponse(double distanceKm, double durationMinutes, String geometry, String source, String capturedAt) {}
