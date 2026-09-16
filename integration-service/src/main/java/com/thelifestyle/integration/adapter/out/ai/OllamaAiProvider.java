package com.thelifestyle.integration.adapter.out.ai;

import com.thelifestyle.integration.application.port.out.AiProvider;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class OllamaAiProvider implements AiProvider {
    private final ExternalApisProperties.Ai.Local config;
    private final RestClient client;

    public OllamaAiProvider(ExternalApisProperties properties) {
        this.config = properties.ai().ollama();
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(config.readTimeoutSeconds()));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override public String name() { return "ollama"; }
    @Override public String model() { return config.model(); }
    @Override public boolean isConfigured() {
        return config.model() != null && !config.model().isBlank()
            && config.baseUrl() != null && !config.baseUrl().isBlank();
    }

    @Override
    public List<String> suggestPlaces(String query, int maxSuggestions) {
        if (!isConfigured()) return List.of();
        var prompt = "Suggest up to %d place names related to the user's search: %s. "
            .formatted(maxSuggestions, query)
            + "Return only a comma-separated list. These are unverified drafts.";
        var response = client.post()
            .uri(config.baseUrl() + "/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChatRequest(config.model(), false, false,
                List.of(new ChatMessage("user", prompt)),
                Map.of("num_ctx", 2048, "num_predict", 96)))
            .retrieve()
            .body(ChatResponse.class);
        if (response == null || response.message() == null || response.message().content() == null) {
            return List.of();
        }
        return Arrays.stream(response.message().content().split(","))
            .map(String::trim)
            .filter(place -> !place.isEmpty())
            .limit(maxSuggestions)
            .toList();
    }

    record ChatMessage(String role, String content) {}
    record ChatRequest(String model, boolean stream, boolean think,
                       List<ChatMessage> messages, Map<String, Integer> options) {}
    record ChatResponse(ChatMessage message) {}
}
