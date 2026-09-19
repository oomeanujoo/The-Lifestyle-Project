package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// The first code that ever writes to travel.audit_log — the table has
// existed since the schema was created (§16) with nothing writing to it.
@Repository
public class JdbcAuditLogRepository implements AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(String entityType, UUID entityId, String action) {
        jdbcTemplate.update("""
            INSERT INTO travel.audit_log (entity_type, entity_id, action)
            VALUES (?, ?, ?)
            """, entityType, entityId, action);
    }
}
