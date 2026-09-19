package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.geonames.GeoNamesClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesEntry;
import com.thelifestyle.integration.application.port.out.LifestyleMasterCityRepository;
import com.thelifestyle.integration.domain.CityMaster;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CityAcquisitionUseCaseTest {
    private final GeoNamesClient geonames = mock(GeoNamesClient.class);
    private final LifestyleMasterCityRepository cities = mock(LifestyleMasterCityRepository.class);
    private final CityAcquisitionUseCase useCase = new CityAcquisitionUseCase(geonames, cities);

    @Test void existingCityNeedsNoProviderCallOrWrite() {
        var existing = city("Pune");
        when(cities.findByName("Pune")).thenReturn(List.of(existing));
        assertEquals(existing, useCase.acquire(" Pune ", "in").orElseThrow());
        verifyNoInteractions(geonames);
        verify(cities, never()).upsert(anyString(), anyString(), any(), any(), anyString());
    }

    @Test void onlyExactVerifiedPopulatedPlaceWithCoordinatesIsSaved() {
        when(cities.findByName("Gwalior")).thenReturn(List.of());
        when(geonames.searchPopulatedPlaces("Gwalior", "IN", 20)).thenReturn(List.of(
            entry(1, "Gwalior North", "IN", "26", "78"),
            entry(2, "Gwalior", "IN", "NaN", "78"),
            entry(3, "Gwalior", "IN", "26.21", "78.17")));
        var saved = city("Gwalior");
        when(cities.upsert("Gwalior", "IN", 26.21, 78.17, "GeoNames")).thenReturn(saved);
        assertEquals(saved, useCase.acquire("Gwalior", "IN").orElseThrow());
        verify(cities).upsert("Gwalior", "IN", 26.21, 78.17, "GeoNames");
    }

    @Test void noTrustworthyMatchLeavesMasterUntouched() {
        when(geonames.searchPopulatedPlaces("Gwalior", "IN", 20)).thenReturn(List.of(
            entry(0, "Gwalior", "IN", "26", "78"),
            entry(1, "Gwalior", "AE", "26", "78")));
        assertTrue(useCase.acquire("Gwalior", "IN").isEmpty());
        verify(cities, never()).upsert(anyString(), anyString(), any(), any(), anyString());
    }

    private static GeoNamesEntry entry(long id, String name, String country, String lat, String lng) {
        return new GeoNamesEntry(id, name, country, country, "State", lat, lng);
    }

    private static CityMaster city(String name) {
        return new CityMaster(UUID.randomUUID(), name, "IN", 26.21, 78.17, "GeoNames", "2026-09-19T00:00:00Z");
    }
}
