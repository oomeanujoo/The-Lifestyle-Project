package com.thelifestyle.integration.domain;

import java.util.UUID;

// A GeoNames postal-code result, tied to a lifestyle_master.city row by
// `cityId` — deliberately NOT lifestyle_master.pincode. That table's
// locality_id is NOT NULL and requires a real locality -> area ->
// municipality -> city chain to already exist; GeoNames' postal-code API
// has no concept matching that Indian real-estate area/locality hierarchy
// (it returns one flat placeName + admin-region names per postal code), so
// honestly populating lifestyle_master.pincode from this data would mean
// inventing a locality/area/municipality row just to satisfy the FK —
// exactly what this refresh must not do. This record models a row in
// lifestyle_master.city_pincode, a same-shaped sibling table with a real
// FK to lifestyle_master.city(id) and no locality requirement — Codex's
// replacement for the original integration.city_pincode staging table
// (left in place, unwritten, until Codex migrates and removes it).
// `cityName`/`countryCode` are read back via a join, not stored columns.
public record CityPincode(
    UUID id, UUID cityId, String cityName, String countryCode,
    String pincode, String placeName, String adminName2, String adminName3,
    Double latitude, Double longitude, String source, String capturedAt) {}
