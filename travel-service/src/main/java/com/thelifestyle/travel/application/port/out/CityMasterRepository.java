package com.thelifestyle.travel.application.port.out;

// Upsert-and-append only (§17.1) — never a delete. Keyed by (name, country
// code), the natural key added in V3__master_refresh_log.sql.
public interface CityMasterRepository {
    void upsert(String name, String countryCode, Double latitude, Double longitude);
    long count();
}
