package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.DocumentRequirementRepository;
import com.thelifestyle.travel.domain.DocumentRequirement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDocumentRequirementRepository implements DocumentRequirementRepository {
    private static final String SELECT_COLUMNS = "id, trip_plan_id, visa_requirement_id, title, verified, created_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentRequirementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DocumentRequirement create(UUID tripPlanId, UUID visaRequirementId, String title) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO travel.document_requirement (trip_plan_id, visa_requirement_id, title)
            VALUES (?, ?, ?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcDocumentRequirementRepository::mapRow, tripPlanId, visaRequirementId, title);
    }

    @Override
    public List<DocumentRequirement> findByTripPlan(UUID tripPlanId) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.document_requirement WHERE trip_plan_id = ? ORDER BY created_at
            """.formatted(SELECT_COLUMNS), JdbcDocumentRequirementRepository::mapRow, tripPlanId);
    }

    @Override
    public Optional<DocumentRequirement> findById(UUID id) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.document_requirement WHERE id = ?
            """.formatted(SELECT_COLUMNS), JdbcDocumentRequirementRepository::mapRow, id).stream().findFirst();
    }

    @Override
    public Optional<DocumentRequirement> updateVerified(UUID id, boolean verified) {
        return jdbcTemplate.query("""
            UPDATE travel.document_requirement SET verified = ?
            WHERE id = ?
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcDocumentRequirementRepository::mapRow, verified, id).stream().findFirst();
    }

    private static DocumentRequirement mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new DocumentRequirement(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("trip_plan_id"),
            (UUID) rs.getObject("visa_requirement_id"),
            rs.getString("title"),
            rs.getBoolean("verified"),
            rs.getTimestamp("created_at").toInstant().toString());
    }
}
