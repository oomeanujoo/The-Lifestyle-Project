package com.thelifestyle.travel.adapter.out.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thelifestyle.travel.application.port.out.RoadRouteProvider;
import com.thelifestyle.travel.application.port.out.RoadRouteProviderException;
import com.thelifestyle.travel.config.RoutingProperties;
import com.thelifestyle.travel.domain.GeoDistance;
import com.thelifestyle.travel.domain.RoadRoute;
import com.thelifestyle.travel.domain.RoutePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// OSRM's free public demo server (router.project-osrm.org) — no key, no
// authentication, and explicitly no availability/SLA guarantee (its own
// usage policy asks for low request volume; this is a personal-demo
// integration, not an enterprise dependency). §10's "never fabricate a
// road or travel time" applies here in the other direction too: every
// distance/duration this class returns came from a real OSRM response, and
// every failure is thrown explicitly rather than degraded into a fake
// success — RouteRecommendationUseCase decides what "fall back to
// straight-line" means, this class never quietly approximates on its own.
@Component
public class OsrmRoadRouteProvider implements RoadRouteProvider {
    private static final Logger log = LoggerFactory.getLogger(OsrmRoadRouteProvider.class);
    private static final String SOURCE = "OSRM demo (router.project-osrm.org)";

    private final RoutingProperties.Osrm config;
    private final RestClient restClient;

    // Takes the injected, Spring-autoconfigured Builder (same pattern as
    // GeoNamesClient/FrankfurterClient) rather than RestClient.create()
    // directly, so tests can bind a MockRestServiceServer to it instead of
    // hitting the real network. Deliberately does not layer a custom
    // request factory/timeout on top the way OllamaAiProvider does in
    // integration-service — that builder bean is a shared singleton
    // (RestClientConfig), and mutating a cloned copy's request factory
    // still risks fighting a test's MockRestServiceServer binding, which
    // also configures the request factory on the same builder. `readPort`
    // failures (including a real timeout) are still fully handled below —
    // this only means the timeout duration itself uses whatever default
    // the shared builder carries, not a value unique to OSRM.
    public OsrmRoadRouteProvider(RoutingProperties properties, RestClient.Builder restClientBuilder) {
        this.config = properties.osrm();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean isConfigured() {
        return config.enabled() && config.baseUrl() != null && !config.baseUrl().isBlank();
    }

    @Override
    public RoadRoute route(List<RoutePoint> waypoints) {
        if (!isConfigured()) {
            throw new RoadRouteProviderException("NOT_CONFIGURED", "OSRM road-routing is disabled (OSRM_ENABLED=false)");
        }
        if (waypoints == null || waypoints.size() < 2) {
            throw new RoadRouteProviderException("INVALID_COORDINATES", "at least an origin and destination are required");
        }
        for (var point : waypoints) {
            if (!isValidCoordinate(point.latitude(), point.longitude())) {
                throw new RoadRouteProviderException("INVALID_COORDINATES",
                    "waypoint '" + point.label() + "' has no usable coordinates");
            }
        }

        var coordinates = waypoints.stream()
            .map(p -> "%s,%s".formatted(format(p.longitude()), format(p.latitude())))
            .collect(Collectors.joining(";"));

        try {
            var response = restClient.get()
                .uri(config.baseUrl() + "/route/v1/driving/{coordinates}?overview=full&geometries=polyline", coordinates)
                .header("User-Agent", "TheLifestyleProject/0.1 (personal route demo)")
                .header("Accept-Encoding", "identity")
                .retrieve()
                .body(OsrmResponse.class);

            if (response == null) {
                throw new RoadRouteProviderException("PROVIDER_ERROR", "empty response body from OSRM");
            }
            if (!"Ok".equalsIgnoreCase(response.code())) {
                // OSRM's own real "no road route exists between these
                // points" response — a genuine, honest outcome, not a
                // provider malfunction.
                throw new RoadRouteProviderException("NO_ROUTE",
                    "OSRM returned '" + response.code() + "' — no road route found between these points");
            }
            if (response.routes() == null || response.routes().isEmpty()) {
                throw new RoadRouteProviderException("NO_ROUTE", "OSRM returned 'Ok' but no routes — unexpected shape");
            }

            var best = response.routes().get(0);
            var distanceKm = GeoDistance.round2(best.distance() / 1000.0);
            var durationMinutes = GeoDistance.round2(best.duration() / 60.0);
            return new RoadRoute(distanceKm, durationMinutes, best.geometry(), SOURCE, Instant.now().toString());
        } catch (RoadRouteProviderException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            var reason = classifyHttpFailure(ex.getStatusCode());
            log.warn("[osrm] road-route call failed with HTTP {}: {}", ex.getStatusCode(), ex.getMessage());
            throw new RoadRouteProviderException(reason, "OSRM request failed: HTTP " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            // Covers connect/read timeouts and any other genuine network
            // failure — SimpleClientHttpRequestFactory throws a plain
            // java.net.SocketTimeoutException, not a Spring-specific type.
            log.warn("[osrm] road-route call failed", ex);
            throw new RoadRouteProviderException("TIMEOUT", "OSRM request failed or timed out: " + ex.getMessage(), ex);
        }
    }

    private static String classifyHttpFailure(HttpStatusCode status) {
        return status.value() == 429 ? "RATE_LIMITED" : "PROVIDER_ERROR";
    }

    private static boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
            && !(latitude == 0.0 && longitude == 0.0); // the "safe()" default this app's RoutePoint uses for a missing coordinate
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsrmResponse(String code, List<OsrmRoute> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsrmRoute(double distance, double duration, String geometry) {}
}
