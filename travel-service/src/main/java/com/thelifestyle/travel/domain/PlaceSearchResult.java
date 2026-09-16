package com.thelifestyle.travel.domain;

import java.util.List;

// aiFallbackNotice is non-null only when matches is empty — see
// TECHNICAL_ARCHITECTURE.md §18 "Smart search / autocomplete". No AI call
// happens here yet; this is the same honest placeholder the frontend uses.
public record PlaceSearchResult(List<Place> matches, String aiFallbackNotice) {}
