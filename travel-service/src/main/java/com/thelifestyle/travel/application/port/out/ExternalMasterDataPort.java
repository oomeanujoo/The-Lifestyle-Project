package com.thelifestyle.travel.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// This service never calls GeoNames/Frankfurter directly — every external
// call goes through integration-service (§18). The adapter behind this
// port is the only class that knows integration-service's HTTP shape.
public interface ExternalMasterDataPort {
    Optional<CityLookup> findCity(String name);

    Map<String, Double> fetchExchangeRates(String base, List<String> symbols);

    record CityLookup(String name, String countryCode, Double latitude, Double longitude) {}
}
