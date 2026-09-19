package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.RouteOptionRepository;
import com.thelifestyle.travel.domain.RouteOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRouteOptionRepository implements RouteOptionRepository {
    private static final String SELECT_COLUMNS = "id, trip_plan_id, label, created_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcRouteOptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RouteOption create(UUID tripPlanId, String label) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO travel.route_option (trip_plan_id, label)
            VALUES (?, ?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcRouteOptionRepository::mapRow, tripPlanId, label);
    }

    @Override
    public Optional<RouteOption> findById(UUID id) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.route_option WHERE id = ?
            """.formatted(SELECT_COLUMNS), JdbcRouteOptionRepository::mapRow, id).stream().findFirst();
    }

    @Override
    public List<RouteOption> findByTripPlan(UUID tripPlanId) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.route_option WHERE trip_plan_id = ? ORDER BY created_at
            """.formatted(SELECT_COLUMNS), JdbcRouteOptionRepository::mapRow, tripPlanId);
    }

    private static RouteOption mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RouteOption(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("trip_plan_id"),
            rs.getString("label"),
            rs.getTimestamp("created_at").toInstant().toString());
    }
}
