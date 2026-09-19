package com.thelifestyle.travel.adapter.in.web;

public record LegPriceQuoteResponse(String id, String tripLegId, String currencyId, double amount,
                                     Double durationHours, String source, String sourceUrl,
                                     String confidence, String capturedAt) {}
