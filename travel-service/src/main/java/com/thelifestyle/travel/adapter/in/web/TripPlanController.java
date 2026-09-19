package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.TripPlanUseCase;
import com.thelifestyle.travel.domain.TripPlan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// The first real CRUD surface over travel's transaction schema (§16/§21
// phase M) — travel.trip_plan has existed since the schema was created,
// with zero application code reading or writing it until now.
@RestController
@RequestMapping("/api/travel/v1/trip-plans")
public class TripPlanController {
    private final TripPlanUseCase useCase;

    public TripPlanController(TripPlanUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TripPlanRequest request) {
        return useCase.create(request.title())
            .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(toResponse(plan)))
            .orElseGet(() -> ResponseEntity.badRequest().body(new ApiErrorResponse("title is required")));
    }

    @GetMapping
    public List<TripPlanResponse> findAll() {
        return useCase.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable UUID id) {
        return useCase.findById(id)
            .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(toResponse(plan)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody TripPlanStatusRequest request) {
        return useCase.updateStatus(id, request.status())
            .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(toResponse(plan)))
            .orElseGet(() -> ResponseEntity.badRequest()
                .body(new ApiErrorResponse("status must be one of DRAFT, ACTIVE, ARCHIVED, and the trip plan must exist")));
    }

    private TripPlanResponse toResponse(TripPlan plan) {
        return new TripPlanResponse(plan.id().toString(), plan.title(), plan.status(), plan.createdAt(), plan.updatedAt());
    }
}
