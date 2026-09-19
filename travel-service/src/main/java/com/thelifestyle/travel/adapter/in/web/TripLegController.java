package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.TripLegUseCase;
import com.thelifestyle.travel.domain.TripLeg;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// fromCity/toCity/transportMode are resolved server-side (TripLegUseCase,
// via LocalMasterIdResolver) against the refreshed lifestyle_master
// masters — this is the first real FK write exercising that resolver
// (§21 phase M).
@RestController
@RequestMapping("/api/travel/v1/route-options/{routeOptionId}/legs")
public class TripLegController {
    private final TripLegUseCase useCase;

    public TripLegController(TripLegUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID routeOptionId, @RequestBody TripLegRequest request) {
        var outcome = useCase.create(routeOptionId, request.fromCity(), request.toCity(), request.transportMode(), request.legOrder());
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.leg()));
    }

    @GetMapping
    public List<TripLegResponse> findByRouteOption(@PathVariable UUID routeOptionId) {
        return useCase.findByRouteOption(routeOptionId).stream().map(this::toResponse).toList();
    }

    private TripLegResponse toResponse(TripLeg leg) {
        return new TripLegResponse(leg.id().toString(), leg.routeOptionId().toString(), leg.fromCityId().toString(),
            leg.toCityId().toString(), leg.transportModeId().toString(), leg.legOrder(), leg.createdAt());
    }
}
