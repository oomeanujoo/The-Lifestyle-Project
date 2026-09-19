package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesEntry;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesPostalCodeEntry;
import com.thelifestyle.integration.application.port.out.CityPincodeRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterRepository;
import com.thelifestyle.integration.application.port.out.LifestyleMasterCityRepository;
import com.thelifestyle.integration.application.port.out.LifestyleMasterCurrencyRepository;
import com.thelifestyle.integration.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.integration.config.MasterRefreshProperties;
import com.thelifestyle.integration.domain.CityMaster;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pins the exact behavior the task requires: a GeoNames postal-code result
// missing a postal code or place name is skipped, never persisted with an
// invented value, and a non-Indian seed city (e.g. Dubai) never even
// triggers a postal-code search — this refresh is India-only by design,
// not by accident.
class MasterDataRefreshUseCaseIndiaPincodeTest {

    @Test void skipsUntrustworthyPostalMatchesAndNeverQueriesNonIndianCities() {
        var geoNamesClient = mock(GeoNamesClient.class);
        var frankfurterClient = mock(FrankfurterClient.class);
        var cityRepository = mock(LifestyleMasterCityRepository.class);
        var currencyRepository = mock(LifestyleMasterCurrencyRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var cityPincodeRepository = mock(CityPincodeRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);
        var masters = new MasterRefreshProperties(List.of("Pune", "Dubai"), "INR", 100, 100, 5, 20);

        when(geoNamesClient.isConfigured()).thenReturn(true);

        var puneId = UUID.randomUUID();
        var dubaiId = UUID.randomUUID();
        when(geoNamesClient.search("Pune", 5)).thenReturn(
            List.of(new GeoNamesEntry(1L, "Pune", "India", "IN", "Maharashtra", "18.52", "73.85")));
        when(geoNamesClient.search("Dubai", 5)).thenReturn(
            List.of(new GeoNamesEntry(2L, "Dubai", "United Arab Emirates", "AE", "Dubai", "25.20", "55.27")));
        when(cityRepository.upsert(eq("Pune"), eq("IN"), any(), any(), any()))
            .thenReturn(new CityMaster(puneId, "Pune", "IN", 18.52, 73.85, "GeoNames", "2026-09-17T00:00:00Z"));
        when(cityRepository.upsert(eq("Dubai"), eq("AE"), any(), any(), any()))
            .thenReturn(new CityMaster(dubaiId, "Dubai", "AE", 25.20, 55.27, "GeoNames", "2026-09-17T00:00:00Z"));
        when(geoNamesClient.searchByCountry(eq("IN"), anyInt(), anyInt())).thenReturn(List.of());
        when(cityRepository.findAll()).thenReturn(List.of(
            new CityMaster(puneId, "Pune", "IN", 18.52, 73.85, "GeoNames", "2026-09-17T00:00:00Z"),
            new CityMaster(dubaiId, "Dubai", "AE", 25.20, 55.27, "GeoNames", "2026-09-17T00:00:00Z")));

        when(geoNamesClient.searchPostalCodes("Pune", "IN", 20)).thenReturn(List.of(
            new GeoNamesPostalCodeEntry("411001", "Pune", "Maharashtra", "Pune", null, 18.52, 73.85, "IN"),
            new GeoNamesPostalCodeEntry(null, "Pune", "Maharashtra", "Pune", null, 18.53, 73.86, "IN"),
            new GeoNamesPostalCodeEntry("411002", "", "Maharashtra", "Pune", null, 18.54, 73.87, "IN")));

        var useCase = new MasterDataRefreshUseCase(geoNamesClient, frankfurterClient, cityRepository,
            currencyRepository, codedMasterRepository, cityPincodeRepository, masterRefreshLogRepository, masters);

        var outcomes = useCase.refreshIndiaCityAndPincodes();

        var pincodeOutcome = outcomes.get(1);
        assertEquals("india_pincode", pincodeOutcome.masterName());
        assertEquals("PARTIAL", pincodeOutcome.status());
        assertEquals(1, pincodeOutcome.recordsUpserted());
        assertTrue(pincodeOutcome.errorMessage().contains("2 GeoNames postal-code result(s) skipped"));

        verify(cityPincodeRepository, times(1)).upsert(eq(puneId),
            eq("411001"), eq("Pune"), any(), any(), any(), any(), eq("GeoNames"));
        verify(geoNamesClient, never()).searchPostalCodes(eq("Dubai"), anyString(), anyInt());
    }
}
