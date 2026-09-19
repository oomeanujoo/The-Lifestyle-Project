package com.thelifestyle.integration.adapter.in.web;

// `id` is the stable lifestyle_master.currency UUID (as a string) — the
// value Travel/Property must persist as the FK target for any currency
// reference, never a locally-generated one. Separate from the bare
// List<String> /currencies endpoint, whose shape the UI depends on.
public record CurrencyMasterResponse(String id, String isoCode) {}
