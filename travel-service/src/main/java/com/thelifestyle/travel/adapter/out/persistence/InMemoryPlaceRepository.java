package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.PlaceRepository;
import com.thelifestyle.travel.domain.Place;
import com.thelifestyle.travel.domain.PlaceKind;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

// Temporary stand-in for the real `trip_leg`/`city` master query designed in
// TECHNICAL_ARCHITECTURE.md §16-§17 — mirrors lifestyle-web's tripData.ts
// illustrative places exactly, so backend and frontend agree on what exists
// until a real Postgres schema + Flyway migration exist locally. This is the
// only class that implements PlaceRepository, so swapping it for a JPA
// adapter later touches nothing in the application layer above it.
@Component
public class InMemoryPlaceRepository implements PlaceRepository {

    private record KnownPlace(String name, List<String> tripTitles) {}

    private static final List<KnownPlace> PLACES = List.of(
        new KnownPlace("Pune", List.of("Pune → Gwalior → Pune", "Pune → Dubai → Pune")),
        new KnownPlace("Mumbai", List.of("Pune → Gwalior → Pune")),
        new KnownPlace("Gwalior", List.of("Pune → Gwalior → Pune")),
        new KnownPlace("Delhi", List.of("Pune → Gwalior → Pune")),
        new KnownPlace("Dubai", List.of("Pune → Dubai → Pune"))
    );

    @Override
    public List<Place> search(String query) {
        var needle = query.toLowerCase(Locale.ROOT);
        return PLACES.stream()
            .filter(place -> place.name().toLowerCase(Locale.ROOT).contains(needle))
            .map(place -> new Place(
                "travel-place-" + place.name(),
                place.name(),
                "Appears in: " + String.join(", ", place.tripTitles()),
                PlaceKind.TRAVEL_PLACE))
            .toList();
    }
}
