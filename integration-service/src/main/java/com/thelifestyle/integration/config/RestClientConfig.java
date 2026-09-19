package com.thelifestyle.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Spring Boot 4's modularized autoconfigure no longer registers a
// RestClient.Builder bean on its own (RestClientAutoConfiguration was
// removed after 3.x) — every RestClient-based adapter here (GeoNamesClient,
// FrankfurterClient, DataGovInClient, the AI provider clients) needs one
// injected, so it's declared explicitly instead of relying on
// autoconfiguration that no longer exists.
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
