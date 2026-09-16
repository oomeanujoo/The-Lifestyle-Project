package com.thelifestyle.integration.adapter.out.geonames;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// GeoNames' searchJSON entry shape — only the fields this app actually
// uses; @JsonIgnoreProperties tolerates the many extra fields GeoNames
// returns that aren't modeled here.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoNamesEntry(long geonameId, String name, String countryName, String countryCode, String adminName1, String lat, String lng) {}
