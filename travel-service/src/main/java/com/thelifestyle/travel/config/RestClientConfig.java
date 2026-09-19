package com.thelifestyle.travel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Spring Boot 4's modularized autoconfigure no longer registers a
// RestClient.Builder bean on its own (RestClientAutoConfiguration was
// removed after 3.x) — every RestClient-based adapter here
// (IntegrationMasterDataClient) needs one injected, so it's declared
// explicitly instead of relying on autoconfiguration that no longer exists.
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
