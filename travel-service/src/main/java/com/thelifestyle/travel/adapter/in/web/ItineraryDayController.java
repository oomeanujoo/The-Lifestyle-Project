package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.ItineraryDayUseCase;
import com.thelifestyle.travel.domain.ItineraryDay;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/travel/v1/trip-plans/{tripPlanId}/itinerary-days")
public class ItineraryDayController {
    private final ItineraryDayUseCase useCase;

    public ItineraryDayController(ItineraryDayUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID tripPlanId, @RequestBody ItineraryDayRequest request) {
        var outcome = useCase.create(tripPlanId, request.dayNumber(), request.title(), request.detail());
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.day()));
    }

    @GetMapping
    public List<ItineraryDayResponse> findByTripPlan(@PathVariable UUID tripPlanId) {
        return useCase.findByTripPlan(tripPlanId).stream().map(this::toResponse).toList();
    }

    private ItineraryDayResponse toResponse(ItineraryDay day) {
        return new ItineraryDayResponse(day.id().toString(), day.tripPlanId().toString(), day.dayNumber(), day.title(), day.detail());
    }
}
