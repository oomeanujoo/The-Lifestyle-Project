package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.ai.AiProviderRouter;
import com.thelifestyle.integration.adapter.out.ai.GroqAiProvider;
import com.thelifestyle.integration.adapter.out.ai.MistralAiProvider;
import com.thelifestyle.integration.adapter.out.ai.OllamaAiProvider;
import com.thelifestyle.integration.adapter.out.resilience.HealthStatusRegistry;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.application.port.out.AiSuggestionRepository;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceSuggestionUseCaseTest {

    // No API keys set anywhere in this unit test, so both cloud AI
    // providers are unconfigured and AiProviderRouter never actually calls
    // the network — it only proves the "no provider configured" path.
    private static PlaceSuggestionUseCase newUseCase() {
        var unconfiguredProvider = new ExternalApisProperties.Ai.Provider("", "", "", 25, 60000);
        var unconfiguredLocal = new ExternalApisProperties.Ai.Local("", "", 720, 60, 60000);
        var properties = new ExternalApisProperties(
            new ExternalApisProperties.GeoNames("", "http://api.geonames.org", 10, 60000),
            new ExternalApisProperties.DataGovIn("", "5c2f62fe-5afa-4119-a499-fec9d604d5bd", "https://api.data.gov.in", 10, 60000),
            new ExternalApisProperties.Frankfurter("https://api.frankfurter.dev/v1", 20, 60000),
            new ExternalApisProperties.Ai(unconfiguredLocal, unconfiguredProvider, unconfiguredProvider));
        var resilienceGuard = new ResilienceGuard(new HealthStatusRegistry());
        var router = new AiProviderRouter(new OllamaAiProvider(properties),
            new GroqAiProvider(properties), new MistralAiProvider(properties), properties, resilienceGuard);
        AiSuggestionRepository repository = (UUID id, String query, List<String> suggestions,
            String provider, String model, String promptVersion, Instant generatedAt) -> {
                throw new AssertionError("No suggestion should be saved in this test");
            };
        return new PlaceSuggestionUseCase(router, repository);
    }

    private final PlaceSuggestionUseCase useCase = newUseCase();

    @Test void returnsNoSuggestionsBelowMinimumQueryLength() {
        var result = useCase.suggest("P");
        assertTrue(result.suggestions().isEmpty());
        assertNull(result.disclaimer());
    }

    @Test void returnsNoProviderNoticeWhenNoneConfigured() {
        var result = useCase.suggest("Antarctica");
        assertTrue(result.suggestions().isEmpty());
        assertTrue(result.disclaimer() != null && result.disclaimer().contains("No AI provider"));
    }
}
