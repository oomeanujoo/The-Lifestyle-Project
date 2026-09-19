package com.thelifestyle.integration.adapter.out.frankfurter;

import com.thelifestyle.integration.adapter.out.resilience.ExternalProviderException;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceConfig;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Frankfurter needs no signup or key at all (§22.1) — genuinely free,
// keyless currency exchange rates. Used to refresh the shared
// lifestyle_master.currency master (§17). Rate-limited via ResilienceGuard
// using a self-imposed courtesy cap (Frankfurter itself publishes no hard
// quota). A genuine failure (network error, malformed/empty response) now
// throws ExternalProviderException via callOrThrow, rather than silently
// returning null — a null return used to look identical to "nothing to
// report," which a refresh job could easily mistake for success.
// Frankfurter's own documented behavior for an unsupported currency code
// (e.g. AED) is to silently omit it from `rates` on an otherwise-200
// response — that's a real partial result, not a failure, and callers
// should keep treating it as such rather than expecting this class to
// throw for it.
@Component
public class FrankfurterClient {
    private static final String NAME = "frankfurter";

    private final ExternalApisProperties.Frankfurter config;
    private final ResilienceGuard resilienceGuard;
    private final RestClient restClient;

    public FrankfurterClient(ExternalApisProperties properties, ResilienceGuard resilienceGuard, RestClient.Builder restClientBuilder) {
        this.config = properties.frankfurter();
        this.resilienceGuard = resilienceGuard;
        // Built from the injected, Spring-autoconfigured Builder rather
        // than RestClient.create() directly, so tests can bind a
        // MockRestServiceServer to it instead of hitting the real network.
        this.restClient = restClientBuilder.build();
    }

    // Deliberately no `symbols` parameter — omitting it is Frankfurter's own
    // documented way to return every currency it supports against `base`,
    // not just a pre-configured shortlist. Importing "all valid currencies"
    // means asking for exactly that, not filtering client-side afterward.
    public FrankfurterRates latest(String base) {
        var resilienceConfig = new ResilienceConfig(config.limitForPeriod(), config.refreshPeriodMs());
        return resilienceGuard.callOrThrow(NAME, resilienceConfig, () -> {
            var response = restClient.get()
                .uri(config.baseUrl() + "/latest?base={base}", base)
                .retrieve()
                .body(FrankfurterRates.class);

            if (response == null || response.rates() == null) {
                throw new ExternalProviderException(NAME, "empty or malformed response body");
            }
            return response;
        });
    }
}
