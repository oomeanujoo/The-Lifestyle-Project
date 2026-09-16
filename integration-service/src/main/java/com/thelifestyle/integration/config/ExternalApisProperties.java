package com.thelifestyle.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds the `app.*` block in application.yaml, which itself only holds
// ${ENV_VAR} placeholders — actual key values live in each developer's own
// local environment, never in this file or in version control. See
// TECHNICAL_ARCHITECTURE.md §18/§22.
@ConfigurationProperties(prefix = "app")
public record ExternalApisProperties(GeoNames geonames, DataGovIn datagovin, Ai ai) {

    public record GeoNames(String username, String baseUrl) {}

    public record DataGovIn(String apiKey, String resourceId, String baseUrl) {}

    public record Ai(Local ollama, Provider groq, Provider mistral, RateLimit rateLimit) {
        public record Local(String model, String baseUrl) {}
        public record Provider(String apiKey, String model, String baseUrl) {}
        public record RateLimit(int requestsPerMinute) {}
    }
}
