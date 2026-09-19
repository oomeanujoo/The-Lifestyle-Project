package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.LegPriceQuote;

import java.util.List;
import java.util.UUID;

// Create-and-list only — no update/delete method exists here at all,
// matching travel.leg_price_quote's own append-only trigger (§17.1/§17.2):
// a new price is always a new row, the previous one is never touched.
public interface LegPriceQuoteRepository {
    LegPriceQuote create(UUID tripLegId, UUID currencyId, double amount, Double durationHours,
                          String source, String sourceUrl, String confidence);
    List<LegPriceQuote> findByTripLeg(UUID tripLegId);
}
