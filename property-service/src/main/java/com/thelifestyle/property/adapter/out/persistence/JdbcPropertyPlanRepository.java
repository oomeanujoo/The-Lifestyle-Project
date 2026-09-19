package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.PropertyPlanRepository;
import com.thelifestyle.property.domain.PropertyPlan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Plain JdbcTemplate, no JPA — matching every other repository in this
// codebase. A property_plan has no natural key, so new rows rely on the
// table's own gen_random_uuid()/now() defaults, same as travel.trip_plan.
@Repository
public class JdbcPropertyPlanRepository implements PropertyPlanRepository {
    private static final String SELECT_COLUMNS = "id, title, status, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcPropertyPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PropertyPlan create(String title) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO property.property_plan (title)
            VALUES (?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcPropertyPlanRepository::mapRow, title);
    }

    @Override
    public Optional<PropertyPlan> findById(UUID id) {
        return jdbcTemplate.query("""
            SELECT %s FROM property.property_plan WHERE id = ?
            """.formatted(SELECT_COLUMNS), JdbcPropertyPlanRepository::mapRow, id).stream().findFirst();
    }

    @Override
    public List<PropertyPlan> findAll() {
        return jdbcTemplate.query("""
            SELECT %s FROM property.property_plan ORDER BY created_at DESC
            """.formatted(SELECT_COLUMNS), JdbcPropertyPlanRepository::mapRow);
    }

    @Override
    public Optional<PropertyPlan> updateStatus(UUID id, String status) {
        return jdbcTemplate.query("""
            UPDATE property.property_plan SET status = ?, updated_at = now()
            WHERE id = ?
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcPropertyPlanRepository::mapRow, status, id).stream().findFirst();
    }

    private static PropertyPlan mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PropertyPlan(
            (UUID) rs.getObject("id"),
            rs.getString("title"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant().toString(),
            rs.getTimestamp("updated_at").toInstant().toString());
    }
}
