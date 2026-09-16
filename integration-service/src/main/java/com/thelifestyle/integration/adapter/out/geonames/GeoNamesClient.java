package com.thelifestyle.integration.adapter.out.geonames;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Component
public class GeoNamesClient {
    private static final String NAME = "geonames";
    private static final Logger log = LoggerFactory.getLogger(GeoNamesClient.class);

    private final ExternalApisProperties.GeoNames config;
    private final ResilienceGuard resilienceGuard;
    private final RestClient restClient = RestClient.create();

    public GeoNamesClient(ExternalApisProperties properties, ResilienceGuard resilienceGuard) {
        this.config = properties.geonames();
        this.resilienceGuard = resilienceGuard;
    }

    public boolean isConfigured() {
        return config.username() != null && !config.username().isBlank();
    }

    public List<GeoNamesEntry> search(String query, int maxRows) {
        if (!isConfigured()) {
            log.info("[{}] skipped — GEONAMES_USERNAME not set", NAME);
            return List.of();
        }

        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.call(NAME, resilienceConfig, () -> {
            var response = restClient.get()
                .uri(config.baseUrl() + "/searchJSON?q={q}&maxRows={maxRows}&username={username}",
                    query, maxRows, config.username())
                .retrieve()
                .body(GeoNamesSearchResponse.class);

            return response == null || response.geonames() == null ? List.<GeoNamesEntry>of() : response.geonames();
        }, List.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoNamesSearchResponse(List<GeoNamesEntry> geonames) {}
}
