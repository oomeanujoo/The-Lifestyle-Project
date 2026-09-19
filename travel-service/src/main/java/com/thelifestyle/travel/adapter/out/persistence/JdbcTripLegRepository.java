package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.TripLegRepository;
import com.thelifestyle.travel.domain.TripLeg;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// from_city_id/to_city_id/transport_mode_id are trusted to already be
// valid local ids by the time this is called — TripLegUseCase resolves
// them via LocalMasterIdResolver first; this class only ever inserts.
@Repository
public class JdbcTripLegRepository implements TripLegRepository {
    private static final String SELECT_COLUMNS =
        "id, route_option_id, from_city_id, to_city_id, transport_mode_id, leg_order, created_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcTripLegRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TripLeg create(UUID routeOptionId, UUID fromCityId, UUID toCityId, UUID transportModeId, int legOrder) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO travel.trip_leg (route_option_id, from_city_id, to_city_id, transport_mode_id, leg_order)
            VALUES (?, ?, ?, ?, ?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcTripLegRepository::mapRow,
            routeOptionId, fromCityId, toCityId, transportModeId, legOrder);
    }

    @Override
    public List<TripLeg> findByRouteOption(UUID routeOptionId) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.trip_leg WHERE route_option_id = ? ORDER BY leg_order
            """.formatted(SELECT_COLUMNS), JdbcTripLegRepository::mapRow, routeOptionId);
    }

    @Override
    public Optional<TripLeg> findById(UUID id) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.trip_leg WHERE id = ?
            """.formatted(SELECT_COLUMNS), JdbcTripLegRepository::mapRow, id).stream().findFirst();
    }

    private static TripLeg mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new TripLeg(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("route_option_id"),
            (UUID) rs.getObject("from_city_id"),
            (UUID) rs.getObject("to_city_id"),
            (UUID) rs.getObject("transport_mode_id"),
            rs.getInt("leg_order"),
            rs.getTimestamp("created_at").toInstant().toString());
    }
}
