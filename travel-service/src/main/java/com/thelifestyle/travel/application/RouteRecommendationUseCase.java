package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.travel.application.port.out.RoadRouteProvider;
import com.thelifestyle.travel.application.port.out.RoadRouteProviderException;
import com.thelifestyle.travel.config.RoutingProperties;
import com.thelifestyle.travel.domain.FuelCostEstimate;
import com.thelifestyle.travel.domain.GeoDistance;
import com.thelifestyle.travel.domain.RoadJourneyOutcome;
import com.thelifestyle.travel.domain.RoadJourneyResult;
import com.thelifestyle.travel.domain.RoadRoute;
import com.thelifestyle.travel.domain.RouteCandidate;
import com.thelifestyle.travel.domain.RoutePoint;
import com.thelifestyle.travel.domain.RouteRecommendationOutcome;
import com.thelifestyle.travel.domain.RouteRecommendationResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Origin/destination/via are resolved against the SAME refreshed masters
// Travel now reads through integration-service (§17/§20) — never a local
// or fabricated coordinate. Every distance here is computed locally and
// deterministically (Haversine, GeoDistance) and labeled as an approximate
// straight-line measure; this class never invents a fare, travel time,
// schedule, or real road route. Via candidates are ranked purely by how
// little detour they add versus the direct route — the same, reproducible
// math every time for the same masters.
@Service
public class RouteRecommendationUseCase {
    public static final String DISTANCE_LABEL =
        "Approximate straight-line distance (Haversine great-circle calculation over refreshed lifestyle_master coordinates) "
            + "— not a real road route, fare, travel time, or schedule.";

    private static final int MAX_VIA_CANDIDATES = 5;
    private static final int MAX_CITY_SUGGESTIONS = 8;

    private final LifestyleMasterReadPort readPort;
    private final RoadRouteProvider roadRouteProvider;
    private final RoutingProperties.FuelCost fuelCostConfig;

    public RouteRecommendationUseCase(LifestyleMasterReadPort readPort, RoadRouteProvider roadRouteProvider,
                                       RoutingProperties routingProperties) {
        this.readPort = readPort;
        this.roadRouteProvider = roadRouteProvider;
        this.fuelCostConfig = routingProperties.fuelCost();
    }

    // Backs the live typeahead for the From/To/Via inputs — reads the same
    // refreshed lifestyle_master.city list `recommend()` uses below, never a
    // hardcoded frontend array. A prefix match (not "contains") so typing
    // "Mu" doesn't surface every city with "mu" anywhere in the name — the
    // ordinary way a place-name autocomplete behaves.
    public List<String> suggestCities(String prefix) {
        if (!StringUtils.hasText(prefix)) return List.of();
        var needle = prefix.trim().toLowerCase(Locale.ROOT);
        return readPort.findAllCities().stream()
            .map(LifestyleMasterReadPort.CityMasterView::name)
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(needle))
            .sorted()
            .limit(MAX_CITY_SUGGESTIONS)
            .toList();
    }

    public RouteRecommendationOutcome recommend(String originQuery, String destinationQuery, String viaQuery) {
        if (!StringUtils.hasText(originQuery)) return RouteRecommendationOutcome.error("origin is required");
        if (!StringUtils.hasText(destinationQuery)) return RouteRecommendationOutcome.error("destination is required");

        var originCity = readPort.findCity(originQuery);
        if (originCity.isEmpty()) {
            return RouteRecommendationOutcome.error("origin '" + originQuery + "' was not found in the refreshed lifestyle_master.city masters");
        }
        var destinationCity = readPort.findCity(destinationQuery);
        if (destinationCity.isEmpty()) {
            return RouteRecommendationOutcome.error("destination '" + destinationQuery + "' was not found in the refreshed lifestyle_master.city masters");
        }

        var origin = toPoint(originCity.get());
        var destination = toPoint(destinationCity.get());
        if (!hasCoordinates(origin)) {
            return RouteRecommendationOutcome.error("origin '" + originQuery + "' has no recorded coordinates yet — refresh city masters first");
        }
        if (!hasCoordinates(destination)) {
            return RouteRecommendationOutcome.error("destination '" + destinationQuery + "' has no recorded coordinates yet — refresh city masters first");
        }

        var directKm = GeoDistance.haversineKm(origin.latitude(), origin.longitude(), destination.latitude(), destination.longitude());

        RouteCandidate requestedVia = null;
        if (StringUtils.hasText(viaQuery)) {
            var resolvedVia = resolveVia(viaQuery);
            if (resolvedVia.isEmpty()) {
                return RouteRecommendationOutcome.error(
                    "via '" + viaQuery + "' did not match a known city or a refreshed Indian pincode");
            }
            requestedVia = buildCandidate(origin, destination, resolvedVia.get(), directKm);
        }

        var candidates = readPort.findAllCities().stream()
            .filter(c -> hasCoordinates(c.latitude(), c.longitude()))
            .map(this::toPoint)
            .filter(p -> !sameCity(p, origin) && !sameCity(p, destination))
            .map(p -> buildCandidate(origin, destination, p, directKm))
            .sorted(Comparator.comparingDouble(RouteCandidate::detourKm).thenComparing(c -> c.via().label()))
            .limit(MAX_VIA_CANDIDATES)
            .toList();

        var result = new RouteRecommendationResult(origin, destination, GeoDistance.round2(directKm), DISTANCE_LABEL, requestedVia, candidates);
        return RouteRecommendationOutcome.ok(result);
    }

    // A real road route alongside the same Haversine straight-line
    // comparison recommend() already computes — origin/destination/via
    // resolution is identical (same masters, same honest errors), so this
    // reuses toPoint()/resolveVia()/hasCoordinates() rather than
    // duplicating that logic. The road route itself is always optional: a
    // disabled/failed/unavailable provider degrades to the straight-line
    // fields plus an honest reason, never a 500 and never a fabricated
    // road distance.
    public RoadJourneyOutcome recommendRoad(String originQuery, String destinationQuery, String viaQuery) {
        return recommendRoadWithVias(originQuery, destinationQuery,
            StringUtils.hasText(viaQuery) ? List.of(viaQuery) : List.of());
    }

    public RoadJourneyOutcome recommendRoadWithVias(String originQuery, String destinationQuery, List<String> viaQueries) {
        if (!StringUtils.hasText(originQuery)) return RoadJourneyOutcome.error("origin is required");
        if (!StringUtils.hasText(destinationQuery)) return RoadJourneyOutcome.error("destination is required");

        var originCity = readPort.findCity(originQuery);
        if (originCity.isEmpty()) {
            return RoadJourneyOutcome.error("origin '" + originQuery + "' was not found in the refreshed lifestyle_master.city masters");
        }
        var destinationCity = readPort.findCity(destinationQuery);
        if (destinationCity.isEmpty()) {
            return RoadJourneyOutcome.error("destination '" + destinationQuery + "' was not found in the refreshed lifestyle_master.city masters");
        }

        var origin = toPoint(originCity.get());
        var destination = toPoint(destinationCity.get());
        if (!hasCoordinates(origin)) {
            return RoadJourneyOutcome.error("origin '" + originQuery + "' has no recorded coordinates yet — refresh city masters first");
        }
        if (!hasCoordinates(destination)) {
            return RoadJourneyOutcome.error("destination '" + destinationQuery + "' has no recorded coordinates yet — refresh city masters first");
        }

        var vias = new ArrayList<RoutePoint>();
        for (var viaQuery : viaQueries == null ? List.<String>of() : viaQueries) {
            if (!StringUtils.hasText(viaQuery)) continue;
            var resolvedVia = resolveVia(viaQuery);
            if (resolvedVia.isEmpty()) {
                return RoadJourneyOutcome.error("via '" + viaQuery + "' did not match a known city or a refreshed Indian pincode");
            }
            vias.add(resolvedVia.get());
        }

        var directKm = GeoDistance.haversineKm(origin.latitude(), origin.longitude(), destination.latitude(), destination.longitude());
        var waypoints = new ArrayList<RoutePoint>();
        waypoints.add(origin);
        waypoints.addAll(vias);
        waypoints.add(destination);

        var suggestions = readPort.findAllCities().stream()
            .filter(c -> hasCoordinates(c.latitude(), c.longitude()))
            .map(this::toPoint)
            .filter(p -> !sameCity(p, origin) && !sameCity(p, destination)
                && vias.stream().noneMatch(v -> sameCity(v, p)))
            .map(p -> buildCandidate(origin, destination, p, directKm))
            .sorted(Comparator.comparingDouble(RouteCandidate::detourKm).thenComparing(c -> c.via().label()))
            .limit(MAX_VIA_CANDIDATES)
            .toList();

        RoadRoute roadRoute = null;
        String unavailableReason = null;
        FuelCostEstimate fuelCostEstimate = null;
        if (!roadRouteProvider.isConfigured()) {
            unavailableReason = "road-routing provider is disabled — showing the straight-line comparison only";
        } else {
            try {
                roadRoute = roadRouteProvider.route(waypoints);
                fuelCostEstimate = FuelCostEstimate.compute(
                    roadRoute.distanceKm(), fuelCostConfig.fuelPricePerLitre(), fuelCostConfig.vehicleKmPerLitre(), fuelCostConfig.currency());
            } catch (RoadRouteProviderException ex) {
                unavailableReason = "road route unavailable (" + ex.reason() + "): " + ex.getMessage();
            }
        }

        var result = new RoadJourneyResult(origin, destination, vias.isEmpty() ? null : vias.get(0), List.copyOf(vias),
            suggestions, GeoDistance.round2(directKm), DISTANCE_LABEL,
            roadRoute, unavailableReason, fuelCostEstimate);
        return RoadJourneyOutcome.ok(result);
    }

    private Optional<RoutePoint> resolveVia(String via) {
        var cityMatch = readPort.findCity(via);
        if (cityMatch.isPresent()) {
            var point = toPoint(cityMatch.get());
            return hasCoordinates(point) ? Optional.of(point) : Optional.empty();
        }
        if (!looksLikePincode(via)) return Optional.empty();
        return readPort.findPincode(via)
            .filter(p -> hasCoordinates(p.latitude(), p.longitude()))
            .map(p -> new RoutePoint(null, p.placeName() + " (" + p.pincode() + ")", p.countryCode(),
                p.latitude(), p.longitude(), p.source(), p.capturedAt()));
    }

    private RouteCandidate buildCandidate(RoutePoint origin, RoutePoint destination, RoutePoint via, double directKm) {
        var originToVia = GeoDistance.haversineKm(origin.latitude(), origin.longitude(), via.latitude(), via.longitude());
        var viaToDestination = GeoDistance.haversineKm(via.latitude(), via.longitude(), destination.latitude(), destination.longitude());
        var total = originToVia + viaToDestination;
        return new RouteCandidate(via, GeoDistance.round2(originToVia), GeoDistance.round2(viaToDestination),
            GeoDistance.round2(total), GeoDistance.round2(total - directKm));
    }

    private RoutePoint toPoint(LifestyleMasterReadPort.CityMasterView city) {
        return new RoutePoint(city.id(), city.name(), city.countryCode(), safe(city.latitude()), safe(city.longitude()),
            city.source(), city.updatedAt());
    }

    private static boolean sameCity(RoutePoint a, RoutePoint b) {
        return a.id() != null && a.id().equals(b.id());
    }

    private static boolean hasCoordinates(RoutePoint point) {
        return hasCoordinates(point.latitude(), point.longitude());
    }

    private static boolean hasCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null;
    }

    private static double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    // Indian pincodes are numeric (6 digits); this is a permissive check,
    // not a strict validator — findPincode() is the real source of truth
    // for whether it actually resolves to anything.
    private static boolean looksLikePincode(String via) {
        return via.chars().allMatch(Character::isDigit);
    }
}
