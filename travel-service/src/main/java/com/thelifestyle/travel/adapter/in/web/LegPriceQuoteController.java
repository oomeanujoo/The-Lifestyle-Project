package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.LegPriceQuoteUseCase;
import com.thelifestyle.travel.domain.LegPriceQuote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Manual price entry (§17, §19) — no live fare source exists for
// flights/trains/road, so every quote here is a person typing in a price
// they found. Append-only: no PATCH/PUT/DELETE endpoint exists, matching
// the database's own trigger that physically rejects those operations.
@RestController
@RequestMapping("/api/travel/v1/legs/{tripLegId}/price-quotes")
public class LegPriceQuoteController {
    private final LegPriceQuoteUseCase useCase;

    public LegPriceQuoteController(LegPriceQuoteUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID tripLegId, @RequestBody LegPriceQuoteRequest request) {
        var outcome = useCase.create(tripLegId, request.currency(), request.amount(), request.durationHours(),
            request.source(), request.sourceUrl(), request.confidence());
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.quote()));
    }

    @GetMapping
    public List<LegPriceQuoteResponse> findByTripLeg(@PathVariable UUID tripLegId) {
        return useCase.findByTripLeg(tripLegId).stream().map(this::toResponse).toList();
    }

    private LegPriceQuoteResponse toResponse(LegPriceQuote quote) {
        return new LegPriceQuoteResponse(quote.id().toString(), quote.tripLegId().toString(), quote.currencyId().toString(),
            quote.amount(), quote.durationHours(), quote.source(), quote.sourceUrl(), quote.confidence(), quote.capturedAt());
    }
}
