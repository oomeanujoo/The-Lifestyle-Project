package com.thelifestyle.integration.adapter.out.persistence;

import com.thelifestyle.integration.application.port.out.CityPincodeRepository;
import com.thelifestyle.integration.domain.CityPincode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Writes/reads lifestyle_master.city_pincode — switched from the earlier
// integration.city_pincode staging table now that Codex has created a
// same-shaped sibling table there with a real FK to lifestyle_master.city
// (see CityPincode's own comment for the original reasoning this was ever
// staged separately, and for why this still isn't lifestyle_master.pincode
// itself). integration.city_pincode is intentionally left in place,
// unwritten from now on — Codex owns comparing row counts/ids, migrating
// any data, and removing it once nothing depends on it.
// `city_id` is a real FK (INSERT fails if the city doesn't exist), so
// `cityName`/`countryCode` are read back via a join to lifestyle_master.city
// rather than being duplicated into this table. `source` is persisted into
// provider_metadata (a plain {"source": "..."} object, same convention as
// JdbcLifestyleMasterCityRepository) and read back out of it.
@Repository
public class JdbcCityPincodeRepository implements CityPincodeRepository {
    private static final String SELECT_COLUMNS = """
        p.id, p.city_id, c.name AS city_name, c.country_code, p.pincode, p.place_name, p.admin_name2,
        p.admin_name3, p.latitude, p.longitude, p.provider_metadata->>'source' AS source, p.updated_at
        """;
    private static final String FROM_CLAUSE = "FROM lifestyle_master.city_pincode p JOIN lifestyle_master.city c ON c.id = p.city_id";

    private final JdbcTemplate jdbcTemplate;

    public JdbcCityPincodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(UUID cityId, String pincode, String placeName,
                        String adminName2, String adminName3, Double latitude, Double longitude, String source) {
        jdbcTemplate.update("""
            INSERT INTO lifestyle_master.city_pincode
                (city_id, pincode, place_name, admin_name2, admin_name3, latitude, longitude, provider_metadata, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, jsonb_build_object('source', ?::text), now())
            ON CONFLICT (city_id, pincode) DO UPDATE SET
                place_name = EXCLUDED.place_name,
                admin_name2 = EXCLUDED.admin_name2,
                admin_name3 = EXCLUDED.admin_name3,
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                provider_metadata = EXCLUDED.provider_metadata,
                updated_at = now()
            """, cityId, pincode, placeName, adminName2, adminName3, latitude, longitude, source);
    }

    @Override
    public List<CityPincode> findByCityId(UUID cityId) {
        return jdbcTemplate.query("""
            SELECT %s %s WHERE p.city_id = ? ORDER BY p.pincode
            """.formatted(SELECT_COLUMNS, FROM_CLAUSE), JdbcCityPincodeRepository::mapRow, cityId);
    }

    @Override
    public Optional<CityPincode> findByPincode(String pincode) {
        return jdbcTemplate.query("""
            SELECT %s %s WHERE p.pincode = ? ORDER BY p.updated_at DESC LIMIT 1
            """.formatted(SELECT_COLUMNS, FROM_CLAUSE), JdbcCityPincodeRepository::mapRow, pincode).stream().findFirst();
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM lifestyle_master.city_pincode", Long.class);
        return result == null ? 0 : result;
    }

    private static CityPincode mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CityPincode(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("city_id"),
            rs.getString("city_name"),
            rs.getString("country_code"),
            rs.getString("pincode"),
            rs.getString("place_name"),
            rs.getString("admin_name2"),
            rs.getString("admin_name3"),
            rs.getObject("latitude") == null ? null : ((Number) rs.getObject("latitude")).doubleValue(),
            rs.getObject("longitude") == null ? null : ((Number) rs.getObject("longitude")).doubleValue(),
            rs.getString("source"),
            rs.getTimestamp("updated_at").toInstant().toString());
    }
}
