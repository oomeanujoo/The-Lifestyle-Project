package com.thelifestyle.integration.adapter.out.persistence;

import com.thelifestyle.integration.application.port.out.CodedMasterRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import com.thelifestyle.integration.domain.CodedMaster;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// table() only ever returns one of three hardcoded literals from
// CodedMasterType, never a caller-supplied string — safe from injection by
// construction despite the string-concatenated table name below.
@Repository
public class JdbcCodedMasterRepository implements CodedMasterRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCodedMasterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CodedMaster upsert(CodedMasterType type, String code, String label) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO %s (code, label)
            VALUES (?, ?)
            ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label
            RETURNING id, code, label
            """.formatted(type.table()),
            (rs, rowNum) -> new CodedMaster((UUID) rs.getObject("id"), rs.getString("code"), rs.getString("label")),
            code, label);
    }

    @Override
    public List<CodedMaster> findAll(CodedMasterType type) {
        return jdbcTemplate.query(
            "SELECT id, code, label FROM %s ORDER BY code".formatted(type.table()),
            (rs, rowNum) -> new CodedMaster(
                (UUID) rs.getObject("id"),
                rs.getString("code"),
                rs.getString("label")));
    }

    @Override
    public long count(CodedMasterType type) {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM %s".formatted(type.table()), Long.class);
        return result == null ? 0 : result;
    }
}
