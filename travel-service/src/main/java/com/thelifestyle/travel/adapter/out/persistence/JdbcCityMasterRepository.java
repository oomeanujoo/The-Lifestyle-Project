package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.CityMasterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// Real Postgres adapter behind CityMasterRepository — replaces the earlier
// in-memory placeholder used by PlaceSearchUseCase's repository (a
// different port, for search; this one is for the refresh job). Upsert via
// ON CONFLICT, never delete (§17.1).
@Repository
public class JdbcCityMasterRepository implements CityMasterRepository {
    private static final String COUNTRY_CODE = "IN";

    private final JdbcTemplate jdbcTemplate;

    public JdbcCityMasterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(String name, String countryCode, Double latitude, Double longitude) {
        var resolvedCountryCode = countryCode == null || countryCode.isBlank() ? COUNTRY_CODE : countryCode;
        jdbcTemplate.update("""
            INSERT INTO travel.city (name, country_code, latitude, longitude, updated_at)
            VALUES (?, ?, ?, ?, now())
            ON CONFLICT (name, country_code) DO UPDATE SET
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                updated_at = now()
            """, name, resolvedCountryCode, latitude, longitude);
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM travel.city", Long.class);
        return result == null ? 0 : result;
    }
}
