package com.thelifestyle.property.adapter.in.web;

import com.thelifestyle.property.application.PropertyPlanUseCase;
import com.thelifestyle.property.domain.PropertyPlan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// The first real CRUD surface over property's transaction schema —
// property.property_plan has existed since the schema was created, with
// zero application code reading or writing it until now. Mirrors
// travel-service's TripPlanController exactly.
@RestController
@RequestMapping("/api/property/v1/property-plans")
public class PropertyPlanController {
    private final PropertyPlanUseCase useCase;

    public PropertyPlanController(PropertyPlanUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PropertyPlanRequest request) {
        return useCase.create(request.title())
            .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(toResponse(plan)))
            .orElseGet(() -> ResponseEntity.badRequest().body(new ApiErrorResponse("title is required")));
    }

    @GetMapping
    public List<PropertyPlanResponse> findAll() {
        return useCase.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable UUID id) {
        return useCase.findById(id)
            .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(toResponse(plan)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody PropertyPlanStatusRequest request) {
        return useCase.updateStatus(id, request.status())
            .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(toResponse(plan)))
            .orElseGet(() -> ResponseEntity.badRequest()
                .body(new ApiErrorResponse("status must be one of DRAFT, ACTIVE, ARCHIVED, and the property plan must exist")));
    }

    private PropertyPlanResponse toResponse(PropertyPlan plan) {
        return new PropertyPlanResponse(plan.id().toString(), plan.title(), plan.status(), plan.createdAt(), plan.updatedAt());
    }
}
