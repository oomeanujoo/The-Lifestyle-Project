package com.thelifestyle.integration.adapter.out.ai;

import com.thelifestyle.integration.config.ExternalApisProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Same scope and reasoning as GroqAiProviderTest — isConfigured()/skip
// behavior only, no real HTTP call, no credential (real or fake) ever
// logged or asserted on.
class MistralAiProviderTest {

    private static ExternalApisProperties.Ai.Provider providerConfig(String apiKey) {
        return new ExternalApisProperties.Ai.Provider(apiKey, "test-model", "http://fake-mistral.test", 1, 1200);
    }

    private static ExternalApisProperties propertiesWithMistral(ExternalApisProperties.Ai.Provider mistral) {
        var unconfiguredProvider = new ExternalApisProperties.Ai.Provider("", "", "", 25, 60000);
        var unconfiguredLocal = new ExternalApisProperties.Ai.Local("", "", 720, 60, 60000);
        return new ExternalApisProperties(
            new ExternalApisProperties.GeoNames("", "http://fake-geonames.test", 10, 60000),
            new ExternalApisProperties.DataGovIn("", "5c2f62fe-5afa-4119-a499-fec9d604d5bd", "https://api.data.gov.in", 10, 60000),
            new ExternalApisProperties.Frankfurter("https://api.frankfurter.dev/v1", 20, 60000),
            new ExternalApisProperties.Ai(unconfiguredLocal, unconfiguredProvider, mistral));
    }

    @Test void isNotConfiguredWithoutApiKey() {
        var provider = new MistralAiProvider(propertiesWithMistral(providerConfig("")));

        assertFalse(provider.isConfigured());
        assertEquals("mistral", provider.name());
    }

    @Test void suggestPlacesReturnsEmptyWithoutCallingOutWhenUnconfigured() {
        var provider = new MistralAiProvider(propertiesWithMistral(providerConfig("")));

        var result = provider.suggestPlaces("Dubai", 3);

        assertTrue(result.isEmpty());
    }

    @Test void isConfiguredWhenApiKeyPresent() {
        var provider = new MistralAiProvider(propertiesWithMistral(providerConfig("test-only-placeholder")));

        assertTrue(provider.isConfigured());
        assertEquals("test-model", provider.model());
    }
}
