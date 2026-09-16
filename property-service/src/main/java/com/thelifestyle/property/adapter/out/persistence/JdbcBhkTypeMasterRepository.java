package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.BhkTypeMasterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBhkTypeMasterRepository implements BhkTypeMasterRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcBhkTypeMasterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(String code, String label) {
        jdbcTemplate.update("""
            INSERT INTO property.bhk_type (code, label)
            VALUES (?, ?)
            ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label
            """, code, label);
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM property.bhk_type", Long.class);
        return result == null ? 0 : result;
    }
}
