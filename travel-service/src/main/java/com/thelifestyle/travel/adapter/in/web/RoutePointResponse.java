package com.thelifestyle.travel.adapter.in.web;

// `id` is null for a via resolved from a pincode (a place inside a city,
// not a lifestyle_master.city row itself) — present for every city.
// `source`/`asOf` are this point's honest provenance and freshness.
public record RoutePointResponse(String id, String label, String countryCode, double latitude, double longitude, String source, String asOf) {}
