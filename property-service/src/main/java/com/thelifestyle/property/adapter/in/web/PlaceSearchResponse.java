package com.thelifestyle.property.adapter.in.web;

import java.util.List;

public record PlaceSearchResponse(List<PlaceMatchResponse> matches, String aiFallbackNotice) {}
