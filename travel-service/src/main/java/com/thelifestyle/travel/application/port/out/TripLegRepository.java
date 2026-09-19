package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.TripLeg;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripLegRepository {
    TripLeg create(UUID routeOptionId, UUID fromCityId, UUID toCityId, UUID transportModeId, int legOrder);
    List<TripLeg> findByRouteOption(UUID routeOptionId);
    Optional<TripLeg> findById(UUID id);
}
