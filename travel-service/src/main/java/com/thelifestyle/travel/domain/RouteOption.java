package com.thelifestyle.travel.domain;

import java.util.UUID;

// A row from travel.route_option — one alternative route (e.g. "via
// Mumbai") belonging to a trip_plan.
public record RouteOption(UUID id, UUID tripPlanId, String label, String createdAt) {}
