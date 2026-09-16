package com.thelifestyle.travel.adapter.out.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thelifestyle.travel.application.port.out.ExternalMasterDataPort;
import com.thelifestyle.travel.config.IntegrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// The only class in this service that talks to integration-service — every
// external call (GeoNames, Frankfurter) is relayed through it rather than
// called directly, per the third-microservice design in §18. Returns empty/
// no-op results rather than throwing on any failure, so a refresh run can
// record a partial failure for one master instead of crashing entirely —
// but every failure is logged at WARN with the actual exception, never
// swallowed silently. A common real cause in a multi-container setup: this
// service's `INTEGRATION_SERVICE_URL` still pointing at `localhost`, which
// inside a container means "this container," not the integration-service
// container — it needs to be that service's Docker Compose hostname instead.
@Component
public class IntegrationServiceClient implements ExternalMasterDataPort {
    private static final Logger log = LoggerFactory.getLogger(IntegrationServiceClient.class);

    private final String baseUrl;
    private final RestClient restClient = RestClient.create();

    public IntegrationServiceClient(IntegrationProperties properties) {
        this.baseUrl = properties.integration().baseUrl();
    }

    @Override
    public Optional<CityLookup> findCity(String name) {
        try {
            var results = restClient.get()
                .uri(baseUrl + "/api/integration/v1/masters/geonames/search?q={q}", name)
                .retrieve()
                .body(GeoNamesEntry[].class);

            if (results == null || results.length == 0) return Optional.empty();
            var best = results[0];
            return Optional.of(new CityLookup(
                best.name(), best.countryCode(), parseOrNull(best.lat()), parseOrNull(best.lng())));
        } catch (Exception ex) {
            log.warn("GeoNames lookup via integration-service failed for '{}' (baseUrl={}): {}",
                name, baseUrl, ex.toString());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Double> fetchExchangeRates(String base, List<String> symbols) {
        try {
            var response = restClient.get()
                .uri(baseUrl + "/api/integration/v1/masters/frankfurter/latest?base={base}&symbols={symbols}",
                    base, String.join(",", symbols))
                .retrieve()
                .body(FrankfurterRates.class);

            return response == null || response.rates() == null ? Map.of() : response.rates();
        } catch (Exception ex) {
            log.warn("Frankfurter lookup via integration-service failed for base={} symbols={} (baseUrl={}): {}",
                base, symbols, baseUrl, ex.toString());
            return Map.of();
        }
    }

    private static Double parseOrNull(String value) {
        try {
            return value == null ? null : Double.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Local copies of integration-service's response shapes — two services,
    // no shared library, same pattern already used for GeoNamesEntry
    // elsewhere in this codebase.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoNamesEntry(String name, String countryCode, String lat, String lng) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FrankfurterRates(String base, Map<String, Double> rates) {
        private FrankfurterRates {
            rates = rates == null ? new HashMap<>() : rates;
        }
    }
}
