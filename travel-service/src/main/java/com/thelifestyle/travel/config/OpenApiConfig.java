package com.thelifestyle.travel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI travelServiceApi() {
        return new OpenAPI().info(new Info()
            .title("Travel Service API")
            .description("Travel planning bounded context: trips, destinations, itineraries and routes. Local-first, stateless, no authentication in Phase 1.")
            .version("0.1.0"));
    }
}
