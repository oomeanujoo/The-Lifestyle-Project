package com.thelifestyle.property.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI propertyServiceApi() {
        return new OpenAPI().info(new Info()
            .title("Property Service API")
            .description("Property and cost-of-living planning bounded context: city comparisons, listings and monthly costs. Local-first, stateless, no authentication in Phase 1.")
            .version("0.1.0"));
    }
}
