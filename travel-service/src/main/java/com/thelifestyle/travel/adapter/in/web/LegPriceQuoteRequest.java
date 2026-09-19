package com.thelifestyle.travel.adapter.in.web;

// currency is a name/code, resolved server-side against the refreshed
// lifestyle_master.currency masters — never a raw id. confidence is
// optional; defaults to UNVERIFIED (manual entry, no live fare source
// exists, §17/§19) if not given.
public record LegPriceQuoteRequest(String currency, double amount, Double durationHours,
                                    String source, String sourceUrl, String confidence) {}
