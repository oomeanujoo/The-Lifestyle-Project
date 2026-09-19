package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesEntry;
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
import java.util.stream.IntStream;

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

// Pins the "at least 100 distinct, sourced Indian cities" requirement: a
// bulk, paged GeoNames discovery pass (searchByCountry), never an invented
// row, growing coverage past a small hand-picked seed list.
class MasterDataRefreshUseCaseCityDiscoveryTest {

    private static GeoNamesEntry entry(int n) {
        return new GeoNamesEntry(n, "City" + n, "India", "IN", "State", "20.0", "77.0");
    }

    @Test void discoversCitiesAcrossMultiplePagesUntilTheTargetIsReached() {
        var geoNamesClient = mock(GeoNamesClient.class);
        var frankfurterClient = mock(FrankfurterClient.class);
        var cityRepository = mock(LifestyleMasterCityRepository.class);
        var currencyRepository = mock(LifestyleMasterCurrencyRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var cityPincodeRepository = mock(CityPincodeRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);
        // No seed cities — isolates this test to the bulk-discovery path.
        // Target 25 cities, page size 10 — expects 3 pages (10 + 10 + 5).
        var masters = new MasterRefreshProperties(List.of(), "INR", 25, 10, 5, 20);

        when(geoNamesClient.isConfigured()).thenReturn(true);
        var page1 = IntStream.range(1, 11).mapToObj(MasterDataRefreshUseCaseCityDiscoveryTest::entry).toList();
        var page2 = IntStream.range(11, 21).mapToObj(MasterDataRefreshUseCaseCityDiscoveryTest::entry).toList();
        var page3 = IntStream.range(21, 26).mapToObj(MasterDataRefreshUseCaseCityDiscoveryTest::entry).toList();
        when(geoNamesClient.searchByCountry("IN", 10, 0)).thenReturn(page1);
        when(geoNamesClient.searchByCountry("IN", 10, 10)).thenReturn(page2);
        when(geoNamesClient.searchByCountry("IN", 10, 20)).thenReturn(page3);
        when(cityRepository.upsert(anyString(), eq("IN"), any(), any(), any()))
            .thenReturn(new CityMaster(UUID.randomUUID(), "City", "IN", 20.0, 77.0, "GeoNames", "2026-09-19T00:00:00Z"));
        when(cityRepository.findAll()).thenReturn(List.of());
        when(frankfurterClient.latest("INR"))
            .thenReturn(new com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterRates("INR", "2026-09-19", java.util.Map.of()));

        var useCase = new MasterDataRefreshUseCase(geoNamesClient, frankfurterClient, cityRepository,
            currencyRepository, codedMasterRepository, cityPincodeRepository, masterRefreshLogRepository, masters);
        var outcomes = useCase.refreshAll();

        var cityOutcome = outcomes.get(0);
        assertEquals("SUCCESS", cityOutcome.status());
        assertEquals(25, cityOutcome.recordsUpserted());
        verify(geoNamesClient, times(1)).searchByCountry("IN", 10, 0);
        verify(geoNamesClient, times(1)).searchByCountry("IN", 10, 10);
        verify(geoNamesClient, times(1)).searchByCountry("IN", 10, 20);
        verify(geoNamesClient, never()).searchByCountry("IN", 10, 30);
    }

    @Test void stopsPagingEarlyWhenGeoNamesRunsOutOfResults() {
        var geoNamesClient = mock(GeoNamesClient.class);
        var frankfurterClient = mock(FrankfurterClient.class);
        var cityRepository = mock(LifestyleMasterCityRepository.class);
        var currencyRepository = mock(LifestyleMasterCurrencyRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var cityPincodeRepository = mock(CityPincodeRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);
        var masters = new MasterRefreshProperties(List.of(), "INR", 100, 10, 5, 20);

        when(geoNamesClient.isConfigured()).thenReturn(true);
        when(geoNamesClient.searchByCountry("IN", 10, 0))
            .thenReturn(IntStream.range(1, 4).mapToObj(MasterDataRefreshUseCaseCityDiscoveryTest::entry).toList());
        when(geoNamesClient.searchByCountry("IN", 10, 10)).thenReturn(List.of()); // real limit reached, not a bug
        when(cityRepository.upsert(anyString(), eq("IN"), any(), any(), any()))
            .thenReturn(new CityMaster(UUID.randomUUID(), "City", "IN", 20.0, 77.0, "GeoNames", "2026-09-19T00:00:00Z"));
        when(cityRepository.findAll()).thenReturn(List.of());
        when(frankfurterClient.latest("INR"))
            .thenReturn(new com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterRates("INR", "2026-09-19", java.util.Map.of()));

        var useCase = new MasterDataRefreshUseCase(geoNamesClient, frankfurterClient, cityRepository,
            currencyRepository, codedMasterRepository, cityPincodeRepository, masterRefreshLogRepository, masters);
        var outcomes = useCase.refreshAll();

        assertEquals(3, outcomes.get(0).recordsUpserted());
        verify(geoNamesClient, never()).searchByCountry("IN", 10, 20);
    }

    @Test void neverThrowsWhenFrankfurterReturnsZeroRates() {
        // A partial/empty provider result must never surface as an
        // uncaught exception (a 500) — this is the concrete meaning of the
        // "no-500 behavior" requirement for a partial provider response.
        var geoNamesClient = mock(GeoNamesClient.class);
        var frankfurterClient = mock(FrankfurterClient.class);
        var cityRepository = mock(LifestyleMasterCityRepository.class);
        var currencyRepository = mock(LifestyleMasterCurrencyRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var cityPincodeRepository = mock(CityPincodeRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);
        var masters = new MasterRefreshProperties(List.of(), "INR", 100, 100, 5, 20);

        when(geoNamesClient.isConfigured()).thenReturn(true);
        when(geoNamesClient.searchByCountry(anyString(), anyInt(), anyInt())).thenReturn(List.of());
        when(cityRepository.findAll()).thenReturn(List.of());
        when(frankfurterClient.latest("INR"))
            .thenReturn(new com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterRates("INR", "2026-09-19", java.util.Map.of()));

        var useCase = new MasterDataRefreshUseCase(geoNamesClient, frankfurterClient, cityRepository,
            currencyRepository, codedMasterRepository, cityPincodeRepository, masterRefreshLogRepository, masters);
        var outcomes = useCase.refreshAll();

        var currencyOutcome = outcomes.get(1);
        assertEquals("PARTIAL", currencyOutcome.status());
        assertEquals(1, currencyOutcome.recordsUpserted());
        assertTrue(currencyOutcome.errorMessage().contains("zero rates"));
    }
}
