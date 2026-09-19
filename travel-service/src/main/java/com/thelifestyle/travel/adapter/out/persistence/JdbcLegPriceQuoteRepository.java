package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.LegPriceQuoteRepository;
import com.thelifestyle.travel.domain.LegPriceQuote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// Plain JdbcTemplate, insert-only — travel.leg_price_quote's own
// travel_leg_price_quote_append_only trigger physically rejects
// UPDATE/DELETE at the database level, so this class doesn't even offer
// those methods; there is nothing here for it to reject.
@Repository
public class JdbcLegPriceQuoteRepository implements LegPriceQuoteRepository {
    private static final String SELECT_COLUMNS =
        "id, trip_leg_id, currency_id, amount, duration_hours, source, source_url, confidence, captured_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcLegPriceQuoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LegPriceQuote create(UUID tripLegId, UUID currencyId, double amount, Double durationHours,
                                 String source, String sourceUrl, String confidence) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO travel.leg_price_quote (trip_leg_id, currency_id, amount, duration_hours, source, source_url, confidence)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcLegPriceQuoteRepository::mapRow,
            tripLegId, currencyId, amount, durationHours, source, sourceUrl, confidence);
    }

    @Override
    public List<LegPriceQuote> findByTripLeg(UUID tripLegId) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.leg_price_quote WHERE trip_leg_id = ? ORDER BY captured_at DESC
            """.formatted(SELECT_COLUMNS), JdbcLegPriceQuoteRepository::mapRow, tripLegId);
    }

    private static LegPriceQuote mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new LegPriceQuote(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("trip_leg_id"),
            (UUID) rs.getObject("currency_id"),
            rs.getDouble("amount"),
            (Double) rs.getObject("duration_hours"),
            rs.getString("source"),
            rs.getString("source_url"),
            rs.getString("confidence"),
            rs.getTimestamp("captured_at").toInstant().toString());
    }
}
