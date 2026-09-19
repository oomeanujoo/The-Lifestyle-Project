package com.thelifestyle.integration.adapter.in.web;

// `id` is the stable lifestyle_master.city UUID (as a string) — the value
// Travel/Property must persist as the FK target for any city reference,
// never a locally-generated one. `source`/`updatedAt` are this row's
// honest provenance and freshness — the answer a consumer (e.g. Travel's
// route-recommendation endpoint) needs before treating latitude/longitude
// as current data rather than a stale or fabricated value.
public record CityMasterResponse(String id, String name, String countryCode, Double latitude, Double longitude, String source, String updatedAt) {}
