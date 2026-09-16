package com.thelifestyle.integration.adapter.out.frankfurter;

import com.thelifestyle.integration.adapter.out.resilience.ResilienceConfig;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Frankfurter needs no signup or key at all (§22.1) — genuinely free,
// keyless currency exchange rates. Used by travel-service/property-service
// to refresh their own `currency` master (§17). Rate-limited via
// ResilienceGuard using a self-imposed courtesy cap (Frankfurter itself
// publishes no hard quota) — returns null on any failure/rejection rather
// than throwing, so a refresh run can record it as a partial failure
// instead of crashing.
@Component
public class FrankfurterClient {
    private static final String NAME = "frankfurter";

    private final ExternalApisProperties.Frankfurter config;
    private final ResilienceGuard resilienceGuard;
    private final RestClient restClient = RestClient.create();

    public FrankfurterClient(ExternalApisProperties properties, ResilienceGuard resilienceGuard) {
        this.config = properties.frankfurter();
        this.resilienceGuard = resilienceGuard;
    }

    public FrankfurterRates latest(String base, String symbols) {
        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.call(NAME, resilienceConfig, () ->
            restClient.get()
                .uri(config.baseUrl() + "/latest?base={base}&symbols={symbols}", base, symbols)
                .retrieve()
                .body(FrankfurterRates.class),
            null);
    }
}
