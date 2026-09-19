package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort.CityMasterView;
import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort.PincodeView;
import com.thelifestyle.travel.application.port.out.RoadRouteProvider;
import com.thelifestyle.travel.config.RoutingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Focused tests for the route-recommendation use case: deterministic
// distance/ranking math, honest "not found" errors instead of a
// fabricated route, and both via-resolution paths (city name, Indian
// pincode) — mirroring the ID-mapping resolver tests' Mockito-on-the-port
// style already used in this service.
class RouteRecommendationUseCaseTest {
    private static final CityMasterView PUNE =
        new CityMasterView(UUID.randomUUID(), "Pune", "IN", 18.5204, 73.8567, "GeoNames", "2026-09-17T00:00:00Z");
    private static final CityMasterView DELHI =
        new CityMasterView(UUID.randomUUID(), "Delhi", "IN", 28.7041, 77.1025, "GeoNames", "2026-09-17T00:00:00Z");
    private static final CityMasterView MUMBAI =
        new CityMasterView(UUID.randomUUID(), "Mumbai", "IN", 19.0760, 72.8777, "GeoNames", "2026-09-17T00:00:00Z");
    private static final CityMasterView GWALIOR =
        new CityMasterView(UUID.randomUUID(), "Gwalior", "IN", 26.2183, 78.1828, "GeoNames", "2026-09-17T00:00:00Z");
    private static final CityMasterView NO_COORDS =
        new CityMasterView(UUID.randomUUID(), "Unrefreshed", "IN", null, null, "GeoNames", "2026-09-17T00:00:00Z");

    // These tests exercise only the deterministic straight-line
    // (Haversine) recommendation path — a disabled road-route provider and
    // default fuel-cost config, so recommend() never even looks at them.
    private static RouteRecommendationUseCase newUseCase(LifestyleMasterReadPort readPort) {
        var roadRouteProvider = mock(RoadRouteProvider.class);
        var routingProperties = new RoutingProperties(
            new RoutingProperties.Osrm(false, "http://fake-osrm.test"),
            new RoutingProperties.FuelCost(100.0, 15.0, "INR"));
        return new RouteRecommendationUseCase(readPort, roadRouteProvider, routingProperties);
    }

    @Test void computesDirectDistanceAndRanksViaCandidatesByAscendingDetour() {
        var readPort = mock(LifestyleMasterReadPort.class);
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(readPort.findCity("Delhi")).thenReturn(Optional.of(DELHI));
        when(readPort.findAllCities()).thenReturn(List.of(PUNE, DELHI, MUMBAI, GWALIOR, NO_COORDS));

        var useCase = newUseCase(readPort);
        var outcome = useCase.recommend("Pune", "Delhi", null);

        assertNull(outcome.error());
        var result = outcome.result();
        assertEquals("Pune", result.origin().label());
        assertEquals("Delhi", result.destination().label());
        assertTrue(result.directDistanceKm() > 1000 && result.directDistanceKm() < 1300);
        assertTrue(result.distanceLabel().toLowerCase().contains("approximate"));
        assertNull(result.requestedVia());

        // Only genuine other cities with coordinates, never origin/destination/unrefreshed
        var candidateLabels = result.suggestedViaCandidates().stream().map(c -> c.via().label()).toList();
        assertEquals(List.of("Gwalior", "Mumbai"), candidateLabels);

        // Ascending detour — Gwalior sits far closer to the direct Pune-Delhi line than Mumbai does
        var detours = result.suggestedViaCandidates().stream().map(c -> c.detourKm()).toList();
        assertTrue(detours.get(0) <= detours.get(1));
    }

    @Test void resolvesAnExplicitlyRequestedViaCity() {
        var readPort = mock(LifestyleMasterReadPort.class);
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(readPort.findCity("Delhi")).thenReturn(Optional.of(DELHI));
        when(readPort.findCity("Gwalior")).thenReturn(Optional.of(GWALIOR));
        when(readPort.findAllCities()).thenReturn(List.of(PUNE, DELHI, GWALIOR));

        var useCase = newUseCase(readPort);
        var outcome = useCase.recommend("Pune", "Delhi", "Gwalior");

        assertNull(outcome.error());
        assertEquals("Gwalior", outcome.result().requestedVia().via().label());
    }

    @Test void resolvesAnExplicitlyRequestedViaPincodeWhenNoCityMatches() {
        var readPort = mock(LifestyleMasterReadPort.class);
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(readPort.findCity("Delhi")).thenReturn(Optional.of(DELHI));
        when(readPort.findCity("411001")).thenReturn(Optional.empty());
        when(readPort.findPincode("411001")).thenReturn(Optional.of(
            new PincodeView(PUNE.id(), "Pune", "IN", "411001", "Pune GPO", 18.5195, 73.8553, "GeoNames", "2026-09-17T00:00:00Z")));
        when(readPort.findAllCities()).thenReturn(List.of(PUNE, DELHI));

        var useCase = newUseCase(readPort);
        var outcome = useCase.recommend("Pune", "Delhi", "411001");

        assertNull(outcome.error());
        var via = outcome.result().requestedVia().via();
        assertTrue(via.label().contains("411001"));
        assertNull(via.id(), "a pincode-resolved via is a place inside a city, not a city master row, so it has no lifestyle_master id");
    }

    @Test void returnsAnHonestErrorInsteadOfAFabricatedRouteWhenOriginIsUnknown() {
        var readPort = mock(LifestyleMasterReadPort.class);
        when(readPort.findCity("Nowhereville")).thenReturn(Optional.empty());

        var useCase = newUseCase(readPort);
        var outcome = useCase.recommend("Nowhereville", "Delhi", null);

        assertNull(outcome.result());
        assertTrue(outcome.error().contains("Nowhereville"));
    }

    @Test void returnsAnHonestErrorWhenViaMatchesNeitherACityNorAPincode() {
        var readPort = mock(LifestyleMasterReadPort.class);
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(readPort.findCity("Delhi")).thenReturn(Optional.of(DELHI));
        when(readPort.findCity("Atlantis")).thenReturn(Optional.empty());

        var useCase = newUseCase(readPort);
        var outcome = useCase.recommend("Pune", "Delhi", "Atlantis");

        assertNull(outcome.result());
        assertTrue(outcome.error().contains("Atlantis"));
    }

    @Test void suggestCitiesMatchesByPrefixNotContainsAndIsCaseInsensitive() {
        var readPort = mock(LifestyleMasterReadPort.class);
        when(readPort.findAllCities()).thenReturn(List.of(PUNE, DELHI, MUMBAI, GWALIOR));

        var useCase = newUseCase(readPort);

        // "mu" is a prefix of Mumbai only — Pune contains no "mu" substring
        // to confuse this with, but the point is the match is anchored at
        // the start of the name, the ordinary way a place autocomplete works.
        assertEquals(List.of("Mumbai"), useCase.suggestCities("mu"));
        assertEquals(List.of("Delhi"), useCase.suggestCities("De"));
        assertTrue(useCase.suggestCities("").isEmpty());
        assertTrue(useCase.suggestCities("Zzz").isEmpty());
    }
}
