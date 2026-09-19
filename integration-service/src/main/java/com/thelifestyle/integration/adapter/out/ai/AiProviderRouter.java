package com.thelifestyle.integration.adapter.out.ai;

import com.thelifestyle.integration.adapter.out.resilience.ResilienceConfig;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.application.port.out.AiProvider;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Tries each configured AI provider in priority order — Groq, then
// Mistral, then Ollama last — skipping any with no configuration, and
// falling through to the next on a rate-limit trip, an open circuit
// breaker, or any call failure. Reordered 2026-09-19: Ollama used to be
// tried first (free/local/no quota), but live-tested response times put
// it well behind both cloud providers on this hardware — a local model
// answering slowly is worse for the fallback chain's purpose (get a fast
// answer) than a cloud call that's merely not free-forever. Groq is
// first specifically because it measured consistently faster than
// Mistral here (~0.3s vs ~0.48s average over three real calls each,
// 2026-09-19) — Ollama moves to last-resort: free and private, but only
// tried once both cloud options have failed or aren't configured. Same
// fallback-chain idea as the diagram in TECHNICAL_ARCHITECTURE.md §18.
// Rate limiting, circuit breaking, and health tracking all happen inside
// ResilienceGuard, not here — this class only decides the priority order
// and which config belongs to which provider.
@Component
public class AiProviderRouter {
    private final List<AiProvider> providers;
    private final OllamaAiProvider ollama;
    private final GroqAiProvider groq;
    private final Map<String, ResilienceConfig> resilienceConfigs = new LinkedHashMap<>();
    private final ResilienceGuard resilienceGuard;

    public AiProviderRouter(OllamaAiProvider ollama, GroqAiProvider groq, MistralAiProvider mistral,
                            ExternalApisProperties properties, ResilienceGuard resilienceGuard) {
        this.providers = List.of(groq, mistral, ollama);
        this.ollama = ollama;
        this.groq = groq;
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
    // Uses the constructor-injected ollama/groq fields directly, not a
    // position in `providers` — that list's order is the fallback
    // priority (Groq, Mistral, Ollama) and is not the pair this method
    // needs, so indexing into it here would silently call the wrong
    // provider under the wrong name after any future reordering.
    public VerifiedSuggestion suggestPlacesVerified(String query, int maxSuggestions) {
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

    // Same fallback order/skip/circuit-breaker behavior as suggestPlaces(),
    // for a caller with its own arbitrary prompt (e.g.
    // MasterAcquisitionUseCase) instead of the fixed place-suggestion
    // template. `null` from a provider (not configured, or the call failed)
    // is treated as "try the next one," same as an empty suggestion list.
    public RoutedCompletion complete(String prompt) {
        for (var provider : providers) {
            if (!provider.isConfigured()) continue;

            var result = resilienceGuard.call(
                provider.name(), resilienceConfigs.get(provider.name()),
                () -> provider.complete(prompt), null);

            if (result != null && !result.isBlank()) return new RoutedCompletion(result, provider.name(), provider.model());
        }
        return new RoutedCompletion(null, null, null);
    }

    public record RoutedSuggestion(List<String> suggestions, String provider, String model) {}

    public record RoutedCompletion(String content, String provider, String model) {}

    public record VerifiedSuggestion(List<String> ollamaSuggestions, List<String> groqSuggestions, boolean providersAgree) {}
}
