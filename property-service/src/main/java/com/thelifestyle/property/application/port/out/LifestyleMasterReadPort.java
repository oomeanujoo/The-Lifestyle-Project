package com.thelifestyle.property.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// This service reads city/currency/bhk-type/service-addon masters through
// integration-service's API (§17/§20) — it no longer refreshes or writes
// any of them itself. Every view carries the canonical lifestyle_master
// `id` — LocalMasterIdResolver mirrors a domain record's FK target using
// that SAME id, never a locally-generated one, so the local row shares
// identity with lifestyle_master instead of diverging from it.
// `source`/`updatedAt` on CityMasterView are the honest provenance and
// freshness the place-search feature reports back to its caller.
public interface LifestyleMasterReadPort {
    Optional<CityMasterView> findCity(String name);
    List<CityMasterView> findAllCities();
    Optional<CurrencyMasterView> findCurrency(String isoCode);
    Optional<CodedMasterView> findBhkType(String code);
    Optional<CodedMasterView> findServiceAddon(String code);

    record CityMasterView(UUID id, String name, String countryCode, Double latitude, Double longitude, String source, String updatedAt) {}
    record CurrencyMasterView(UUID id, String isoCode) {}
    record CodedMasterView(UUID id, String code, String label) {}
}
