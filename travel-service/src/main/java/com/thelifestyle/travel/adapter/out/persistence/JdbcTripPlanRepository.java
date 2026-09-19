package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.TripPlan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Plain JdbcTemplate, no JPA — matching every other repository in this
// codebase (no entity layer exists anywhere, a deliberate choice, §12).
// Writes real rows into travel.trip_plan; new-row inserts rely on the
// table's own gen_random_uuid()/now() defaults, since a trip_plan has no
// natural key to upsert on — unlike a master row, two trip plans with the
// same title are two different plans, not a conflict.
@Repository
public class JdbcTripPlanRepository implements TripPlanRepository {
    private static final String SELECT_COLUMNS = "id, title, status, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcTripPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TripPlan create(String title) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO travel.trip_plan (title)
            VALUES (?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcTripPlanRepository::mapRow, title);
    }

    @Override
    public Optional<TripPlan> findById(UUID id) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.trip_plan WHERE id = ?
            """.formatted(SELECT_COLUMNS), JdbcTripPlanRepository::mapRow, id).stream().findFirst();
    }

    @Override
    public List<TripPlan> findAll() {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.trip_plan ORDER BY created_at DESC
            """.formatted(SELECT_COLUMNS), JdbcTripPlanRepository::mapRow);
    }

    @Override
    public Optional<TripPlan> updateStatus(UUID id, String status) {
        return jdbcTemplate.query("""
            UPDATE travel.trip_plan SET status = ?, updated_at = now()
            WHERE id = ?
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcTripPlanRepository::mapRow, status, id).stream().findFirst();
    }

    private static TripPlan mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new TripPlan(
            (UUID) rs.getObject("id"),
            rs.getString("title"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant().toString(),
            rs.getTimestamp("updated_at").toInstant().toString());
    }
}
