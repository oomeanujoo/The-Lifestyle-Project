package com.thelifestyle.property.adapter.out.persistence;

import com.thelifestyle.property.application.port.out.CurrencyMasterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcCurrencyMasterRepository implements CurrencyMasterRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCurrencyMasterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(String isoCode, Double exchangeRateToBase) {
        jdbcTemplate.update("""
            INSERT INTO property.currency (iso_code, exchange_rate_to_base, rate_captured_at)
            VALUES (?, ?, now())
            ON CONFLICT (iso_code) DO UPDATE SET
                exchange_rate_to_base = EXCLUDED.exchange_rate_to_base,
                rate_captured_at = now()
            """, isoCode, exchangeRateToBase);
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM property.currency", Long.class);
        return result == null ? 0 : result;
    }

    @Override
    public List<String> findAllIsoCodes() {
        return jdbcTemplate.queryForList("SELECT iso_code FROM property.currency ORDER BY iso_code", String.class);
    }
}
