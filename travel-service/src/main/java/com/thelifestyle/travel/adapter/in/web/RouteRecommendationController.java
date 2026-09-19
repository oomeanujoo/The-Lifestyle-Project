package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.RouteRecommendationUseCase;
import com.thelifestyle.travel.domain.FuelCostEstimate;
import com.thelifestyle.travel.domain.RoadRoute;
import com.thelifestyle.travel.domain.RouteCandidate;
import com.thelifestyle.travel.domain.RoutePoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// A demo, end-to-end route-planning endpoint over the real GeoNames-backed
// masters integration-service refreshes (§17/§20) — origin/destination/via
// are resolved against lifestyle_master.city and integration's staged
// Indian pincode data, never a hardcoded or fabricated coordinate. Every
// distance returned is an approximate straight-line (Haversine) measure,
// clearly labeled as such; this endpoint never invents a fare, travel
// time, schedule, or real road route.
@RestController
@RequestMapping("/api/travel/v1")
public class RouteRecommendationController {
    private final RouteRecommendationUseCase useCase;

    public RouteRecommendationController(RouteRecommendationUseCase useCase) {
        this.useCase = useCase;
    }

    // Live typeahead for the From/To/Via inputs — reads real
    // lifestyle_master.city data on every keystroke's request, never a
    // bundled frontend array. `q` shorter than 2 characters returns
    // nothing, matching the existing PlaceAutocomplete convention.
    @GetMapping("/routes/city-suggestions")
    public List<String> citySuggestions(@RequestParam(name = "q", defaultValue = "") String q) {
        return useCase.suggestCities(q);
    }

    @GetMapping("/routes/recommend")
    public ResponseEntity<?> recommend(
        @RequestParam String origin,
        @RequestParam String destination,
        @RequestParam(required = false) String via
    ) {
        var outcome = useCase.recommend(origin, destination, via);
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }

        var result = outcome.result();
        var response = new RouteRecommendationResponse(
            toResponse(result.origin()),
            toResponse(result.destination()),
            result.directDistanceKm(),
            result.distanceLabel(),
            result.requestedVia() == null ? null : toResponse(result.requestedVia()),
            result.suggestedViaCandidates().stream().map(this::toResponse).toList());
        return ResponseEntity.ok(response);
    }

    // Separate, clearly-named endpoint rather than changing /recommend's
    // existing response shape — the current single-search UI contract
    // (Cline's own task) stays untouched; this is additive.
    @GetMapping("/routes/recommend-road")
    public ResponseEntity<?> recommendRoad(
        @RequestParam String origin,
        @RequestParam String destination,
        @RequestParam(required = false) List<String> via
    ) {
        var outcome = useCase.recommendRoadWithVias(origin, destination, via == null ? List.of() : via);
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }

        var result = outcome.result();
        var response = new RoadJourneyResponse(
            toResponse(result.origin()),
            toResponse(result.destination()),
            result.via() == null ? null : toResponse(result.via()),
            result.vias().stream().map(this::toResponse).toList(),
            result.suggestedViaCandidates().stream().map(this::toResponse).toList(),
            result.straightLineDistanceKm(),
            result.straightLineLabel(),
            result.roadRoute() == null ? null : toResponse(result.roadRoute()),
            result.roadRouteUnavailableReason(),
            result.fuelCostEstimate() == null ? null : toResponse(result.fuelCostEstimate()));
        return ResponseEntity.ok(response);
    }

    private RoadRouteResponse toResponse(RoadRoute route) {
        return new RoadRouteResponse(route.distanceKm(), route.durationMinutes(), route.geometry(), route.source(), route.capturedAt());
    }

    private FuelCostEstimateResponse toResponse(FuelCostEstimate estimate) {
        return new FuelCostEstimateResponse(
            estimate.amount(), estimate.currency(), estimate.fuelPricePerLitre(), estimate.vehicleKmPerLitre(), estimate.label());
    }

    private RoutePointResponse toResponse(RoutePoint point) {
        return new RoutePointResponse(point.id() == null ? null : point.id().toString(), point.label(),
            point.countryCode(), point.latitude(), point.longitude(), point.source(), point.asOf());
    }

    private RouteCandidateResponse toResponse(RouteCandidate candidate) {
        return new RouteCandidateResponse(toResponse(candidate.via()), candidate.originToViaKm(),
            candidate.viaToDestinationKm(), candidate.totalKm(), candidate.detourKm());
    }
}
