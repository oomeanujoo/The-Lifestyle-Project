package com.thelifestyle.integration.adapter.out.persistence;

import com.thelifestyle.integration.application.port.out.LifestyleMasterCityRepository;
import com.thelifestyle.integration.domain.CityMaster;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// Real Postgres adapter over the shared lifestyle_master.city table
// (scripts/lifestyle-master-schema.sql) — the write side integration-service
// owns (granted INSERT/UPDATE there; Travel/Property get SELECT only).
// Upsert via ON CONFLICT (name, country_code), never delete (§17.1). `id`
// is always selected/returned — it's the one stable UUID Travel/Property
// must use for any city FK (§17/§20), never a locally-generated one.
// `source` is persisted into provider_metadata (a plain {"source": "..."}
// object — this table has only ever had one source, GeoNames, so this is
// not a history, just the current provenance) and read back out of it.
@Repository
public class JdbcLifestyleMasterCityRepository implements LifestyleMasterCityRepository {
    private static final String DEFAULT_COUNTRY_CODE = "IN";
    private static final String SELECT_COLUMNS =
        "id, name, country_code, latitude, longitude, provider_metadata->>'source' AS source, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcLifestyleMasterCityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CityMaster upsert(String name, String countryCode, Double latitude, Double longitude, String source) {
        var resolvedCountryCode = countryCode == null || countryCode.isBlank() ? DEFAULT_COUNTRY_CODE : countryCode;
        return jdbcTemplate.queryForObject("""
            INSERT INTO lifestyle_master.city (name, country_code, latitude, longitude, provider_metadata, updated_at)
            VALUES (?, ?, ?, ?, jsonb_build_object('source', ?::text), now())
            ON CONFLICT (name, country_code) DO UPDATE SET
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                provider_metadata = EXCLUDED.provider_metadata,
                updated_at = now()
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcLifestyleMasterCityRepository::mapRow,
            name, resolvedCountryCode, latitude, longitude, source);
    }

    @Override
    public List<CityMaster> findByName(String name) {
        return jdbcTemplate.query("""
            SELECT %s
            FROM lifestyle_master.city
            WHERE lower(name) = lower(?)
            ORDER BY name
            """.formatted(SELECT_COLUMNS), JdbcLifestyleMasterCityRepository::mapRow, name);
    }

    @Override
    public List<CityMaster> findAll() {
        return jdbcTemplate.query("""
            SELECT %s
            FROM lifestyle_master.city
            ORDER BY name
            """.formatted(SELECT_COLUMNS), JdbcLifestyleMasterCityRepository::mapRow);
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM lifestyle_master.city", Long.class);
        return result == null ? 0 : result;
    }

    private static CityMaster mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CityMaster(
            (UUID) rs.getObject("id"),
            rs.getString("name"),
            rs.getString("country_code"),
            rs.getObject("latitude") == null ? null : ((Number) rs.getObject("latitude")).doubleValue(),
            rs.getObject("longitude") == null ? null : ((Number) rs.getObject("longitude")).doubleValue(),
            rs.getString("source"),
            rs.getTimestamp("updated_at").toInstant().toString());
    }
}
