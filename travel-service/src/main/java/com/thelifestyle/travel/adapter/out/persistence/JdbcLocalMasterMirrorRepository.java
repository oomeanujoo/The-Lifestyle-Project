package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.LocalMasterMirrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Writes only to travel's own city/currency/transport_mode tables — never
// lifestyle_master (this service has no write grant there — see
// scripts/lifestyle-master-schema.sql). A brand-new local row is inserted
// WITH the canonical lifestyle_master id explicitly (never the table's
// gen_random_uuid() default), so it shares identity with the shared master
// instead of becoming a permanent, independently-numbered duplicate. An
// already-existing local row's id is never touched — only its non-key
// columns are refreshed — to preserve any real FK already pointing at it;
// a mismatch between that existing id and the canonical id passed in is
// logged as a WARN and left for a manual database-level remap (see the
// FK/ID mapping report for Codex — this service never executes that
// remap itself). No table was created, dropped, or altered to support
// this — travel.city/currency/transport_mode already existed with exactly
// this shape.
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
                UPDATE travel.city SET latitude = ?, longitude = ?, updated_at = now()
                WHERE id = ?
                """, latitude, longitude, existingId.get());
            warnIfDiverged("travel.city", name + "/" + resolvedCountryCode, existingId.get(), canonicalId);
            return existingId.get();
        }
        jdbcTemplate.update("""
            INSERT INTO travel.city (id, name, country_code, latitude, longitude, updated_at)
            VALUES (?, ?, ?, ?, ?, now())
            """, canonicalId, name, resolvedCountryCode, latitude, longitude);
        return canonicalId;
    }

    @Override
    public UUID upsertCurrency(UUID canonicalId, String isoCode, Double exchangeRateToBase) {
        var existingId = findCurrencyId(isoCode);
        if (existingId.isPresent()) {
            jdbcTemplate.update("""
                UPDATE travel.currency SET exchange_rate_to_base = ?, rate_captured_at = now()
                WHERE id = ?
                """, exchangeRateToBase, existingId.get());
            warnIfDiverged("travel.currency", isoCode, existingId.get(), canonicalId);
            return existingId.get();
        }
        jdbcTemplate.update("""
            INSERT INTO travel.currency (id, iso_code, exchange_rate_to_base, rate_captured_at)
            VALUES (?, ?, ?, now())
            """, canonicalId, isoCode, exchangeRateToBase);
        return canonicalId;
    }

    @Override
    public UUID upsertTransportMode(UUID canonicalId, String code, String label) {
        var existingId = findTransportModeId(code);
        if (existingId.isPresent()) {
            jdbcTemplate.update("UPDATE travel.transport_mode SET label = ? WHERE id = ?", label, existingId.get());
            warnIfDiverged("travel.transport_mode", code, existingId.get(), canonicalId);
            return existingId.get();
        }
        jdbcTemplate.update("""
            INSERT INTO travel.transport_mode (id, code, label)
            VALUES (?, ?, ?)
            """, canonicalId, code, label);
        return canonicalId;
    }

    @Override
    public Optional<UUID> findCityId(String name, String countryCode) {
        return jdbcTemplate.query("SELECT id FROM travel.city WHERE lower(name) = lower(?) AND country_code = ?",
            (rs, rowNum) -> (UUID) rs.getObject("id"), name, countryCode).stream().findFirst();
    }

    @Override
    public Optional<UUID> findCurrencyId(String isoCode) {
        return jdbcTemplate.query("SELECT id FROM travel.currency WHERE iso_code = ?",
            (rs, rowNum) -> (UUID) rs.getObject("id"), isoCode).stream().findFirst();
    }

    @Override
    public Optional<UUID> findTransportModeId(String code) {
        return jdbcTemplate.query("SELECT id FROM travel.transport_mode WHERE code = ?",
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
