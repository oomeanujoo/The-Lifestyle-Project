package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.TransportModeMasterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransportModeMasterRepository implements TransportModeMasterRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTransportModeMasterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(String code, String label) {
        jdbcTemplate.update("""
            INSERT INTO travel.transport_mode (code, label)
            VALUES (?, ?)
            ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label
            """, code, label);
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM travel.transport_mode", Long.class);
        return result == null ? 0 : result;
    }
}
