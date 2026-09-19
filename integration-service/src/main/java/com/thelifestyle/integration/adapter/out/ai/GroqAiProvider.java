package com.thelifestyle.integration.adapter.out.ai;

import com.thelifestyle.integration.application.port.out.AiProvider;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

// Calls Groq's OpenAI-compatible chat completions endpoint to draft plausible
// place-name suggestions — a DRAFT only, never treated as verified (§10).
// isConfigured() is a config check (is an API key set), not a live network
// call, so it costs nothing and can't fail from being offline.
@Component
public class GroqAiProvider implements AiProvider {
    private static final String PROMPT_TEMPLATE =
        "List up to %d real place names that could plausibly match or relate to '%s'. "
            + "Reply with ONLY a comma-separated list of place names, nothing else, no numbering, no explanation.";

    private final ExternalApisProperties.Ai.Provider config;
    private final RestClient restClient = RestClient.create();

    public GroqAiProvider(ExternalApisProperties properties) {
        this.config = properties.ai().groq();
    }

    @Override
    public String name() {
        return "groq";
    }

    @Override
    public String model() {
        return config.model();
    }

    @Override
    public boolean isConfigured() {
        return config.apiKey() != null && !config.apiKey().isBlank();
    }

    @Override
    public List<String> suggestPlaces(String query, int maxSuggestions) {
        if (!isConfigured()) return List.of();

        var prompt = PROMPT_TEMPLATE.formatted(maxSuggestions, query);
        var response = restClient.post()
            .uri(config.baseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + config.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChatRequest(config.model(), List.of(new ChatMessage("user", prompt))))
            .retrieve()
            .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) return List.of();
        var content = response.choices().get(0).message().content();
        if (content == null || content.isBlank()) return List.of();

        return Arrays.stream(content.split(","))
            .map(String::trim)
            .filter(place -> !place.isEmpty())
            .limit(maxSuggestions)
            .toList();
    }

    @Override
    public String complete(String prompt) {
        if (!isConfigured()) return null;

        var response = restClient.post()
            .uri(config.baseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + config.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChatRequest(config.model(), List.of(new ChatMessage("user", prompt))))
            .retrieve()
            .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) return null;
        return response.choices().get(0).message().content();
    }

    record ChatMessage(String role, String content) {}
    record ChatRequest(String model, List<ChatMessage> messages) {}
    record ChatChoice(ChatMessage message) {}
    record ChatResponse(List<ChatChoice> choices) {}
}
