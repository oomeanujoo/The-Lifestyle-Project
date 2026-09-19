package com.thelifestyle.integration.adapter.out.persistence;

import com.thelifestyle.integration.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.integration.domain.MasterRefreshOutcome;
import com.thelifestyle.integration.domain.MasterStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

// Append-only log of every refresh attempt (§17.3) — a failed attempt is
// recorded too, never silently swallowed. Lives in integration's own
// schema, not lifestyle_master — refresh logs stay with the service that
// ran the refresh, per scripts/lifestyle-master-schema.sql's own comment.
@Repository
public class JdbcMasterRefreshLogRepository implements MasterRefreshLogRepository {
    // All ten lifestyle_master tables, not just the five with a real
    // refresh source — status reports honestly on every master this
    // service now owns (§20), including the ones still deferred
    // (visa_requirement has no source at all; municipality/area/locality/
    // pincode need data.gov.in, explicitly kept deferred). Those simply
    // show a real (likely 0) row count with no refresh history yet.
    // "india_pincode" is not one of the ten lifestyle_master tables above —
    // it counts rows in lifestyle_master.city_pincode, the same-shaped
    // sibling table refreshIndianPincodes() writes to instead of
    // lifestyle_master.pincode itself (see CityPincode's comment for why).
    // Reported here anyway since it's a real master this service refreshes
    // and status should be honest about every one of them.
    private static final List<String> KNOWN_MASTERS = List.of(
        "city", "currency", "transport_mode", "bhk_type", "service_addon",
        "visa_requirement", "municipality", "area", "locality", "pincode", "india_pincode");

    // No authoritative source or schema mapping exists for any of these
    // (visa_requirement has no free source at all; municipality/area/
    // locality/pincode need data.gov.in's response mapped onto the real
    // locality hierarchy, not yet built). A `null` lastStatus would be
    // ambiguous — "never run yet, but could be" vs. "cannot be run at
    // all" — so these five get an explicit synthetic UNSUPPORTED status
    // instead of silently reporting no status, per the honest-refresh-
    // status requirement (§17.3/§19). This is never written to
    // master_refresh_log — it's synthesized at read time, since no actual
    // refresh attempt happened to log.
    private static final List<String> NO_SOURCE_MASTERS = List.of(
        "visa_requirement", "municipality", "area", "locality", "pincode");
    private static final String UNSUPPORTED_STATUS = "UNSUPPORTED";

    private final JdbcTemplate jdbcTemplate;

    public JdbcMasterRefreshLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(MasterRefreshOutcome outcome, Instant startedAt) {
        jdbcTemplate.update("""
            INSERT INTO integration.master_refresh_log (master_name, status, records_upserted, error_message, started_at, completed_at)
            VALUES (?, ?, ?, ?, ?, now())
            """, outcome.masterName(), outcome.status(), outcome.recordsUpserted(), outcome.errorMessage(), Timestamp.from(startedAt));
    }

    @Override
    public List<MasterStatus> statusForAllMasters() {
        return KNOWN_MASTERS.stream().map(this::statusFor).toList();
    }

    private MasterStatus statusFor(String masterName) {
        var count = countFor(masterName);
        var source = sourceFor(masterName);
        var latest = jdbcTemplate.query("""
            SELECT status, error_message, completed_at FROM integration.master_refresh_log
            WHERE master_name = ? ORDER BY completed_at DESC LIMIT 1
            """, (rs, rowNum) -> new MasterStatus(masterName, count, rs.getTimestamp("completed_at").toInstant(),
                rs.getString("status"), source, rs.getString("error_message")),
            masterName);
        if (!latest.isEmpty()) return latest.get(0);
        var syntheticStatus = NO_SOURCE_MASTERS.contains(masterName) ? UNSUPPORTED_STATUS : null;
        return new MasterStatus(masterName, count, null, syntheticStatus, source, null);
    }

    // A short, honest description of where each master's data actually
    // comes from — never invented per-row, just a fixed label per master
    // category so a status response answers "where would this data even
    // come from" without a caller having to already know the architecture.
    private static String sourceFor(String masterName) {
        return switch (masterName) {
            case "city", "india_pincode" -> "GeoNames";
            case "currency" -> "Frankfurter";
            case "transport_mode", "bhk_type", "service_addon" -> "Manual CRUD, or an AI-drafted proposal (DRAFT until accepted)";
            case "visa_requirement", "municipality", "area", "locality", "pincode" -> "None — no authoritative source or schema mapping exists yet";
            default -> "Unknown";
        };
    }

    private long countFor(String masterName) {
        var table = switch (masterName) {
            case "city" -> "lifestyle_master.city";
            case "currency" -> "lifestyle_master.currency";
            case "transport_mode" -> "lifestyle_master.transport_mode";
            case "bhk_type" -> "lifestyle_master.bhk_type";
            case "service_addon" -> "lifestyle_master.service_addon";
            case "visa_requirement" -> "lifestyle_master.visa_requirement";
            case "municipality" -> "lifestyle_master.municipality";
            case "area" -> "lifestyle_master.area";
            case "locality" -> "lifestyle_master.locality";
            case "pincode" -> "lifestyle_master.pincode";
            case "india_pincode" -> "lifestyle_master.city_pincode";
            default -> throw new IllegalArgumentException("Unknown master: " + masterName);
        };
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return result == null ? 0 : result;
    }
}
