package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.RouteOptionUseCase;
import com.thelifestyle.travel.domain.RouteOption;
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
@RequestMapping("/api/travel/v1/trip-plans/{tripPlanId}/route-options")
public class RouteOptionController {
    private final RouteOptionUseCase useCase;

    public RouteOptionController(RouteOptionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID tripPlanId, @RequestBody RouteOptionRequest request) {
        return useCase.create(tripPlanId, request.label())
            .<ResponseEntity<?>>map(option -> ResponseEntity.ok(toResponse(option)))
            .orElseGet(() -> ResponseEntity.badRequest()
                .body(new ApiErrorResponse("label is required and tripPlanId '" + tripPlanId + "' must already exist")));
    }

    @GetMapping
    public List<RouteOptionResponse> findByTripPlan(@PathVariable UUID tripPlanId) {
        return useCase.findByTripPlan(tripPlanId).stream().map(this::toResponse).toList();
    }

    private RouteOptionResponse toResponse(RouteOption option) {
        return new RouteOptionResponse(option.id().toString(), option.tripPlanId().toString(), option.label(), option.createdAt());
    }
}
