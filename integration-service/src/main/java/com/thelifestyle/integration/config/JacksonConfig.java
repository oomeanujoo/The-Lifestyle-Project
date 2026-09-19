package com.thelifestyle.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Same root cause as RestClientConfig: Spring Boot 4's modularized
// autoconfigure doesn't register an ObjectMapper bean here even though
// JacksonAutoConfiguration exists on the classpath (spring-boot-jackson) —
// declared explicitly instead of relying on autoconfiguration that isn't
// activating. Needed by MasterAcquisitionUseCase/MasterProposalUseCase to
// parse AI responses and stored proposedData as untrusted JSON.
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
