package com.thelifestyle.property.application;

import com.thelifestyle.property.application.port.out.PlaceRepository;
import com.thelifestyle.property.domain.Place;
import com.thelifestyle.property.domain.PlaceSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

// DB-first search, per TECHNICAL_ARCHITECTURE.md §18: the repository (today
// an in-memory placeholder, later a real Postgres query over city/area/
// locality masters) is checked first and its results returned as-is — no AI
// call happens in this class. Only when it finds nothing does this return a
// non-null aiFallbackNotice, which the controller passes straight through.
// The actual AI-backed fallback call lives in integration-service, not
// here — see §18's "Where the external API calls actually live" note for why.
@Service
public class PlaceSearchUseCase {
    private static final int MIN_QUERY_LENGTH = 2;
    private static final String AI_FALLBACK_NOTICE =
        "No local match. Once an AI provider is wired up (see TECHNICAL_ARCHITECTURE.md §18), "
            + "an AI-drafted suggestion would appear here instead — clearly marked unverified, never silently trusted.";

    private final PlaceRepository placeRepository;

    public PlaceSearchUseCase(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public PlaceSearchResult search(String rawQuery) {
        var query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < MIN_QUERY_LENGTH) return new PlaceSearchResult(List.of(), null);

        List<Place> matches = placeRepository.search(query);
        return matches.isEmpty()
            ? new PlaceSearchResult(List.of(), AI_FALLBACK_NOTICE)
            : new PlaceSearchResult(matches, null);
    }
}
