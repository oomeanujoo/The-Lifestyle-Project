package com.thelifestyle.property.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds `app.integration.base-url` — where to reach integration-service,
// the single owner of lifestyle_master (§17/§20). This service no longer
// owns any master-refresh config of its own (seed cities, tracked
// currencies, etc.) — that config moved to integration-service along with
// the refresh logic itself.
@ConfigurationProperties(prefix = "app")
public record IntegrationProperties(Integration integration) {

    public record Integration(String baseUrl) {}
}
