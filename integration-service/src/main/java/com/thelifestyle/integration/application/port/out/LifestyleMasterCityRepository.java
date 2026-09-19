package com.thelifestyle.integration.application.port.out;

import com.thelifestyle.integration.domain.CityMaster;

import java.util.List;

// Upsert-and-append only (§17.1) — never a delete. Writes/reads
// lifestyle_master.city, the schema shared across Travel/Property
// (scripts/lifestyle-master-schema.sql) — integration-service is the only
// service granted INSERT/UPDATE on it; Travel/Property get SELECT only,
// and are meant to reach it through this service's read API, not direct SQL.
// upsert() takes and persists `source` (into the row's provider_metadata
// jsonb) and returns the persisted row with its id, so a caller (e.g. the
// Indian-pincode refresh) can immediately use that city's canonical id
// without a second lookup.
public interface LifestyleMasterCityRepository {
    CityMaster upsert(String name, String countryCode, Double latitude, Double longitude, String source);
    List<CityMaster> findByName(String name);
    List<CityMaster> findAll();
    long count();
}
