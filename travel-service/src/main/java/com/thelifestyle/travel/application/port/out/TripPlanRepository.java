package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.TripPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripPlanRepository {
    TripPlan create(String title);
    Optional<TripPlan> findById(UUID id);
    List<TripPlan> findAll();
    Optional<TripPlan> updateStatus(UUID id, String status);
}
