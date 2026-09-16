package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.ServiceAddonMasterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcServiceAddonMasterRepository implements ServiceAddonMasterRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcServiceAddonMasterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(String code, String label) {
        // On conflict, only the label is refreshed — is_active is left alone
        // rather than reset to true, so deliberately deactivating an addon
        // isn't silently undone by the next refresh.
        jdbcTemplate.update("""
            INSERT INTO property.service_addon (code, label)
            VALUES (?, ?)
            ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label
            """, code, label);
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM property.service_addon", Long.class);
        return result == null ? 0 : result;
    }
}
