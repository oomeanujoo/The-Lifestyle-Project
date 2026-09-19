package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.geonames.GeoNamesClient;
import com.thelifestyle.integration.application.port.out.LifestyleMasterCityRepository;
import com.thelifestyle.integration.domain.CityMaster;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

// Deliberate, one-city acquisition. AI may suggest a name elsewhere, but a
// city is authoritative only after GeoNames confirms its populated-place
// identity and usable coordinates. Existing master rows are never removed.
@Service
public class CityAcquisitionUseCase {
    private final GeoNamesClient geoNames;
    private final LifestyleMasterCityRepository cities;

    public CityAcquisitionUseCase(GeoNamesClient geoNames, LifestyleMasterCityRepository cities) {
        this.geoNames = geoNames;
        this.cities = cities;
    }

    public Optional<CityMaster> acquire(String rawName, String rawCountryCode) {
        if (rawName == null || rawName.isBlank() || rawName.length() > 100) {
            throw new IllegalArgumentException("City name must contain 1 to 100 characters");
        }
        if (rawCountryCode == null || !rawCountryCode.matches("(?i)[A-Z]{2}")) {
            throw new IllegalArgumentException("Country code must be two letters");
        }
        var name = rawName.strip();
        var country = rawCountryCode.toUpperCase(Locale.ROOT);
        var existing = cities.findByName(name).stream()
            .filter(city -> country.equalsIgnoreCase(city.countryCode()))
            .findFirst();
        if (existing.isPresent()) return existing;

        return geoNames.searchPopulatedPlaces(name, country, 20).stream()
            .filter(entry -> entry.geonameId() > 0
                && name.equalsIgnoreCase(entry.name())
                && country.equalsIgnoreCase(entry.countryCode()))
            .filter(entry -> validCoordinate(entry.lat(), -90, 90)
                && validCoordinate(entry.lng(), -180, 180))
            .findFirst()
            .map(entry -> cities.upsert(entry.name(), country,
                Double.parseDouble(entry.lat()), Double.parseDouble(entry.lng()), "GeoNames"));
    }

    private boolean validCoordinate(String raw, double min, double max) {
        try {
            double value = Double.parseDouble(raw);
            return Double.isFinite(value) && value >= min && value <= max;
        } catch (NumberFormatException | NullPointerException ex) {
            return false;
        }
    }
}
