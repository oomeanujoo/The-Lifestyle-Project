package com.thelifestyle.integration.adapter.out.datagovin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceConfig;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Thin client over a single data.gov.in dataset resource (the one saved
// during account setup — see TECHNICAL_ARCHITECTURE.md §22.2). Records come
// back as a generic Map, not a typed DTO, because this dataset's exact
// column schema hasn't been inspected against a live response on this
// machine yet — typing it precisely is a follow-up once this has actually
// been called once with a real key. Rate-limited via ResilienceGuard using
// a conservative, explicitly UNVERIFIED cap — data.gov.in publishes no
// numeric rate limit anywhere found (§17.3/§18/§22).
@Component
public class DataGovInClient {
    private static final String NAME = "datagovin";
    private static final Logger log = LoggerFactory.getLogger(DataGovInClient.class);

    private final ExternalApisProperties.DataGovIn config;
    private final ResilienceGuard resilienceGuard;
    private final RestClient restClient = RestClient.create();

    public DataGovInClient(ExternalApisProperties properties, ResilienceGuard resilienceGuard) {
        this.config = properties.datagovin();
        this.resilienceGuard = resilienceGuard;
    }

    public boolean isConfigured() {
        return config.apiKey() != null && !config.apiKey().isBlank();
    }

    public List<Map<String, Object>> fetchSample(int limit) {
        if (!isConfigured()) {
            log.info("[{}] skipped — DATA_GOV_IN_API_KEY not set", NAME);
            return List.of();
        }

        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.call(NAME, resilienceConfig, () -> {
            var response = restClient.get()
                .uri(config.baseUrl() + "/resource/{resourceId}?api-key={key}&format=json&limit={limit}",
                    config.resourceId(), config.apiKey(), limit)
                .retrieve()
                .body(DataGovInResponse.class);

            return response == null || response.records() == null ? List.<Map<String, Object>>of() : response.records();
        }, List.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DataGovInResponse(List<Map<String, Object>> records) {}
}
