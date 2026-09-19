package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.ItineraryDay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryDayRepository {
    ItineraryDay create(UUID tripPlanId, int dayNumber, String title, String detail);
    List<ItineraryDay> findByTripPlan(UUID tripPlanId);
    Optional<ItineraryDay> findByTripPlanAndDayNumber(UUID tripPlanId, int dayNumber);
}
