package com.thelifestyle.property.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Binds `app.integration.*` and `app.masters.*` from application.yaml.
// `seedCities`/`trackedCurrencies` are plain comma-separated env-var-backed
// lists, not hardcoded Java — see MasterRefreshUseCase.
@ConfigurationProperties(prefix = "app")
public record IntegrationProperties(Integration integration, Masters masters) {

    public record Integration(String baseUrl) {}

    public record Masters(List<String> seedCities, String baseCurrency, List<String> trackedCurrencies) {}
}
