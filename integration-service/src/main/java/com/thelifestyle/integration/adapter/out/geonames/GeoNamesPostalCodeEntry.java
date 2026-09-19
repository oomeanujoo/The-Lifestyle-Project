package com.thelifestyle.integration.adapter.out.geonames;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// GeoNames' postalCodeSearchJSON entry shape — only the fields this app
// actually uses. Deliberately flat: GeoNames has no concept matching this
// project's Indian real-estate area/locality hierarchy (lifestyle_master's
// municipality → area → locality → pincode chain) — it returns one
// `placeName` per postal code, plus admin-region names (district/state),
// nothing finer-grained. Never treat placeName/adminName2/adminName3 as a
// substitute for a verified locality/area/municipality — see
// MasterDataRefreshUseCase.refreshIndianPincodes() for how this is kept
// honest.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoNamesPostalCodeEntry(
    String postalCode, String placeName, String adminName1, String adminName2, String adminName3,
    Double lat, Double lng, String countryCode) {}
