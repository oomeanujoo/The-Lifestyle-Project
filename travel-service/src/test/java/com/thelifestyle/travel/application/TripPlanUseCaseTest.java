package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.TripPlan;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripPlanUseCaseTest {

    @Test void createsAPlanWithATrimmedTitle() {
        var repository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var created = new TripPlan(UUID.randomUUID(), "Kerala trip", "DRAFT", "2026-09-17T00:00:00Z", "2026-09-17T00:00:00Z");
        when(repository.create("Kerala trip")).thenReturn(created);

        var useCase = new TripPlanUseCase(repository, auditLogRepository);
        var result = useCase.create("  Kerala trip  ");

        assertEquals(Optional.of(created), result);
    }

    @Test void rejectsABlankTitleWithoutTouchingTheRepository() {
        var repository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var useCase = new TripPlanUseCase(repository, auditLogRepository);

        var result = useCase.create("   ");

        assertTrue(result.isEmpty());
        verify(repository, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test void updatesStatusOnlyToAValueTheDbsCheckConstraintAllows() {
        var repository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var id = UUID.randomUUID();
        var updated = new TripPlan(id, "Kerala trip", "ACTIVE", "2026-09-17T00:00:00Z", "2026-09-17T00:01:00Z");
        when(repository.updateStatus(id, "ACTIVE")).thenReturn(Optional.of(updated));

        var useCase = new TripPlanUseCase(repository, auditLogRepository);

        assertEquals(Optional.of(updated), useCase.updateStatus(id, "active"));
        assertTrue(useCase.updateStatus(id, "CANCELLED").isEmpty());
        verify(repository, never()).updateStatus(id, "CANCELLED");
    }
}
