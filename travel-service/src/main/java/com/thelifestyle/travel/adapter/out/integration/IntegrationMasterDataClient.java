package com.thelifestyle.travel.adapter.out.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.travel.config.IntegrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// The only class in this service that talks to integration-service's
// master-data read API (§17/§20) — reads city/currency/transport-mode
// masters, and staged Indian postal-code data, from lifestyle_master; never
// writes there (this service has no grant to write lifestyle_master anyway
// — see scripts/lifestyle-master-schema.sql). Returns Optional.empty()/an
// empty list rather than throwing on any failure, since a read-side lookup
// failing shouldn't crash whatever domain operation triggered it — that
// caller decides what "couldn't resolve this master" means for it.
@Component
public class IntegrationMasterDataClient implements LifestyleMasterReadPort {
    private static final Logger log = LoggerFactory.getLogger(IntegrationMasterDataClient.class);

    private final String baseUrl;
    private final RestClient restClient;

    public IntegrationMasterDataClient(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.baseUrl = properties.integration().baseUrl();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public Optional<CityMasterView> findCity(String name) {
        try {
            var results = restClient.get()
                .uri(baseUrl + "/api/integration/v1/masters/lifestyle/cities?name={name}", name)
                .retrieve()
                .body(CityMasterResponse[].class);

            if (results == null || results.length == 0) return Optional.empty();
            return Optional.of(toView(results[0]));
        } catch (Exception ex) {
            log.warn("City lookup via integration-service failed for '{}' (baseUrl={}): {}", name, baseUrl, ex.toString());
            return Optional.empty();
        }
    }

    // Full city list — for the route-recommendation endpoint's via-city
    // candidate ranking, which needs every known city, not one lookup.
    @Override
    public List<CityMasterView> findAllCities() {
        try {
            var results = restClient.get()
                .uri(baseUrl + "/api/integration/v1/masters/lifestyle/cities")
                .retrieve()
                .body(CityMasterResponse[].class);

            if (results == null) return List.of();
            return List.of(results).stream().map(this::toView).toList();
        } catch (Exception ex) {
            log.warn("City list via integration-service failed (baseUrl={}): {}", baseUrl, ex.toString());
            return List.of();
        }
    }

    @Override
    public Optional<CurrencyMasterView> findCurrency(String isoCode) {
        try {
            var result = restClient.get()
                .uri(baseUrl + "/api/integration/v1/masters/lifestyle/currencies/{isoCode}", isoCode)
                .retrieve()
                .body(CurrencyMasterResponse.class);

            if (result == null) return Optional.empty();
            return Optional.of(new CurrencyMasterView(UUID.fromString(result.id()), result.isoCode()));
        } catch (Exception ex) {
            log.warn("Currency lookup via integration-service failed for '{}' (baseUrl={}): {}", isoCode, baseUrl, ex.toString());
            return Optional.empty();
        }
    }

    @Override
    public Optional<TransportModeView> findTransportMode(String code) {
        try {
            var results = restClient.get()
                .uri(baseUrl + "/api/integration/v1/masters/lifestyle/transport-modes")
                .retrieve()
                .body(CodedMasterResponse[].class);

            if (results == null) return Optional.empty();
            return List.of(results).stream()
                .filter(r -> r.code().equalsIgnoreCase(code))
                .findFirst()
                .map(r -> new TransportModeView(UUID.fromString(r.id()), r.code(), r.label()));
        } catch (Exception ex) {
            log.warn("Transport mode lookup via integration-service failed for '{}' (baseUrl={}): {}", code, baseUrl, ex.toString());
            return Optional.empty();
        }
    }

    // For the route-recommendation endpoint's "via pincode" resolution —
    // 404 (pincode not staged) and any other failure both resolve to
    // Optional.empty(), same pattern as every other lookup here.
    @Override
    public Optional<PincodeView> findPincode(String code) {
        try {
            var result = restClient.get()
                .uri(baseUrl + "/api/integration/v1/masters/lifestyle/pincodes/{code}", code)
                .retrieve()
                .body(PincodeMasterResponse.class);

            if (result == null) return Optional.empty();
            return Optional.of(new PincodeView(UUID.fromString(result.cityId()), result.cityName(), result.countryCode(),
                result.pincode(), result.placeName(), result.latitude(), result.longitude(), result.source(), result.capturedAt()));
        } catch (Exception ex) {
            log.warn("Pincode lookup via integration-service failed for '{}' (baseUrl={}): {}", code, baseUrl, ex.toString());
            return Optional.empty();
        }
    }

    private CityMasterView toView(CityMasterResponse r) {
        return new CityMasterView(UUID.fromString(r.id()), r.name(), r.countryCode(), r.latitude(), r.longitude(), r.source(), r.updatedAt());
    }

    // Local copies of integration-service's response shapes — separate
    // services, no shared library, same pattern already used elsewhere.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CityMasterResponse(String id, String name, String countryCode, Double latitude, Double longitude, String source, String updatedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CurrencyMasterResponse(String id, String isoCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CodedMasterResponse(String id, String code, String label) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PincodeMasterResponse(String cityId, String cityName, String countryCode, String pincode,
                                          String placeName, Double latitude, Double longitude, String source, String capturedAt) {}
}
