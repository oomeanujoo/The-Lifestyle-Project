package com.thelifestyle.integration.domain;

import java.util.UUID;

// A single lifestyle_master.currency row, by iso_code — separate from the
// bare `List<String>` the /currencies endpoint returns (that shape is
// consumed by the UI's currency selector and must not change). This is
// for Travel/Property's ID-mapping resolver, which needs the stable
// authoritative UUID a currency FK must use, not just its code.
public record CurrencyMaster(UUID id, String isoCode) {}
