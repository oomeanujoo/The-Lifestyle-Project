package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.LocalMasterMirrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Writes only to property's own city/currency/bhk_type/service_addon
// tables — never lifestyle_master (this service has no write grant there
// — see scripts/lifestyle-master-schema.sql). A brand-new local row is
// inserted WITH the canonical lifestyle_master id explicitly (never the
// table's gen_random_uuid() default), so it shares identity with the
// shared master instead of becoming a permanent, independently-numbered
// duplicate. An already-existing local row's id is never touched — only
// its non-key columns are refreshed — to preserve any real FK already
// pointing at it; a mismatch between that existing id and the canonical id
// passed in is logged as a WARN and left for a manual database-level remap
// (see the FK/ID mapping report for Codex — this service never executes
// that remap itself). No table was created, dropped, or altered to
// support this.
@Repository
public class JdbcLocalMasterMirrorRepository implements LocalMasterMirrorRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcLocalMasterMirrorRepository.class);
    private static final String DEFAULT_COUNTRY_CODE = "IN";

    private final JdbcTemplate jdbcTemplate;

    public JdbcLocalMasterMirrorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UUID upsertCity(UUID canonicalId, String name, String countryCode, Double latitude, Double longitude) {
        var resolvedCountryCode = countryCode == null || countryCode.isBlank() ? DEFAULT_COUNTRY_CODE : countryCode;
        var existingId = findCityId(name, resolvedCountryCode);
        if (existingId.isPresent()) {
            jdbcTemplate.update("""
                UPDATE property.city SET latitude = ?, longitude = ?, updated_at = now()
                WHERE id = ?
                """, latitude, longitude, existingId.get());
            warnIfDiverged("property.city", name + "/" + resolvedCountryCode, existingId.get(), canonicalId);
            return existingId.get();
        }
        jdbcTemplate.update("""
            INSERT INTO property.city (id, name, country_code, latitude, longitude, updated_at)
            VALUES (?, ?, ?, ?, ?, now())
            """, canonicalId, name, resolvedCountryCode, latitude, longitude);
        return canonicalId;
    }

    @Override
    public UUID upsertCurrency(UUID canonicalId, String isoCode, Double exchangeRateToBase) {
        var existingId = findCurrencyId(isoCode);
        if (existingId.isPresent()) {
            jdbcTemplate.update("""
                UPDATE property.currency SET exchange_rate_to_base = ?, rate_captured_at = now()
                WHERE id = ?
                """, exchangeRateToBase, existingId.get());
            warnIfDiverged("property.currency", isoCode, existingId.get(), canonicalId);
            return existingId.get();
        }
        jdbcTemplate.update("""
            INSERT INTO property.currency (id, iso_code, exchange_rate_to_base, rate_captured_at)
            VALUES (?, ?, ?, now())
            """, canonicalId, isoCode, exchangeRateToBase);
        return canonicalId;
    }

    @Override
    public UUID upsertBhkType(UUID canonicalId, String code, String label) {
        var existingId = findBhkTypeId(code);
        if (existingId.isPresent()) {
            jdbcTemplate.update("UPDATE property.bhk_type SET label = ? WHERE id = ?", label, existingId.get());
            warnIfDiverged("property.bhk_type", code, existingId.get(), canonicalId);
            return existingId.get();
        }
        jdbcTemplate.update("""
            INSERT INTO property.bhk_type (id, code, label)
            VALUES (?, ?, ?)
            """, canonicalId, code, label);
        return canonicalId;
    }

    @Override
    public UUID upsertServiceAddon(UUID canonicalId, String code, String label) {
        var existingId = findServiceAddonId(code);
        if (existingId.isPresent()) {
            jdbcTemplate.update("UPDATE property.service_addon SET label = ? WHERE id = ?", label, existingId.get());
            warnIfDiverged("property.service_addon", code, existingId.get(), canonicalId);
            return existingId.get();
        }
        jdbcTemplate.update("""
            INSERT INTO property.service_addon (id, code, label)
            VALUES (?, ?, ?)
            """, canonicalId, code, label);
        return canonicalId;
    }

    @Override
    public Optional<UUID> findCityId(String name, String countryCode) {
        return jdbcTemplate.query("SELECT id FROM property.city WHERE lower(name) = lower(?) AND country_code = ?",
            (rs, rowNum) -> (UUID) rs.getObject("id"), name, countryCode).stream().findFirst();
    }

    @Override
    public Optional<UUID> findCurrencyId(String isoCode) {
        return jdbcTemplate.query("SELECT id FROM property.currency WHERE iso_code = ?",
            (rs, rowNum) -> (UUID) rs.getObject("id"), isoCode).stream().findFirst();
    }

    @Override
    public Optional<UUID> findBhkTypeId(String code) {
        return jdbcTemplate.query("SELECT id FROM property.bhk_type WHERE code = ?",
            (rs, rowNum) -> (UUID) rs.getObject("id"), code).stream().findFirst();
    }

    @Override
    public Optional<UUID> findServiceAddonId(String code) {
        return jdbcTemplate.query("SELECT id FROM property.service_addon WHERE code = ?",
            (rs, rowNum) -> (UUID) rs.getObject("id"), code).stream().findFirst();
    }

    private static void warnIfDiverged(String table, String naturalKey, UUID existingId, UUID canonicalId) {
        if (!existingId.equals(canonicalId)) {
            log.warn("{} row for '{}' has local id {} which diverges from lifestyle_master canonical id {} — "
                    + "requires a manual database-level remap, see the FK/ID mapping report",
                table, naturalKey, existingId, canonicalId);
        }
    }
}
