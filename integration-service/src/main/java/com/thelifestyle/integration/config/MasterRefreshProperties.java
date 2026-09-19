package com.thelifestyle.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Binds `app.masters.*` — which cities this service's own refresh looks
// up by name, plus the bounds for its bulk Indian-city discovery pass
// (never hardcoded Java, §10 — see application.yaml for the actual
// values). `trackedCurrencies` was removed: currency refresh now imports
// every currency Frankfurter returns for `baseCurrency`, not a
// pre-configured shortlist.
@ConfigurationProperties(prefix = "app.masters")
public record MasterRefreshProperties(
    List<String> seedCities,
    String baseCurrency,
    int indiaCityTargetCount,
    int indiaCityPageSize,
    int indiaCityMaxPages,
    int indiaPincodeCityLimit) {}
