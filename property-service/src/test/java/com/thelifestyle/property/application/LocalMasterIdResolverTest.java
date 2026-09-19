package com.thelifestyle.property.application;

import com.thelifestyle.property.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.property.application.port.out.LocalMasterMirrorRepository;
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

    @Test void resolvesBhkTypeByMirroringCanonicalValueFromIntegrationUnderTheSameId() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var mirror = mock(LocalMasterMirrorRepository.class);
        var canonicalId = UUID.randomUUID();
        when(readPort.findBhkType("2BHK")).thenReturn(Optional.of(
            new LifestyleMasterReadPort.CodedMasterView(canonicalId, "2BHK", "2 BHK")));
        when(mirror.upsertBhkType(canonicalId, "2BHK", "2 BHK")).thenReturn(canonicalId);

        var resolver = new LocalMasterIdResolver(readPort, mirror);
        var result = resolver.resolveBhkTypeId("2BHK");

        assertEquals(Optional.of(canonicalId), result);
        verify(mirror, never()).findBhkTypeId(any());
    }

    @Test void fallsBackToExistingLocalRowWhenIntegrationHasNoMatch() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var mirror = mock(LocalMasterMirrorRepository.class);
        var fallbackId = UUID.randomUUID();
        when(readPort.findServiceAddon("laundry")).thenReturn(Optional.empty());
        when(mirror.findServiceAddonId("laundry")).thenReturn(Optional.of(fallbackId));

        var resolver = new LocalMasterIdResolver(readPort, mirror);
        var result = resolver.resolveServiceAddonId("laundry");

        assertEquals(Optional.of(fallbackId), result);
    }

    @Test void resolvesCurrencyOnlyWhenIntegrationHasItAndUsesTheCanonicalId() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var mirror = mock(LocalMasterMirrorRepository.class);
        var canonicalId = UUID.randomUUID();
        when(readPort.findCurrency("EUR")).thenReturn(Optional.of(
            new LifestyleMasterReadPort.CurrencyMasterView(canonicalId, "EUR")));
        when(mirror.upsertCurrency(eq(canonicalId), eq("EUR"), any())).thenReturn(canonicalId);

        var resolver = new LocalMasterIdResolver(readPort, mirror);
        var result = resolver.resolveCurrencyId("EUR");

        assertEquals(Optional.of(canonicalId), result);
        verify(mirror, never()).findCurrencyId(any());
    }
}
