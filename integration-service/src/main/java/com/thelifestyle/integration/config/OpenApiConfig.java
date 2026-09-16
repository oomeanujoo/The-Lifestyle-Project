package com.thelifestyle.integration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI integrationServiceApi() {
        return new OpenAPI().info(new Info()
            .title("Integration Service API")
            .description("External API integration layer: AI provider fallback (Groq/Mistral) and external master-data sources (GeoNames, data.gov.in). Owns no data of its own — stateless relay, no authentication in Phase 1.")
            .version("0.1.0"));
    }
}
