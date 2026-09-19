package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.travel.application.port.out.PlaceRepository;
import com.thelifestyle.travel.domain.Place;
import com.thelifestyle.travel.domain.PlaceKind;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

// Replaces the earlier InMemoryPlaceRepository — a hardcoded 5-city
// fixture list — with a real search over lifestyle_master.city, through
// the same LifestyleMasterReadPort the route-recommendation feature
// already reads (§21). Never a bundled list; every result is a real,
// GeoNames-refreshed city with its actual source and freshness. This is
// the only class that implements PlaceRepository, so nothing above this
// adapter (PlaceSearchUseCase, PlaceSearchController) needed to change.
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
                PlaceKind.TRAVEL_PLACE))
            .toList();
    }

    private static String sublabel(LifestyleMasterReadPort.CityMasterView city) {
        var source = city.source() != null ? city.source() : "unknown source";
        return "City · " + city.countryCode() + " · " + source;
    }
}
