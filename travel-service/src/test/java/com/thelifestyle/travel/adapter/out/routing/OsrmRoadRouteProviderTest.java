package com.thelifestyle.travel.adapter.out.routing;

import com.thelifestyle.travel.application.port.out.RoadRouteProviderException;
import com.thelifestyle.travel.config.RoutingProperties;
import com.thelifestyle.travel.domain.RoutePoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// MockRestServiceServer (bundled with spring-boot-starter-test), same
// pattern as integration-service's GeoNamesClientTest/FrankfurterClientTest
// — proves real response parsing and explicit-failure classification
// without ever hitting the real OSRM demo server.
class OsrmRoadRouteProviderTest {
    private static final RoutePoint MUMBAI = new RoutePoint(UUID.randomUUID(), "Mumbai", "IN", 19.0760, 72.8777, "GeoNames", "2026-09-19T00:00:00Z");
    private static final RoutePoint PUNE = new RoutePoint(UUID.randomUUID(), "Pune", "IN", 18.5204, 73.8567, "GeoNames", "2026-09-19T00:00:00Z");

    private static RoutingProperties properties(boolean enabled) {
        return new RoutingProperties(new RoutingProperties.Osrm(enabled, "http://fake-osrm.test"),
            new RoutingProperties.FuelCost(100.0, 15.0, "INR"));
    }

    @Test void returnsARealRouteOnSuccess() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/route/v1/driving/")))
            .andRespond(withSuccess("""
                {"code":"Ok","routes":[{"distance":150000.0,"duration":10800.0,"geometry":"encoded-polyline"}]}
                """, MediaType.APPLICATION_JSON));

        var provider = new OsrmRoadRouteProvider(properties(true), restClientBuilder);
        var result = provider.route(List.of(MUMBAI, PUNE));

        assertEquals(150.0, result.distanceKm());
        assertEquals(180.0, result.durationMinutes());
        assertEquals("encoded-polyline", result.geometry());
        server.verify();
    }

    @Test void throwsNoRouteWhenOsrmReportsNoRoute() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/route/v1/driving/")))
            .andRespond(withSuccess("""
                {"code":"NoRoute","routes":[]}
                """, MediaType.APPLICATION_JSON));

        var provider = new OsrmRoadRouteProvider(properties(true), restClientBuilder);
        var ex = assertThrows(RoadRouteProviderException.class, () -> provider.route(List.of(MUMBAI, PUNE)));

        assertEquals("NO_ROUTE", ex.reason());
        server.verify();
    }

    @Test void throwsRateLimitedOnHttp429() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/route/v1/driving/")))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        var provider = new OsrmRoadRouteProvider(properties(true), restClientBuilder);
        var ex = assertThrows(RoadRouteProviderException.class, () -> provider.route(List.of(MUMBAI, PUNE)));

        assertEquals("RATE_LIMITED", ex.reason());
        server.verify();
    }

    @Test void throwsProviderErrorOnServerFailure() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/route/v1/driving/")))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        var provider = new OsrmRoadRouteProvider(properties(true), restClientBuilder);
        var ex = assertThrows(RoadRouteProviderException.class, () -> provider.route(List.of(MUMBAI, PUNE)));

        assertEquals("PROVIDER_ERROR", ex.reason());
        server.verify();
    }

    @Test void rejectsInvalidCoordinatesWithoutCallingTheNetwork() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        var noCoords = new RoutePoint(UUID.randomUUID(), "Unrefreshed", "IN", 0.0, 0.0, "GeoNames", "2026-09-19T00:00:00Z");

        var provider = new OsrmRoadRouteProvider(properties(true), restClientBuilder);
        var ex = assertThrows(RoadRouteProviderException.class, () -> provider.route(List.of(MUMBAI, noCoords)));

        assertEquals("INVALID_COORDINATES", ex.reason());
        assertTrue(ex.getMessage().contains("Unrefreshed"));
        server.verify(); // no expectations set — verifies zero requests were made
    }

    @Test void throwsNotConfiguredWithoutCallingTheNetworkWhenDisabled() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();

        var provider = new OsrmRoadRouteProvider(properties(false), restClientBuilder);
        var ex = assertThrows(RoadRouteProviderException.class, () -> provider.route(List.of(MUMBAI, PUNE)));

        assertEquals("NOT_CONFIGURED", ex.reason());
        server.verify();
    }
}
