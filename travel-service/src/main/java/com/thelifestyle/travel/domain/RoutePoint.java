package com.thelifestyle.travel.domain;

import java.util.UUID;

// A resolved endpoint or via-point for a route recommendation — either a
// lifestyle_master city (id present) or a place resolved from a staged
// Indian pincode (id absent — a pincode names a place inside a city, not a
// city master row itself). `source`/`asOf` are this point's honest
// provenance and freshness, carried straight through from
// LifestyleMasterReadPort so the caller can judge how current the
// coordinate is, never asserted as always-current.
public record RoutePoint(UUID id, String label, String countryCode, double latitude, double longitude, String source, String asOf) {}
