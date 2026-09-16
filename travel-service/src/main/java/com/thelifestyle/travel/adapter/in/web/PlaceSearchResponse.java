package com.thelifestyle.travel.adapter.in.web;

import java.util.List;

public record PlaceSearchResponse(List<PlaceMatchResponse> matches, String aiFallbackNotice) {}
