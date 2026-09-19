package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterClient;
import com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterRates;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Pins the explicit ask: ONE refresh action (the Settings page's single
// button) pulls every master this service has a real source for — city,
// currency, AND Indian postal codes — not just the first two. Before this
// pass, india_pincode was only reachable via the separate, narrower
// /refresh/india-geo endpoint.
class MasterDataRefreshUseCaseRefreshAllTest {

    @Test void refreshAllPullsCityCurrencyAndIndianPincodesInOneCall() {
        var geoNamesClient = mock(GeoNamesClient.class);
        var frankfurterClient = mock(FrankfurterClient.class);
        var cityRepository = mock(LifestyleMasterCityRepository.class);
        var currencyRepository = mock(LifestyleMasterCurrencyRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var cityPincodeRepository = mock(CityPincodeRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);
        var masters = new MasterRefreshProperties(List.of("Pune"), "INR", 100, 100, 5, 20);

        when(geoNamesClient.isConfigured()).thenReturn(true);
        var puneId = UUID.randomUUID();
        var pune = new CityMaster(puneId, "Pune", "IN", 18.52, 73.85, "GeoNames", "2026-09-19T00:00:00Z");
        when(geoNamesClient.search("Pune", 5)).thenReturn(
            List.of(new GeoNamesEntry(1L, "Pune", "India", "IN", "Maharashtra", "18.52", "73.85")));
        when(cityRepository.upsert(eq("Pune"), eq("IN"), any(), any(), any())).thenReturn(pune);
        when(geoNamesClient.searchByCountry(eq("IN"), anyInt(), anyInt())).thenReturn(List.of());
        when(cityRepository.findAll()).thenReturn(List.of(pune));
        when(geoNamesClient.searchPostalCodes(anyString(), anyString(), anyInt())).thenReturn(List.of());
        when(frankfurterClient.latest(eq("INR"))).thenReturn(new FrankfurterRates("INR", "2026-09-19", Map.of("USD", 0.012)));

        var useCase = new MasterDataRefreshUseCase(geoNamesClient, frankfurterClient, cityRepository,
            currencyRepository, codedMasterRepository, cityPincodeRepository, masterRefreshLogRepository, masters);

        var outcomes = useCase.refreshAll();

        assertEquals(3, outcomes.size());
        assertEquals("city", outcomes.get(0).masterName());
        assertEquals("currency", outcomes.get(1).masterName());
        assertEquals("india_pincode", outcomes.get(2).masterName());
    }
}
