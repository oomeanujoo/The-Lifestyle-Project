package com.thelifestyle.property.application;

import com.thelifestyle.property.adapter.out.persistence.LiveCityPlaceRepository;
import com.thelifestyle.property.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.property.application.port.out.LifestyleMasterReadPort.CityMasterView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Searches real lifestyle_master.city data (via a mocked
// LifestyleMasterReadPort), never a hardcoded fixture tree. City-only —
// municipality/area/locality/pincode have no live source yet (§17.4), so
// this box deliberately doesn't search them rather than mixing real
// cities with stale hardcoded fixtures in one list.
class PlaceSearchUseCaseTest {
    private static final CityMasterView PUNE =
        new CityMasterView(UUID.randomUUID(), "Pune", "IN", 18.52, 73.85, "GeoNames", "2026-09-17T00:00:00Z");

    private static PlaceSearchUseCase useCaseWith(List<CityMasterView> cities) {
        var readPort = mock(LifestyleMasterReadPort.class);
        when(readPort.findAllCities()).thenReturn(cities);
        return new PlaceSearchUseCase(new LiveCityPlaceRepository(readPort));
    }

    @Test void returnsNoResultsBelowMinimumQueryLength() {
        var result = useCaseWith(List.of(PUNE)).search("P");
        assertTrue(result.matches().isEmpty());
        assertNull(result.aiFallbackNotice());
    }

    @Test void findsARealRefreshedCityCaseInsensitively() {
        var result = useCaseWith(List.of(PUNE)).search("pune");
        assertEquals(1, result.matches().size());
        assertEquals("Pune", result.matches().get(0).label());
        assertNull(result.aiFallbackNotice());
    }

    @Test void returnsAiFallbackNoticeWhenNothingMatchesLive() {
        var result = useCaseWith(List.of(PUNE)).search("Antarctica");
        assertTrue(result.matches().isEmpty());
        assertNotNull(result.aiFallbackNotice());
    }
}
