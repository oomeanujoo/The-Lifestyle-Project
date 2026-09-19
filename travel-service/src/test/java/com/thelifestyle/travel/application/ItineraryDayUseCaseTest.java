package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.ItineraryDayRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.ItineraryDay;
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

class ItineraryDayUseCaseTest {

    @Test void createsADayAndRecordsAnAuditEntry() {
        var dayRepository = mock(ItineraryDayRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        var dayId = UUID.randomUUID();
        var created = new ItineraryDay(dayId, tripPlanId, 1, "Arrival in Helsinki", "Land, check in, rest");

        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.of(
            new TripPlan(tripPlanId, "Finland trip", "DRAFT", "2026-09-19T00:00:00Z", "2026-09-19T00:00:00Z")));
        when(dayRepository.findByTripPlanAndDayNumber(tripPlanId, 1)).thenReturn(Optional.empty());
        when(dayRepository.create(tripPlanId, 1, "Arrival in Helsinki", "Land, check in, rest")).thenReturn(created);

        var useCase = new ItineraryDayUseCase(dayRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, 1, "Arrival in Helsinki", "Land, check in, rest");

        assertNull(outcome.error());
        assertEquals(created, outcome.day());
        verify(auditLogRepository).record("itinerary_day", dayId, "CREATE");
    }

    @Test void refusesToCreateADayForATripPlanThatDoesNotExist() {
        var dayRepository = mock(ItineraryDayRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.empty());

        var useCase = new ItineraryDayUseCase(dayRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, 1, "Arrival", "Detail");

        assertTrue(outcome.error().contains(tripPlanId.toString()));
        verify(dayRepository, never()).create(any(), any(Integer.class), any(), any());
    }

    @Test void refusesADayNumberOfZeroOrLess() {
        var dayRepository = mock(ItineraryDayRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.of(
            new TripPlan(tripPlanId, "Finland trip", "DRAFT", "2026-09-19T00:00:00Z", "2026-09-19T00:00:00Z")));

        var useCase = new ItineraryDayUseCase(dayRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, 0, "Arrival", "Detail");

        assertTrue(outcome.error().contains("dayNumber"));
        verify(dayRepository, never()).create(any(), any(Integer.class), any(), any());
    }

    @Test void refusesABlankTitle() {
        var dayRepository = mock(ItineraryDayRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.of(
            new TripPlan(tripPlanId, "Finland trip", "DRAFT", "2026-09-19T00:00:00Z", "2026-09-19T00:00:00Z")));

        var useCase = new ItineraryDayUseCase(dayRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, 1, "   ", "Detail");

        assertTrue(outcome.error().contains("title"));
        verify(dayRepository, never()).create(any(), any(Integer.class), any(), any());
    }

    @Test void refusesADuplicateDayNumberForTheSameTripPlan() {
        var dayRepository = mock(ItineraryDayRepository.class);
        var tripPlanRepository = mock(TripPlanRepository.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripPlanId = UUID.randomUUID();
        when(tripPlanRepository.findById(tripPlanId)).thenReturn(Optional.of(
            new TripPlan(tripPlanId, "Finland trip", "DRAFT", "2026-09-19T00:00:00Z", "2026-09-19T00:00:00Z")));
        when(dayRepository.findByTripPlanAndDayNumber(tripPlanId, 1)).thenReturn(Optional.of(
            new ItineraryDay(UUID.randomUUID(), tripPlanId, 1, "Existing day", null)));

        var useCase = new ItineraryDayUseCase(dayRepository, tripPlanRepository, auditLogRepository);
        var outcome = useCase.create(tripPlanId, 1, "Arrival", "Detail");

        assertTrue(outcome.error().contains("already exists"));
        verify(dayRepository, never()).create(any(), any(Integer.class), any(), any());
    }
}
