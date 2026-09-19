package com.thelifestyle.travel.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// This service reads city/currency/transport-mode masters through
// integration-service's API (§17/§20) — it no longer refreshes or writes
// any of them itself. Every view carries the canonical lifestyle_master
// `id` — LocalMasterIdResolver mirrors a domain record's FK target using
// that SAME id, never a locally-generated one, so the local row shares
// identity with lifestyle_master instead of diverging from it.
// `source`/`updatedAt` on CityMasterView are the honest provenance and
// freshness the route-recommendation feature reports back to its caller.
public interface LifestyleMasterReadPort {
    Optional<CityMasterView> findCity(String name);
    List<CityMasterView> findAllCities();
    Optional<CurrencyMasterView> findCurrency(String isoCode);
    Optional<TransportModeView> findTransportMode(String code);
    Optional<PincodeView> findPincode(String code);

    record CityMasterView(UUID id, String name, String countryCode, Double latitude, Double longitude, String source, String updatedAt) {}
    record CurrencyMasterView(UUID id, String isoCode) {}
    record TransportModeView(UUID id, String code, String label) {}

    // A GeoNames postal-code result staged by integration-service — not a
    // lifestyle_master master (see integration's CityPincode for why), but
    // a real, sourced place a route's "via" can resolve to.
    record PincodeView(UUID cityId, String cityName, String countryCode, String pincode, String placeName,
                        Double latitude, Double longitude, String source, String capturedAt) {}
}
