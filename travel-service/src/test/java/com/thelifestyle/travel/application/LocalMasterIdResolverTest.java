package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.travel.application.port.out.LocalMasterMirrorRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalMasterIdResolverTest {

    @Test void resolvesCityByMirroringCanonicalValueFromIntegrationUnderTheSameId() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var mirror = mock(LocalMasterMirrorRepository.class);
        var canonicalId = UUID.randomUUID();
        when(readPort.findCity("Pune")).thenReturn(Optional.of(
            new LifestyleMasterReadPort.CityMasterView(canonicalId, "Pune", "IN", 18.52, 73.85, "GeoNames", "2026-09-17T00:00:00Z")));
        when(mirror.upsertCity(canonicalId, "Pune", "IN", 18.52, 73.85)).thenReturn(canonicalId);

        var resolver = new LocalMasterIdResolver(readPort, mirror);
        var result = resolver.resolveCityId("Pune", "IN");

        assertEquals(Optional.of(canonicalId), result);
        verify(mirror, never()).findCityId(any(), any());
    }

    @Test void fallsBackToExistingLocalRowWhenIntegrationHasNoMatch() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var mirror = mock(LocalMasterMirrorRepository.class);
        var fallbackId = UUID.randomUUID();
        when(readPort.findCity("Nowhere")).thenReturn(Optional.empty());
        when(mirror.findCityId("Nowhere", "IN")).thenReturn(Optional.of(fallbackId));

        var resolver = new LocalMasterIdResolver(readPort, mirror);
        var result = resolver.resolveCityId("Nowhere", "IN");

        assertEquals(Optional.of(fallbackId), result);
    }

    @Test void resolvesCurrencyOnlyWhenIntegrationHasItAndUsesTheCanonicalId() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var mirror = mock(LocalMasterMirrorRepository.class);
        var canonicalId = UUID.randomUUID();
        when(readPort.findCurrency("USD")).thenReturn(Optional.of(
            new LifestyleMasterReadPort.CurrencyMasterView(canonicalId, "USD")));
        when(mirror.upsertCurrency(eq(canonicalId), eq("USD"), any())).thenReturn(canonicalId);

        var resolver = new LocalMasterIdResolver(readPort, mirror);
        var result = resolver.resolveCurrencyId("USD");

        assertEquals(Optional.of(canonicalId), result);
        verify(mirror, never()).findCurrencyId(any());
    }
}
