package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.DocumentRequirementRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.DocumentRequirement;
import com.thelifestyle.travel.domain.TripPlan;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentRequirementUseCaseTest {

    @Test void createsARequirementAndRecordsAnAuditEntry() {
        var requirementRepository = mock(DocumentRequirementRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        var requirementId = UUID.randomUUID();
        var created = new DocumentRequirement(requirementId, tripPlanId, null, "Visa application", false, "2026-09-19T00:00:00Z");

        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.of(
            new TripPlan(tripPlanId, "Finland trip", "DRAFT", "2026-09-19T00:00:00Z", "2026-09-19T00:00:00Z")));
        when(requirementRepository.create(tripPlanId, null, "Visa application")).thenReturn(created);

        var useCase = new DocumentRequirementUseCase(requirementRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, null, "Visa application");

        assertNull(outcome.error());
        assertEquals(created, outcome.documentRequirement());
        verify(auditLogRepository).record("document_requirement", requirementId, "CREATE");
    }

    @Test void refusesToCreateARequirementForATripPlanThatDoesNotExist() {
        var requirementRepository = mock(DocumentRequirementRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.empty());

        var useCase = new DocumentRequirementUseCase(requirementRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, null, "Visa application");

        assertTrue(outcome.error().contains(tripPlanId.toString()));
        verify(requirementRepository, never()).create(any(), any(), any());
    }

    @Test void refusesABlankTitle() {
        var requirementRepository = mock(DocumentRequirementRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.of(
            new TripPlan(tripPlanId, "Finland trip", "DRAFT", "2026-09-19T00:00:00Z", "2026-09-19T00:00:00Z")));

        var useCase = new DocumentRequirementUseCase(requirementRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, null, "   ");

        assertTrue(outcome.error().contains("title"));
        verify(requirementRepository, never()).create(any(), any(), any());
    }

    @Test void updatesVerifiedAndRecordsAnAuditEntry() {
        var requirementRepository = mock(DocumentRequirementRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var requirementId = UUID.randomUUID();
        var tripPlanId = UUID.randomUUID();
        var updated = new DocumentRequirement(requirementId, tripPlanId, null, "Visa application", true, "2026-09-19T00:00:00Z");
        when(requirementRepository.updateVerified(requirementId, true)).thenReturn(Optional.of(updated));

        var useCase = new DocumentRequirementUseCase(requirementRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.updateVerified(requirementId, true);

        assertNull(outcome.error());
        assertEquals(updated, outcome.documentRequirement());
        verify(auditLogRepository).record("document_requirement", requirementId, "UPDATE");
    }

    @Test void returnsAnHonestErrorWhenUpdatingARequirementThatDoesNotExist() {
        var requirementRepository = mock(DocumentRequirementRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var requirementId = UUID.randomUUID();
        when(requirementRepository.updateVerified(requirementId, true)).thenReturn(Optional.empty());

        var useCase = new DocumentRequirementUseCase(requirementRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.updateVerified(requirementId, true);

        assertTrue(outcome.error().contains(requirementId.toString()));
    }
}
