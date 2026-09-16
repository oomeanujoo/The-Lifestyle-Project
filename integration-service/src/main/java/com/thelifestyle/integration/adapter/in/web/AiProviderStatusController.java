package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.adapter.out.ai.GroqAiProvider;
import com.thelifestyle.integration.adapter.out.ai.MistralAiProvider;
import com.thelifestyle.integration.adapter.out.ai.OllamaAiProvider;
import com.thelifestyle.integration.application.port.out.AiProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// A config check, not a live health ping — calling each provider's API just
// to report "configured" would cost real free-tier quota for no benefit.
// "Configured" means an API key is present; it does not confirm the key is
// still valid or that the provider is currently reachable.
@RestController
@RequestMapping("/api/integration/v1")
public class AiProviderStatusController {
    private final List<AiProvider> providers;

    public AiProviderStatusController(OllamaAiProvider ollama, GroqAiProvider groq, MistralAiProvider mistral) {
        this.providers = List.of(ollama, groq, mistral);
    }

    @GetMapping("/ai/providers/status")
    public List<AiProviderStatusResponse> status() {
        return providers.stream()
            .map(provider -> new AiProviderStatusResponse(provider.name(), provider.isConfigured()))
            .toList();
    }
}
