package com.thelifestyle.travel.domain;

import java.util.UUID;

// A row from travel.itinerary_day — one numbered day within a trip plan,
// optional/free-text (§15.5's ItinerarySection, currently backed by
// static tripData.ts, is what this table is meant to eventually back).
public record ItineraryDay(UUID id, UUID tripPlanId, int dayNumber, String title, String detail) {}
