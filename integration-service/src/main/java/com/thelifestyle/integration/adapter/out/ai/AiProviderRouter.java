package com.thelifestyle.integration.adapter.out.ai;

import com.thelifestyle.integration.adapter.out.resilience.ResilienceConfig;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.application.port.out.AiProvider;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Tries each configured AI provider in priority order — Ollama, then Groq,
// then Mistral — skipping any with no configuration, and falling through
// to the next on a rate-limit trip, an open circuit breaker, or any call
// failure. Same fallback-chain idea as the diagram in
// TECHNICAL_ARCHITECTURE.md §18. Rate limiting, circuit breaking, and
// health tracking all happen inside ResilienceGuard, not here — this class
// only decides the priority order and which config belongs to which
// provider.
@Component
public class AiProviderRouter {
    private final List<AiProvider> providers;
    private final Map<String, ResilienceConfig> resilienceConfigs = new LinkedHashMap<>();
    private final ResilienceGuard resilienceGuard;

    public AiProviderRouter(OllamaAiProvider ollama, GroqAiProvider groq, MistralAiProvider mistral,
                            ExternalApisProperties properties, ResilienceGuard resilienceGuard) {
        this.providers = List.of(ollama, groq, mistral);
        this.resilienceGuard = resilienceGuard;

        var ollamaCfg = properties.ai().ollama();
        var groqCfg = properties.ai().groq();
        var mistralCfg = properties.ai().mistral();
        resilienceConfigs.put(ollama.name(), new ResilienceConfig(ollamaCfg.limitForPeriod(), ollamaCfg.refreshPeriodMs()));
        resilienceConfigs.put(groq.name(), new ResilienceConfig(groqCfg.limitForPeriod(), groqCfg.refreshPeriodMs()));
        resilienceConfigs.put(mistral.name(), new ResilienceConfig(mistralCfg.limitForPeriod(), mistralCfg.refreshPeriodMs()));
    }

    public boolean anyProviderConfigured() {
        return providers.stream().anyMatch(AiProvider::isConfigured);
    }

    public RoutedSuggestion suggestPlaces(String query, int maxSuggestions) {
        for (var provider : providers) {
            if (!provider.isConfigured()) continue;

            var result = resilienceGuard.call(
                provider.name(), resilienceConfigs.get(provider.name()),
                () -> provider.suggestPlaces(query, maxSuggestions), List.<String>of());

            if (!result.isEmpty()) return new RoutedSuggestion(result, provider.name(), provider.model());
        }
        return new RoutedSuggestion(List.of(), null, null);
    }

    // For a designated high-stakes draft only (§18's "does more AI mean more
    // accuracy" section) — calls Ollama and Groq in parallel and reports
    // whether they agree, instead of the normal single-answer fallback.
    // Deliberately excludes Mistral (its 1 req/sec ceiling is the
    // tightest of the three; spending it on routine cross-checks isn't
    // worth it) and is never called by the default suggestion path — see
    // PlaceSuggestionUseCase, which still uses suggestPlaces() above.
    public VerifiedSuggestion suggestPlacesVerified(String query, int maxSuggestions) {
        var ollama = providers.get(0);
        var groq = providers.get(1);

        var ollamaFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
            ollama.isConfigured()
                ? resilienceGuard.call(ollama.name(), resilienceConfigs.get(ollama.name()),
                    () -> ollama.suggestPlaces(query, maxSuggestions), List.<String>of())
                : List.<String>of());
        var groqFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
            groq.isConfigured()
                ? resilienceGuard.call(groq.name(), resilienceConfigs.get(groq.name()),
                    () -> groq.suggestPlaces(query, maxSuggestions), List.<String>of())
                : List.<String>of());

        var ollamaResult = ollamaFuture.join();
        var groqResult = groqFuture.join();
        var agree = !ollamaResult.isEmpty() && !groqResult.isEmpty()
            && ollamaResult.stream().anyMatch(groqResult::contains);

        return new VerifiedSuggestion(ollamaResult, groqResult, agree);
    }

    public record RoutedSuggestion(List<String> suggestions, String provider, String model) {}

    public record VerifiedSuggestion(List<String> ollamaSuggestions, List<String> groqSuggestions, boolean providersAgree) {}
}
