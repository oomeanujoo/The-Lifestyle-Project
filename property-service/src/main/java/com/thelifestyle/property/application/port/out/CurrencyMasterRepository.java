package com.thelifestyle.property.application.port.out;

import java.util.List;

// Upsert-and-append only (§17.1) — keyed by iso_code, already UNIQUE since
// V2. Every upsert records rate_captured_at, so an old rate is never lost,
// only superseded — the price-history pattern in §17.2 applied to currency.
public interface CurrencyMasterRepository {
    void upsert(String isoCode, Double exchangeRateToBase);
    long count();
    List<String> findAllIsoCodes();
}
