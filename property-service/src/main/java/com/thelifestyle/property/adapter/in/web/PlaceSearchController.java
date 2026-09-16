package com.thelifestyle.property.adapter.in.web;

import com.thelifestyle.property.application.PlaceSearchUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Backend counterpart of lifestyle-web's PlaceAutocomplete — same DB-first,
// AI-fallback-notice contract, mirrored field-for-field with the frontend's
// PlaceMatch type in src/lib/placeSearch.ts, so the two can be wired
// together directly once there's a reason to call this from the browser.
@RestController
@RequestMapping("/api/property/v1")
public class PlaceSearchController {
    private final PlaceSearchUseCase useCase;

    public PlaceSearchController(PlaceSearchUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/places/search")
    public PlaceSearchResponse search(@RequestParam(name = "q", defaultValue = "") String q) {
        var result = useCase.search(q);
        var matches = result.matches().stream()
            .map(place -> new PlaceMatchResponse(place.id(), place.label(), place.sublabel(), place.kind().name(), place.path()))
            .toList();
        return new PlaceSearchResponse(matches, result.aiFallbackNotice());
    }
}
