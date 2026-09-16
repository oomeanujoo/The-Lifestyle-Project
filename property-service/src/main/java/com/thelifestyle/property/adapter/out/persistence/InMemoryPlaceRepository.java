package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.PlaceRepository;
import com.thelifestyle.property.domain.Place;
import com.thelifestyle.property.domain.PlaceKind;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Temporary stand-in for the real city/area/locality master query designed
// in TECHNICAL_ARCHITECTURE.md §16.2/§17 — mirrors lifestyle-web's
// cityData.ts/areaData.ts illustrative data exactly, so backend and frontend
// agree on what exists until a real Postgres schema + Flyway migration
// exist locally. This is the only class that implements PlaceRepository, so
// swapping it for a JPA adapter later touches nothing in the application
// layer above it.
@Component
public class InMemoryPlaceRepository implements PlaceRepository {

    private record LocalityFixture(String id, String name, String pincode) {}
    private record AreaFixture(String id, String name, List<LocalityFixture> localities) {}
    private record CityFixture(String id, String name, List<AreaFixture> areas) {}

    private static final List<CityFixture> CITIES = List.of(
        new CityFixture("pune", "Pune", List.of(
            new AreaFixture("koregaon-park", "Koregaon Park", List.of(
                new LocalityFixture("kp-north-main-road", "North Main Road", "411001"),
                new LocalityFixture("kp-lane-6", "Lane 6", "411001"))),
            new AreaFixture("aundh", "Aundh", List.of(
                new LocalityFixture("aundh-gandhi-nagar", "Gandhi Nagar", "411007"),
                new LocalityFixture("aundh-dp-road", "DP Road", "411007"))),
            new AreaFixture("hinjewadi", "Hinjewadi", List.of(
                new LocalityFixture("hinjewadi-phase-1", "Phase 1", "411057"),
                new LocalityFixture("hinjewadi-phase-2", "Phase 2", "411057"))))),
        new CityFixture("bengaluru", "Bengaluru", List.of())
    );

    @Override
    public List<Place> search(String query) {
        var needle = query.toLowerCase(Locale.ROOT);
        var results = new ArrayList<Place>();

        for (var city : CITIES) {
            if (contains(city.name(), needle)) {
                results.add(new Place(
                    "city-" + city.id(), city.name(), "City", PlaceKind.CITY, "/property/cities/" + city.id()));
            }

            for (var area : city.areas()) {
                if (contains(area.name(), needle)) {
                    results.add(new Place(
                        "area-" + city.id() + "-" + area.id(),
                        area.name(),
                        "Area · " + city.name(),
                        PlaceKind.AREA,
                        "/property/cities/" + city.id() + "/areas/" + area.id()));
                }

                for (var locality : area.localities()) {
                    if (!contains(locality.name(), needle)) continue;
                    // Same locality name can exist under different areas/cities —
                    // parent context in the label disambiguates them (§18 "Gandhi
                    // Road, Pune" vs "Gandhi Road, Mumbai"), never an AI guess.
                    results.add(new Place(
                        "locality-" + city.id() + "-" + area.id() + "-" + locality.id(),
                        locality.name() + ", " + area.name(),
                        "Locality · " + city.name() + " · " + locality.pincode(),
                        PlaceKind.LOCALITY,
                        "/property/cities/" + city.id() + "/areas/" + area.id() + "/localities/" + locality.id()));
                }
            }
        }

        return results;
    }

    private static boolean contains(String name, String needle) {
        return name.toLowerCase(Locale.ROOT).contains(needle);
    }
}
