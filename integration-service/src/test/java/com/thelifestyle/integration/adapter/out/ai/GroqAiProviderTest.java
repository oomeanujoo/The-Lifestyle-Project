package com.thelifestyle.integration.adapter.out.ai;

import com.thelifestyle.integration.config.ExternalApisProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Deliberately scoped to isConfigured()/skip behavior only — never makes a
// real HTTP call, so there's no risk of hitting Groq's live API or of any
// credential (real or fake) ever reaching a log line or an assertion
// message. Any string used as a "key" below is an obvious placeholder,
// never a value that could be mistaken for a real one.
class GroqAiProviderTest {

    private static ExternalApisProperties.Ai.Provider providerConfig(String apiKey) {
        return new ExternalApisProperties.Ai.Provider(apiKey, "test-model", "http://fake-groq.test", 25, 60000);
    }

    private static ExternalApisProperties propertiesWithGroq(ExternalApisProperties.Ai.Provider groq) {
        var unconfiguredProvider = new ExternalApisProperties.Ai.Provider("", "", "", 25, 60000);
        var unconfiguredLocal = new ExternalApisProperties.Ai.Local("", "", 720, 60, 60000);
        return new ExternalApisProperties(
            new ExternalApisProperties.GeoNames("", "http://fake-geonames.test", 10, 60000),
            new ExternalApisProperties.DataGovIn("", "5c2f62fe-5afa-4119-a499-fec9d604d5bd", "https://api.data.gov.in", 10, 60000),
            new ExternalApisProperties.Frankfurter("https://api.frankfurter.dev/v1", 20, 60000),
            new ExternalApisProperties.Ai(unconfiguredLocal, groq, unconfiguredProvider));
    }

    @Test void isNotConfiguredWithoutApiKey() {
        var provider = new GroqAiProvider(propertiesWithGroq(providerConfig("")));

        assertFalse(provider.isConfigured());
        assertEquals("groq", provider.name());
    }

    @Test void suggestPlacesReturnsEmptyWithoutCallingOutWhenUnconfigured() {
        var provider = new GroqAiProvider(propertiesWithGroq(providerConfig("")));

        // No network client is even reachable here (RestClient.create()
        // would fail fast against "fake-groq.test" if it were ever
        // actually invoked) — an empty result with no exception confirms
        // the unconfigured short-circuit fired before any HTTP attempt.
        var result = provider.suggestPlaces("Dubai", 3);

        assertTrue(result.isEmpty());
    }

    @Test void isConfiguredWhenApiKeyPresent() {
        // "test-only-placeholder" is an obviously fake value, never a real
        // credential — this only proves isConfigured()'s blank-check logic,
        // it does not trigger any network call.
        var provider = new GroqAiProvider(propertiesWithGroq(providerConfig("test-only-placeholder")));

        assertTrue(provider.isConfigured());
        assertEquals("test-model", provider.model());
    }
}
