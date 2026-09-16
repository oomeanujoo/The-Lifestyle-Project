package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.adapter.out.ai.GroqAiProvider;
import com.thelifestyle.integration.adapter.out.ai.MistralAiProvider;
import com.thelifestyle.integration.adapter.out.ai.OllamaAiProvider;
import com.thelifestyle.integration.adapter.out.resilience.HealthStatusRegistry;
import com.thelifestyle.integration.application.port.out.AiProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Backs the Dashboard's AI-status widget (§20) — health is read straight
// from HealthStatusRegistry, which every real AiProviderRouter call updates
// as a side effect (§18). This is a config+state read, not a live ping: it
// never itself calls Ollama/Groq/Mistral, so it costs no quota and returns
// instantly regardless of whether a provider is actually reachable right now.
@RestController
@RequestMapping("/api/integration/v1")
public class AiProviderStatusController {
    private final List<AiProvider> providers;
    private final HealthStatusRegistry healthStatusRegistry;

    public AiProviderStatusController(OllamaAiProvider ollama, GroqAiProvider groq, MistralAiProvider mistral,
                                      HealthStatusRegistry healthStatusRegistry) {
        this.providers = List.of(ollama, groq, mistral);
        this.healthStatusRegistry = healthStatusRegistry;
    }

    @GetMapping("/ai/providers/status")
    public List<AiProviderStatusResponse> status() {
        return providers.stream()
            .map(provider -> {
                var health = healthStatusRegistry.get(provider.name());
                return new AiProviderStatusResponse(
                    provider.name(),
                    provider.isConfigured(),
                    health.status().name(),
                    health.lastCheckedAt() == null ? null : health.lastCheckedAt().toString(),
                    health.lastError());
            })
            .toList();
    }
}
