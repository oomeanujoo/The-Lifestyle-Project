package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.RouteOptionRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.RouteOption;
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

class RouteOptionUseCaseTest {

    @Test void createsARouteOptionWhenTheParentTripPlanExists() {
        var routeOptionRepository = mock(RouteOptionRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        var created = new RouteOption(UUID.randomUUID(), tripPlanId, "via Mumbai", "2026-09-17T00:00:00Z");
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.of(
            new TripPlan(tripPlanId, "Kerala trip", "DRAFT", "2026-09-17T00:00:00Z", "2026-09-17T00:00:00Z")));
        when(routeOptionRepository.create(tripPlanId, "via Mumbai")).thenReturn(created);

        var useCase = new RouteOptionUseCase(routeOptionRepository, tripPlanRepository, auditLogRepository);

        assertEquals(Optional.of(created), useCase.create(tripPlanId, "via Mumbai"));
    }

    @Test void refusesToCreateADanglingRouteOptionForAMissingTripPlan() {
        var routeOptionRepository = mock(RouteOptionRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.empty());

        var useCase = new RouteOptionUseCase(routeOptionRepository, tripPlanRepository, auditLogRepository);

        assertTrue(useCase.create(tripPlanId, "via Mumbai").isEmpty());
        verify(routeOptionRepository, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
