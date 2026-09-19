package com.thelifestyle.travel.adapter.in.web;

// fromCity/toCity/transportMode are names/codes, not ids — resolved
// against the refreshed lifestyle_master masters server-side
// (TripLegUseCase), the same DB-driven pattern as everywhere else in this
// service; never pass a raw id here.
public record TripLegRequest(String fromCity, String toCity, String transportMode, int legOrder) {}
