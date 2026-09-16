package com.thelifestyle.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds the `app.*` block in application.yaml, which itself only holds
// ${ENV_VAR} placeholders — actual key values live in each developer's own
// local environment, never in this file or in version control. See
// TECHNICAL_ARCHITECTURE.md §18/§22.
//
// Every leaf here also carries limitForPeriod/refreshPeriodMs — real,
// researched rate-limit ceilings per external API (see application.yaml's
// comments for the source of each number), fed into ResilienceGuard.
@ConfigurationProperties(prefix = "app")
public record ExternalApisProperties(GeoNames geonames, DataGovIn datagovin, Frankfurter frankfurter, Ai ai) {

    public record GeoNames(String username, String baseUrl, int limitForPeriod, long refreshPeriodMs) {}

    public record DataGovIn(String apiKey, String resourceId, String baseUrl, int limitForPeriod, long refreshPeriodMs) {}

    public record Frankfurter(String baseUrl, int limitForPeriod, long refreshPeriodMs) {}

    public record Ai(Local ollama, Provider groq, Provider mistral) {
        public record Local(String model, String baseUrl, int readTimeoutSeconds, int limitForPeriod, long refreshPeriodMs) {}
        public record Provider(String apiKey, String model, String baseUrl, int limitForPeriod, long refreshPeriodMs) {}
    }
}
