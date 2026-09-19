package com.thelifestyle.property.application;

import com.thelifestyle.property.application.port.out.PropertyPlanRepository;
import com.thelifestyle.property.domain.PropertyPlan;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyPlanUseCaseTest {

    @Test void createsAPlanWithATrimmedTitle() {
        var repository = mock(PropertyPlanRepository.class);
        var created = new PropertyPlan(UUID.randomUUID(), "Pune move", "DRAFT", "2026-09-19T00:00:00Z", "2026-09-19T00:00:00Z");
        when(repository.create("Pune move")).thenReturn(created);

        var useCase = new PropertyPlanUseCase(repository);
        var result = useCase.create("  Pune move  ");

        assertEquals(Optional.of(created), result);
    }

    @Test void rejectsABlankTitleWithoutTouchingTheRepository() {
        var repository = mock(PropertyPlanRepository.class);
        var useCase = new PropertyPlanUseCase(repository);

        var result = useCase.create("   ");

        assertTrue(result.isEmpty());
        verify(repository, never()).create(any());
    }

    @Test void updatesStatusOnlyToAValueTheDbsCheckConstraintAllows() {
        var repository = mock(PropertyPlanRepository.class);
        var id = UUID.randomUUID();
        var updated = new PropertyPlan(id, "Pune move", "ACTIVE", "2026-09-19T00:00:00Z", "2026-09-19T00:01:00Z");
        when(repository.updateStatus(id, "ACTIVE")).thenReturn(Optional.of(updated));

        var useCase = new PropertyPlanUseCase(repository);

        assertEquals(Optional.of(updated), useCase.updateStatus(id, "active"));
        assertTrue(useCase.updateStatus(id, "CANCELLED").isEmpty());
        verify(repository, never()).updateStatus(id, "CANCELLED");
    }
}
