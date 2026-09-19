package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort.CityMasterView;
import com.thelifestyle.travel.application.port.out.RoadRouteProvider;
import com.thelifestyle.travel.application.port.out.RoadRouteProviderException;
import com.thelifestyle.travel.config.RoutingProperties;
import com.thelifestyle.travel.domain.RoadRoute;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

// Focused tests for recommendRoad() — direct and via journeys through a
// real (fixture) road-route provider, honest fallback to the straight-line
// comparison when the provider fails or is disabled, and the deterministic
// fuel-cost calculation. Never a fabricated road distance/duration — a
// provider failure always produces an explained fallback, never a 500 or
// a silently-invented value.
class RouteRecommendationUseCaseRoadRouteTest {
    private static final CityMasterView PUNE =
        new CityMasterView(UUID.randomUUID(), "Pune", "IN", 18.5204, 73.8567, "GeoNames", "2026-09-17T00:00:00Z");
    private static final CityMasterView MUMBAI =
        new CityMasterView(UUID.randomUUID(), "Mumbai", "IN", 19.0760, 72.8777, "GeoNames", "2026-09-17T00:00:00Z");
    private static final CityMasterView GWALIOR =
        new CityMasterView(UUID.randomUUID(), "Gwalior", "IN", 26.2183, 78.1828, "GeoNames", "2026-09-17T00:00:00Z");

    private static RoutingProperties routingProperties(boolean osrmEnabled) {
        return new RoutingProperties(
            new RoutingProperties.Osrm(osrmEnabled, "http://fake-osrm.test"),
            new RoutingProperties.FuelCost(100.0, 20.0, "INR"));
    }

    @Test void returnsARealRoadRouteAndAFuelCostEstimateForADirectJourney() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var roadRouteProvider = mock(RoadRouteProvider.class);
        when(readPort.findCity("Mumbai")).thenReturn(Optional.of(MUMBAI));
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(roadRouteProvider.isConfigured()).thenReturn(true);
        when(roadRouteProvider.route(any()))
            .thenReturn(new RoadRoute(150.0, 180.0, "encoded-polyline", "OSRM demo (router.project-osrm.org)", "2026-09-19T00:00:00Z"));

        var useCase = new RouteRecommendationUseCase(readPort, roadRouteProvider, routingProperties(true));
        var outcome = useCase.recommendRoad("Mumbai", "Pune", null);

        assertNull(outcome.error());
        var result = outcome.result();
        assertNotNull(result.roadRoute());
        assertEquals(150.0, result.roadRoute().distanceKm());
        assertNull(result.roadRouteUnavailableReason());
        assertNotNull(result.fuelCostEstimate());
        // 150km / 20km-per-litre * 100/litre = 750.0
        assertEquals(750.0, result.fuelCostEstimate().amount());
        assertTrue(result.fuelCostEstimate().label().toLowerCase().contains("excludes tolls"));
        // Straight-line comparison is always present too, regardless of the road route
        assertTrue(result.straightLineDistanceKm() > 0);
    }

    @Test void returnsARealRoadRouteThroughAnExplicitVia() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var roadRouteProvider = mock(RoadRouteProvider.class);
        when(readPort.findCity("Mumbai")).thenReturn(Optional.of(MUMBAI));
        when(readPort.findCity("Gwalior")).thenReturn(Optional.of(GWALIOR));
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(roadRouteProvider.isConfigured()).thenReturn(true);
        when(roadRouteProvider.route(any()))
            .thenReturn(new RoadRoute(900.0, 720.0, null, "OSRM demo (router.project-osrm.org)", "2026-09-19T00:00:00Z"));

        var useCase = new RouteRecommendationUseCase(readPort, roadRouteProvider, routingProperties(true));
        var outcome = useCase.recommendRoad("Mumbai", "Pune", "Gwalior");

        assertNull(outcome.error());
        assertEquals("Gwalior", outcome.result().via().label());
        assertEquals(900.0, outcome.result().roadRoute().distanceKm());
    }

    @Test void routesThroughMultipleViasInOrderAndSuggestsOtherMasterCities() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var roadRouteProvider = mock(RoadRouteProvider.class);
        when(readPort.findCity("Mumbai")).thenReturn(Optional.of(MUMBAI));
        when(readPort.findCity("Gwalior")).thenReturn(Optional.of(GWALIOR));
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(readPort.findAllCities()).thenReturn(List.of(PUNE, MUMBAI, GWALIOR));
        when(roadRouteProvider.isConfigured()).thenReturn(true);
        when(roadRouteProvider.route(any())).thenReturn(new RoadRoute(900, 720, null, "OSRM", "2026-09-19T00:00:00Z"));

        var result = new RouteRecommendationUseCase(readPort, roadRouteProvider, routingProperties(true))
            .recommendRoadWithVias("Pune", "Gwalior", List.of("Mumbai", "Pune")).result();

        assertEquals(List.of("Mumbai", "Pune"), result.vias().stream().map(point -> point.label()).toList());
        verify(roadRouteProvider).route(org.mockito.ArgumentMatchers.argThat(points ->
            points.stream().map(point -> point.label()).toList().equals(List.of("Pune", "Mumbai", "Pune", "Gwalior"))));
    }

    @Test void fallsBackToTheStraightLineComparisonWhenTheProviderIsDisabled() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var roadRouteProvider = mock(RoadRouteProvider.class);
        when(readPort.findCity("Mumbai")).thenReturn(Optional.of(MUMBAI));
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(roadRouteProvider.isConfigured()).thenReturn(false);

        var useCase = new RouteRecommendationUseCase(readPort, roadRouteProvider, routingProperties(false));
        var outcome = useCase.recommendRoad("Mumbai", "Pune", null);

        assertNull(outcome.error());
        var result = outcome.result();
        assertNull(result.roadRoute());
        assertNull(result.fuelCostEstimate());
        assertNotNull(result.roadRouteUnavailableReason());
        assertTrue(result.straightLineDistanceKm() > 0);
    }

    @Test void fallsBackHonestlyWhenTheProviderThrows() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var roadRouteProvider = mock(RoadRouteProvider.class);
        when(readPort.findCity("Mumbai")).thenReturn(Optional.of(MUMBAI));
        when(readPort.findCity("Pune")).thenReturn(Optional.of(PUNE));
        when(roadRouteProvider.isConfigured()).thenReturn(true);
        when(roadRouteProvider.route(any())).thenThrow(new RoadRouteProviderException("NO_ROUTE", "no road route found"));

        var useCase = new RouteRecommendationUseCase(readPort, roadRouteProvider, routingProperties(true));
        var outcome = useCase.recommendRoad("Mumbai", "Pune", null);

        assertNull(outcome.error());
        var result = outcome.result();
        assertNull(result.roadRoute());
        assertNull(result.fuelCostEstimate());
        assertTrue(result.roadRouteUnavailableReason().contains("NO_ROUTE"));
        assertTrue(result.straightLineDistanceKm() > 0);
    }

    @Test void returnsAnHonestErrorInsteadOfCallingTheProviderWhenOriginIsUnknown() {
        var readPort = mock(LifestyleMasterReadPort.class);
        var roadRouteProvider = mock(RoadRouteProvider.class);
        when(readPort.findCity("Nowhereville")).thenReturn(Optional.empty());

        var useCase = new RouteRecommendationUseCase(readPort, roadRouteProvider, routingProperties(true));
        var outcome = useCase.recommendRoad("Nowhereville", "Pune", null);

        assertNull(outcome.result());
        assertTrue(outcome.error().contains("Nowhereville"));
    }
}
