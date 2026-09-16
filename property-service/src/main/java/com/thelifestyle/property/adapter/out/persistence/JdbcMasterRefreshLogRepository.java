package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.property.domain.MasterRefreshOutcome;
import com.thelifestyle.property.domain.MasterStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

// Append-only log of every refresh attempt (§17.3) — a failed attempt is
// recorded too, never silently swallowed, so the Settings page can show an
// honest "last attempt failed" state instead of a stale success.
@Repository
public class JdbcMasterRefreshLogRepository implements MasterRefreshLogRepository {
    private static final List<String> KNOWN_MASTERS = List.of("city", "currency", "bhk_type", "service_addon");

    private final JdbcTemplate jdbcTemplate;

    public JdbcMasterRefreshLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(MasterRefreshOutcome outcome, Instant startedAt) {
        jdbcTemplate.update("""
            INSERT INTO property.master_refresh_log (master_name, status, records_upserted, error_message, started_at, completed_at)
            VALUES (?, ?, ?, ?, ?, now())
            """, outcome.masterName(), outcome.status(), outcome.recordsUpserted(), outcome.errorMessage(), Timestamp.from(startedAt));
    }

    @Override
    public List<MasterStatus> statusForAllMasters() {
        return KNOWN_MASTERS.stream().map(this::statusFor).toList();
    }

    private MasterStatus statusFor(String masterName) {
        var count = countFor(masterName);
        var latest = jdbcTemplate.query("""
            SELECT status, completed_at FROM property.master_refresh_log
            WHERE master_name = ? ORDER BY completed_at DESC LIMIT 1
            """, (rs, rowNum) -> new MasterStatus(masterName, count, rs.getTimestamp("completed_at").toInstant(), rs.getString("status")),
            masterName);
        return latest.isEmpty() ? new MasterStatus(masterName, count, null, null) : latest.get(0);
    }

    private long countFor(String masterName) {
        var table = switch (masterName) {
            case "city" -> "property.city";
            case "currency" -> "property.currency";
            case "bhk_type" -> "property.bhk_type";
            case "service_addon" -> "property.service_addon";
            default -> throw new IllegalArgumentException("Unknown master: " + masterName);
        };
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return result == null ? 0 : result;
    }
}
