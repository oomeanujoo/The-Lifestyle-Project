package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.application.PlaceSuggestionUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Called by travel-service/property-service's PlaceSearchUseCase when their
// own local search finds nothing — the AI-fallback step designed in
// TECHNICAL_ARCHITECTURE.md §18. Not yet actually called over HTTP by
// either service (that cross-service wiring is a follow-up); this endpoint
// is real and callable today on its own.
@RestController
@RequestMapping("/api/integration/v1")
public class PlaceSuggestionController {
    private final PlaceSuggestionUseCase useCase;

    public PlaceSuggestionController(PlaceSuggestionUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/ai/place-suggestions")
    public PlaceSuggestionResponse suggest(@RequestParam(name = "q", defaultValue = "") String q) {
        var result = useCase.suggest(q);
        return new PlaceSuggestionResponse(result.suggestions(), result.disclaimer(),
            result.draftId(), result.acceptanceStatus());
    }
}
