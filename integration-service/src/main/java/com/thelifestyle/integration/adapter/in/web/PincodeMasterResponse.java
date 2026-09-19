package com.thelifestyle.integration.adapter.in.web;

// A GeoNames postal-code result (lifestyle_master.city_pincode — see
// CityPincode's comment for why not lifestyle_master.pincode itself).
// `source`/`capturedAt` are its honest provenance and freshness, same as
// CityMasterResponse. `cityId` ties it back to the lifestyle_master.city
// row this service resolved it against.
public record PincodeMasterResponse(
    String cityId, String cityName, String countryCode,
    String pincode, String placeName, String adminName2, String adminName3,
    Double latitude, Double longitude, String source, String capturedAt) {}
