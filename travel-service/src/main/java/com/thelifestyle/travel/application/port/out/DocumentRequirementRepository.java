package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.DocumentRequirement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRequirementRepository {
    DocumentRequirement create(UUID tripPlanId, UUID visaRequirementId, String title);
    List<DocumentRequirement> findByTripPlan(UUID tripPlanId);
    Optional<DocumentRequirement> findById(UUID id);
    Optional<DocumentRequirement> updateVerified(UUID id, boolean verified);
}
