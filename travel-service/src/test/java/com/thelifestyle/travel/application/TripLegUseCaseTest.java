package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.RouteOptionRepository;
import com.thelifestyle.travel.application.port.out.TripLegRepository;
import com.thelifestyle.travel.domain.RouteOption;
import com.thelifestyle.travel.domain.TripLeg;
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

// The first real exercise of LocalMasterIdResolver by a use case that
// actually writes an FK — previously only LocalMasterIdResolverTest
// covered it in isolation.
class TripLegUseCaseTest {

    @Test void createsALegByResolvingNamesThroughTheMasterIdResolver() {
        var tripLegRepository = mock(TripLegRepository.class);
        var routeOptionRepository = mock(RouteOptionRepository.class);
        var masterIdResolver = mock(LocalMasterIdResolver.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var routeOptionId = UUID.randomUUID();
        var fromCityId = UUID.randomUUID();
        var toCityId = UUID.randomUUID();
        var transportModeId = UUID.randomUUID();
        var createdLeg = new TripLeg(UUID.randomUUID(), routeOptionId, fromCityId, toCityId, transportModeId, 1, "2026-09-17T00:00:00Z");

        when(routeOptionRepository.findById(routeOptionId)).thenReturn(Optional.of(
            new RouteOption(routeOptionId, UUID.randomUUID(), "via Mumbai", "2026-09-17T00:00:00Z")));
        when(masterIdResolver.resolveCityId("Pune", "IN")).thenReturn(Optional.of(fromCityId));
        when(masterIdResolver.resolveCityId("Mumbai", "IN")).thenReturn(Optional.of(toCityId));
        when(masterIdResolver.resolveTransportModeId("Flight")).thenReturn(Optional.of(transportModeId));
        when(tripLegRepository.create(routeOptionId, fromCityId, toCityId, transportModeId, 1)).thenReturn(createdLeg);

        var useCase = new TripLegUseCase(tripLegRepository, routeOptionRepository, masterIdResolver, auditLogRepository);
        var outcome = useCase.create(routeOptionId, "Pune", "Mumbai", "Flight", 1);

        assertNull(outcome.error());
        assertEquals(createdLeg, outcome.leg());
    }

    @Test void returnsAnHonestErrorInsteadOfAFabricatedFkWhenACityDoesNotResolve() {
        var tripLegRepository = mock(TripLegRepository.class);
        var routeOptionRepository = mock(RouteOptionRepository.class);
        var masterIdResolver = mock(LocalMasterIdResolver.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var routeOptionId = UUID.randomUUID();

        when(routeOptionRepository.findById(routeOptionId)).thenReturn(Optional.of(
            new RouteOption(routeOptionId, UUID.randomUUID(), "via Mumbai", "2026-09-17T00:00:00Z")));
        when(masterIdResolver.resolveCityId("Atlantis", "IN")).thenReturn(Optional.empty());

        var useCase = new TripLegUseCase(tripLegRepository, routeOptionRepository, masterIdResolver, auditLogRepository);
        var outcome = useCase.create(routeOptionId, "Atlantis", "Mumbai", "Flight", 1);

        assertTrue(outcome.error().contains("Atlantis"));
        verify(tripLegRepository, never()).create(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test void refusesToCreateALegUnderARouteOptionThatDoesNotExist() {
        var tripLegRepository = mock(TripLegRepository.class);
        var routeOptionRepository = mock(RouteOptionRepository.class);
        var masterIdResolver = mock(LocalMasterIdResolver.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var routeOptionId = UUID.randomUUID();
        when(routeOptionRepository.findById(routeOptionId)).thenReturn(Optional.empty());

        var useCase = new TripLegUseCase(tripLegRepository, routeOptionRepository, masterIdResolver, auditLogRepository);
        var outcome = useCase.create(routeOptionId, "Pune", "Mumbai", "Flight", 1);

        assertTrue(outcome.error().contains(routeOptionId.toString()));
        verify(masterIdResolver, never()).resolveCityId(any(), any());
    }
}
