package com.thelifestyle.integration.application.port.out;

import com.thelifestyle.integration.domain.CurrencyMaster;

import java.util.List;
import java.util.Optional;

// Upsert-and-append only (§17.1) — keyed by iso_code, already UNIQUE in
// lifestyle_master.currency. Every upsert records rate_captured_at, so an
// old rate is never lost, only superseded (§17.2 applied to currency).
public interface LifestyleMasterCurrencyRepository {
    void upsert(String isoCode, Double exchangeRateToBase);
    List<String> findAllIsoCodes();
    // For Travel/Property's ID-mapping resolver — the stable UUID a
    // currency FK must use, not just the code the UI-facing list returns.
    Optional<CurrencyMaster> findByIsoCode(String isoCode);
    long count();
}
