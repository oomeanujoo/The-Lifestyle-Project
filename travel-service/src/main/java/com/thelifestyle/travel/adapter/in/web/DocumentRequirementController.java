package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.DocumentRequirementUseCase;
import com.thelifestyle.travel.domain.DocumentRequirement;
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

@RestController
@RequestMapping("/api/travel/v1/trip-plans/{tripPlanId}/document-requirements")
public class DocumentRequirementController {
    private final DocumentRequirementUseCase useCase;

    public DocumentRequirementController(DocumentRequirementUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID tripPlanId, @RequestBody DocumentRequirementRequest request) {
        var outcome = useCase.create(tripPlanId, request.visaRequirementId(), request.title());
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.documentRequirement()));
    }

    @GetMapping
    public List<DocumentRequirementResponse> findByTripPlan(@PathVariable UUID tripPlanId) {
        return useCase.findByTripPlan(tripPlanId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable UUID tripPlanId, @PathVariable UUID id) {
        return useCase.findById(id)
            .<ResponseEntity<?>>map(requirement -> ResponseEntity.ok(toResponse(requirement)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/verified")
    public ResponseEntity<?> updateVerified(@PathVariable UUID tripPlanId, @PathVariable UUID id,
                                             @RequestBody DocumentRequirementVerifiedRequest request) {
        var outcome = useCase.updateVerified(id, request.verified());
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.documentRequirement()));
    }

    private DocumentRequirementResponse toResponse(DocumentRequirement requirement) {
        return new DocumentRequirementResponse(
            requirement.id().toString(),
            requirement.tripPlanId().toString(),
            requirement.visaRequirementId() == null ? null : requirement.visaRequirementId().toString(),
            requirement.title(),
            requirement.verified(),
            requirement.createdAt());
    }
}
