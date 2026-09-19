package com.thelifestyle.integration.adapter.out.geonames;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thelifestyle.integration.adapter.out.resilience.ExternalProviderException;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceConfig;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Thin client over GeoNames' free searchJSON endpoint — the master source
// for city/state/country data designed in TECHNICAL_ARCHITECTURE.md §17.
// Genuinely rate-limited now (via ResilienceGuard, using GeoNames' own
// published 1,000-credits/hour ceiling, §17.3/§18/§22) — every call also
// updates this client's health status, read from the last real hit made.
//
// A genuine failure (bad auth, rate limit exceeded, malformed response) is
// now explicit — it throws ExternalProviderException via
// ResilienceGuard.callOrThrow, rather than silently degrading to an empty
// list the way a real "no matches" result also looks. Only isConfigured()
// short-circuits to an empty list, since "not configured" is a distinct,
// already-visible state (§18/§22), not a masked failure.
@Component
public class GeoNamesClient {
    private static final String NAME = "geonames";
    private static final Logger log = LoggerFactory.getLogger(GeoNamesClient.class);

    private final ExternalApisProperties.GeoNames config;
    private final ResilienceGuard resilienceGuard;
    private final RestClient restClient;

    public GeoNamesClient(ExternalApisProperties properties, ResilienceGuard resilienceGuard, RestClient.Builder restClientBuilder) {
        this.config = properties.geonames();
        this.resilienceGuard = resilienceGuard;
        // Built from the injected, Spring-autoconfigured Builder rather
        // than RestClient.create() directly, so tests can bind a
        // MockRestServiceServer to it instead of hitting the real network.
        this.restClient = restClientBuilder.build();
    }

    public boolean isConfigured() {
        return config.username() != null && !config.username().isBlank();
    }

    // Throws ExternalProviderException on any genuine failure — a caller
    // that wants to treat "GeoNames errored" the same as "not configured"
    // (i.e. degrade quietly) should catch it explicitly and decide that
    // for itself; this method does not make that call silently anymore.
    public List<GeoNamesEntry> search(String query, int maxRows) {
        if (!isConfigured()) {
            log.info("[{}] skipped — GEONAMES_USERNAME not set", NAME);
            return List.of();
        }

        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.callOrThrow(NAME, resilienceConfig, () -> {
            var response = restClient.get()
                .uri(config.baseUrl() + "/searchJSON?q={q}&maxRows={maxRows}&username={username}",
                    query, maxRows, config.username())
                .retrieve()
                .body(GeoNamesSearchResponse.class);

            if (response == null) {
                throw new ExternalProviderException(NAME, "empty response body");
            }
            if (response.status() != null) {
                // GeoNames' real error shape — HTTP 200 with a status
                // object instead of results. Auth failures, exceeded
                // quotas, and bad parameters all land here, never as an
                // HTTP 4xx/5xx that RestClient would already throw for.
                throw new ExternalProviderException(NAME,
                    "GeoNames returned status " + response.status().value() + ": " + response.status().message());
            }
            if (response.geonames() == null) {
                throw new ExternalProviderException(NAME, "response had neither results nor a status — unexpected shape");
            }
            return response.geonames();
        });
    }

    // Explicit missing-city lookup. GeoNames filters to populated places
    // and one country; the caller still checks exact name and coordinates
    // before writing anything to the authoritative city master.
    public List<GeoNamesEntry> searchPopulatedPlaces(String name, String countryCode, int maxRows) {
        if (!isConfigured()) {
            throw new ExternalProviderException(NAME, "GEONAMES_USERNAME is not configured");
        }
        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.callOrThrow(NAME, resilienceConfig, () -> {
            var response = restClient.get()
                .uri(config.baseUrl() + "/searchJSON?q={q}&country={country}&featureClass=P&maxRows={maxRows}&username={username}",
                    name, countryCode, maxRows, config.username())
                .retrieve()
                .body(GeoNamesSearchResponse.class);
            if (response == null || response.status() != null || response.geonames() == null) {
                throw new ExternalProviderException(NAME, response != null && response.status() != null
                    ? "GeoNames returned status " + response.status().value() + ": " + response.status().message()
                    : "unexpected or empty response");
            }
            return response.geonames();
        });
    }

    // Bulk city discovery — country + featureClass (P = populated place)
    // ordered by population, paged via startRow. This is what lets a
    // refresh grow real city coverage past a small hand-picked seed list
    // without inventing a single name: every row is still a real GeoNames
    // result, just discovered by "biggest places in this country" rather
    // than "look up this name I already guessed." Same rate-limiter/
    // circuit-breaker bucket as search() (same GeoNames account ceiling).
    public List<GeoNamesEntry> searchByCountry(String countryCode, int maxRows, int startRow) {
        if (!isConfigured()) {
            log.info("[{}] country search skipped — GEONAMES_USERNAME not set", NAME);
            return List.of();
        }

        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.callOrThrow(NAME, resilienceConfig, () -> {
            var response = restClient.get()
                .uri(config.baseUrl() + "/searchJSON?country={country}&featureClass=P&orderby=population"
                        + "&maxRows={maxRows}&startRow={startRow}&username={username}",
                    countryCode, maxRows, startRow, config.username())
                .retrieve()
                .body(GeoNamesSearchResponse.class);

            if (response == null) {
                throw new ExternalProviderException(NAME, "empty response body");
            }
            if (response.status() != null) {
                throw new ExternalProviderException(NAME,
                    "GeoNames returned status " + response.status().value() + ": " + response.status().message());
            }
            if (response.geonames() == null) {
                throw new ExternalProviderException(NAME, "response had neither results nor a status — unexpected shape");
            }
            return response.geonames();
        });
    }

    // Same free GeoNames account/quota as search() above — deliberately
    // shares the "geonames" rate-limiter/circuit-breaker bucket rather than
    // getting its own, since GeoNames enforces one ceiling per account
    // regardless of which endpoint is called. Same explicit-failure
    // contract: throws on a genuine error, never returns an empty list that
    // looks identical to "no postal codes found."
    public List<GeoNamesPostalCodeEntry> searchPostalCodes(String placeName, String countryCode, int maxRows) {
        if (!isConfigured()) {
            log.info("[{}] postal code search skipped — GEONAMES_USERNAME not set", NAME);
            return List.of();
        }

        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.callOrThrow(NAME, resilienceConfig, () -> {
            var response = restClient.get()
                .uri(config.baseUrl() + "/postalCodeSearchJSON?placename={placeName}&country={countryCode}&maxRows={maxRows}&username={username}",
                    placeName, countryCode, maxRows, config.username())
                .retrieve()
                .body(GeoNamesPostalCodeSearchResponse.class);

            if (response == null) {
                throw new ExternalProviderException(NAME, "empty response body");
            }
            if (response.status() != null) {
                throw new ExternalProviderException(NAME,
                    "GeoNames returned status " + response.status().value() + ": " + response.status().message());
            }
            if (response.postalCodes() == null) {
                throw new ExternalProviderException(NAME, "response had neither postal codes nor a status — unexpected shape");
            }
            return response.postalCodes();
        });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoNamesSearchResponse(List<GeoNamesEntry> geonames, GeoNamesStatus status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoNamesPostalCodeSearchResponse(List<GeoNamesPostalCodeEntry> postalCodes, GeoNamesStatus status) {}
}
