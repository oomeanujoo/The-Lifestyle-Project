package com.thelifestyle.integration.adapter.out.persistence;

import com.thelifestyle.integration.application.port.out.LifestyleMasterCurrencyRepository;
import com.thelifestyle.integration.domain.CurrencyMaster;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLifestyleMasterCurrencyRepository implements LifestyleMasterCurrencyRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcLifestyleMasterCurrencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(String isoCode, Double exchangeRateToBase) {
        jdbcTemplate.update("""
            INSERT INTO lifestyle_master.currency (iso_code, exchange_rate_to_base, rate_captured_at)
            VALUES (?, ?, now())
            ON CONFLICT (iso_code) DO UPDATE SET
                exchange_rate_to_base = EXCLUDED.exchange_rate_to_base,
                rate_captured_at = now()
            """, isoCode, exchangeRateToBase);
    }

    @Override
    public List<String> findAllIsoCodes() {
        return jdbcTemplate.queryForList(
            "SELECT iso_code FROM lifestyle_master.currency ORDER BY iso_code", String.class);
    }

    @Override
    public Optional<CurrencyMaster> findByIsoCode(String isoCode) {
        try {
            CurrencyMaster result = jdbcTemplate.queryForObject("""
                SELECT id, iso_code
                FROM lifestyle_master.currency
                WHERE iso_code = ?
                """, (rs, rowNum) -> new CurrencyMaster(
                    (UUID) rs.getObject("id"),
                    rs.getString("iso_code")),
                isoCode);
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT count(*) FROM lifestyle_master.currency", Long.class);
        return result == null ? 0 : result;
    }
}
