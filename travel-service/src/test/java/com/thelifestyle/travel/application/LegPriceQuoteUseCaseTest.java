package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.LegPriceQuoteRepository;
import com.thelifestyle.travel.application.port.out.TripLegRepository;
import com.thelifestyle.travel.domain.LegPriceQuote;
import com.thelifestyle.travel.domain.TripLeg;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegPriceQuoteUseCaseTest {

    @Test void createsAQuoteResolvingCurrencyByNameAndDefaultingConfidenceToUnverified() {
        var quoteRepository = mock(LegPriceQuoteRepository.class);
        var tripLegRepository = mock(TripLegRepository.class);
        var masterIdResolver = mock(LocalMasterIdResolver.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripLegId = UUID.randomUUID();
        var currencyId = UUID.randomUUID();
        var createdQuote = new LegPriceQuote(UUID.randomUUID(), tripLegId, currencyId, 4200.0, 3.5,
            "IndiGo website", null, "UNVERIFIED", "2026-09-19T00:00:00Z");

        when(tripLegRepository.findById(tripLegId)).thenReturn(Optional.of(
            new TripLeg(tripLegId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "2026-09-19T00:00:00Z")));
        when(masterIdResolver.resolveCurrencyId("INR")).thenReturn(Optional.of(currencyId));
        when(quoteRepository.create(tripLegId, currencyId, 4200.0, 3.5, "IndiGo website", null, "UNVERIFIED"))
            .thenReturn(createdQuote);

        var useCase = new LegPriceQuoteUseCase(quoteRepository, tripLegRepository, masterIdResolver, auditLogRepository);
        var outcome = useCase.create(tripLegId, "INR", 4200.0, 3.5, "IndiGo website", null, null);

        assertNull(outcome.error());
        assertEquals(createdQuote, outcome.quote());
    }

    @Test void refusesANegativeAmountWithoutTouchingTheRepository() {
        var quoteRepository = mock(LegPriceQuoteRepository.class);
        var tripLegRepository = mock(TripLegRepository.class);
        var masterIdResolver = mock(LocalMasterIdResolver.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripLegId = UUID.randomUUID();
        when(tripLegRepository.findById(tripLegId)).thenReturn(Optional.of(
            new TripLeg(tripLegId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "2026-09-19T00:00:00Z")));

        var useCase = new LegPriceQuoteUseCase(quoteRepository, tripLegRepository, masterIdResolver, auditLogRepository);
        var outcome = useCase.create(tripLegId, "INR", -1.0, null, "Some source", null, null);

        assertTrue(outcome.error().contains("amount"));
        verify(quoteRepository, never()).create(any(), any(), any(Double.class), any(), any(), any(), any());
    }

    @Test void returnsAnHonestErrorWhenCurrencyDoesNotResolve() {
        var quoteRepository = mock(LegPriceQuoteRepository.class);
        var tripLegRepository = mock(TripLegRepository.class);
        var masterIdResolver = mock(LocalMasterIdResolver.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripLegId = UUID.randomUUID();
        when(tripLegRepository.findById(tripLegId)).thenReturn(Optional.of(
            new TripLeg(tripLegId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "2026-09-19T00:00:00Z")));
        when(masterIdResolver.resolveCurrencyId("ZZZ")).thenReturn(Optional.empty());

        var useCase = new LegPriceQuoteUseCase(quoteRepository, tripLegRepository, masterIdResolver, auditLogRepository);
        var outcome = useCase.create(tripLegId, "ZZZ", 100.0, null, "Some source", null, null);

        assertTrue(outcome.error().contains("ZZZ"));
    }

    @Test void refusesToCreateAQuoteForATripLegThatDoesNotExist() {
        var quoteRepository = mock(LegPriceQuoteRepository.class);
        var tripLegRepository = mock(TripLegRepository.class);
        var masterIdResolver = mock(LocalMasterIdResolver.class);
        var auditLogRepository = mock(AuditLogRepository.class);
        var tripLegId = UUID.randomUUID();
        when(tripLegRepository.findById(tripLegId)).thenReturn(Optional.empty());

        var useCase = new LegPriceQuoteUseCase(quoteRepository, tripLegRepository, masterIdResolver, auditLogRepository);
        var outcome = useCase.create(tripLegId, "INR", 100.0, null, "Some source", null, null);

        assertTrue(outcome.error().contains(tripLegId.toString()));
        verify(masterIdResolver, never()).resolveCurrencyId(any());
    }
}
