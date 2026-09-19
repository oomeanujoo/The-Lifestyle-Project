package com.thelifestyle.travel.domain;

import java.util.UUID;

// A row from travel.trip_leg — one ordered segment of a route_option.
// from_city_id/to_city_id/transport_mode_id are local ids that share
// identity with lifestyle_master (§17/§20) — resolved via
// LocalMasterIdResolver before this is ever created, never a raw name.
public record TripLeg(UUID id, UUID routeOptionId, UUID fromCityId, UUID toCityId, UUID transportModeId, int legOrder, String createdAt) {}
