package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.property.application.port.out.PlaceRepository;
import com.thelifestyle.property.domain.Place;
import com.thelifestyle.property.domain.PlaceKind;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

// Replaces the earlier InMemoryPlaceRepository — a hardcoded city/area/
// locality fixture tree — with a real search over lifestyle_master.city,
// through the same LifestyleMasterReadPort the ID-mapping resolver already
// reads. Deliberately CITY ONLY: municipality/area/locality/pincode have
// no live source yet (data.gov.in's response shape was only confirmed
// live on 2026-09-19, and no refresh code writes those masters yet — see
// TECHNICAL_ARCHITECTURE.md §17.4) — mixing real cities with the old
// hardcoded area/locality fixtures in one unlabeled result list would be
// less honest than returning fewer, all-real results. Area/locality
// search returns once those masters have a real source wired up.
@Component
public class LiveCityPlaceRepository implements PlaceRepository {
    private final LifestyleMasterReadPort readPort;

    public LiveCityPlaceRepository(LifestyleMasterReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public List<Place> search(String query) {
        var needle = query.toLowerCase(Locale.ROOT);
        return readPort.findAllCities().stream()
            .filter(city -> city.name().toLowerCase(Locale.ROOT).contains(needle))
            .map(city -> new Place(
                "city-" + city.id(),
                city.name(),
                sublabel(city),
                PlaceKind.CITY,
                "/property/cities/" + city.id()))
            .toList();
    }

    private static String sublabel(LifestyleMasterReadPort.CityMasterView city) {
        var source = city.source() != null ? city.source() : "unknown source";
        return "City · " + city.countryCode() + " · " + source;
    }
}
