package com.thelifestyle.travel.domain;

import java.util.UUID;

// A row from travel.leg_price_quote — append-only (§17.1/§17.2), the
// table's own trigger physically rejects UPDATE/DELETE. `confidence`
// mirrors the DB's own default ('UNVERIFIED') for a manually-entered
// price, since no free live fare source exists (§19) — never asserted as
// verified unless a human explicitly says so.
public record LegPriceQuote(UUID id, UUID tripLegId, UUID currencyId, double amount, Double durationHours,
                             String source, String sourceUrl, String confidence, String capturedAt) {}
