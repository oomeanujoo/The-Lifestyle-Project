package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.RouteOption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteOptionRepository {
    RouteOption create(UUID tripPlanId, String label);
    Optional<RouteOption> findById(UUID id);
    List<RouteOption> findByTripPlan(UUID tripPlanId);
}
