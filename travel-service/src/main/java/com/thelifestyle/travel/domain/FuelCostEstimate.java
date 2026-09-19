package com.thelifestyle.travel.domain;

// A deterministic, clearly-labeled fuel-cost estimate for a road route —
// computed in plain Java from explicit inputs (never AI, §10: AI explains
// sourced results, it never calculates them). Deliberately excludes tolls,
// parking, and live fuel prices — those need a verified provider this
// service doesn't have; `label` says exactly that so a caller can never
// mistake this for a real, total trip cost.
public record FuelCostEstimate(
    double amount, String currency, double fuelPricePerLitre, double vehicleKmPerLitre, String label) {

    private static final String DEFAULT_LABEL =
        "Fuel-cost estimate only — excludes tolls, parking, and live fuel prices; not a total trip cost.";

    public static FuelCostEstimate compute(double distanceKm, double fuelPricePerLitre, double vehicleKmPerLitre, String currency) {
        var litresNeeded = distanceKm / vehicleKmPerLitre;
        var amount = Math.round(litresNeeded * fuelPricePerLitre * 100.0) / 100.0;
        return new FuelCostEstimate(amount, currency, fuelPricePerLitre, vehicleKmPerLitre, DEFAULT_LABEL);
    }
}
