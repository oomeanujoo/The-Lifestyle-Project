package com.thelifestyle.travel.domain;

// Same result-or-error pattern as TripLegOutcome/RouteRecommendationOutcome.
public record LegPriceQuoteOutcome(LegPriceQuote quote, String error) {
    public static LegPriceQuoteOutcome ok(LegPriceQuote quote) {
        return new LegPriceQuoteOutcome(quote, null);
    }

    public static LegPriceQuoteOutcome error(String error) {
        return new LegPriceQuoteOutcome(null, error);
    }
}
