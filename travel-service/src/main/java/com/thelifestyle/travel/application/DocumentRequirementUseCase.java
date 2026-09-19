package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.DocumentRequirementRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.DocumentRequirement;
import com.thelifestyle.travel.domain.DocumentRequirementOutcome;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// travel.visa_requirement is manually curated with no live source (§19),
// so visaRequirementId is accepted here as a raw, unvalidated optional UUID
// rather than resolved through a live master-data port — documented, not
// silently skipped.
@Service
public class DocumentRequirementUseCase {
    private static final String ENTITY_TYPE = "document_requirement";

    private final DocumentRequirementRepository documentRequirementRepository;
    private final TripPlanRepository tripPlanRepository;
    private final AuditLogRepository auditLogRepository;

    public DocumentRequirementUseCase(DocumentRequirementRepository documentRequirementRepository,
                                       TripPlanRepository tripPlanRepository,
                                       AuditLogRepository auditLogRepository) {
        this.documentRequirementRepository = documentRequirementRepository;
        this.tripPlanRepository = tripPlanRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public DocumentRequirementOutcome create(UUID tripPlanId, UUID visaRequirementId, String title) {
        if (tripPlanRepository.findById(tripPlanId).isEmpty()) {
            return DocumentRequirementOutcome.error("trip plan '" + tripPlanId + "' does not exist");
        }
        if (!StringUtils.hasText(title)) {
            return DocumentRequirementOutcome.error("title is required");
        }

        var requirement = documentRequirementRepository.create(tripPlanId, visaRequirementId, title.trim());
        auditLogRepository.record(ENTITY_TYPE, requirement.id(), "CREATE");
        return DocumentRequirementOutcome.ok(requirement);
    }

    public List<DocumentRequirement> findByTripPlan(UUID tripPlanId) {
        return documentRequirementRepository.findByTripPlan(tripPlanId);
    }

    public Optional<DocumentRequirement> findById(UUID id) {
        return documentRequirementRepository.findById(id);
    }

    public DocumentRequirementOutcome updateVerified(UUID id, boolean verified) {
        return documentRequirementRepository.updateVerified(id, verified)
            .map(requirement -> {
                auditLogRepository.record(ENTITY_TYPE, requirement.id(), "UPDATE");
                return DocumentRequirementOutcome.ok(requirement);
            })
            .orElseGet(() -> DocumentRequirementOutcome.error("document requirement '" + id + "' does not exist"));
    }
}
