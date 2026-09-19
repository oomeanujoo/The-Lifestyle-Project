package com.thelifestyle.travel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds `app.routing.*` — the real road-routing provider (§21 phase
// road-route) and the deterministic fuel-cost inputs, all env-var-backed,
// never hardcoded Java (§10). `enabled` defaults false: OSRM's public demo
// server has no availability guarantee and no authentication, so calling
// it is opt-in, not a default network call every route-recommendation
// request now makes.
@ConfigurationProperties(prefix = "app.routing")
public record RoutingProperties(Osrm osrm, FuelCost fuelCost) {

    public record Osrm(boolean enabled, String baseUrl) {}

    // Vehicle efficiency and fuel price are explicit, configured demo
    // inputs — never fetched live (no verified free fuel-price provider
    // exists), and never AI-estimated (§10 — AI never calculates a cost).
    public record FuelCost(double fuelPricePerLitre, double vehicleKmPerLitre, String currency) {}
}
