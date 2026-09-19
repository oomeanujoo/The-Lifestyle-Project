package com.thelifestyle.travel.adapter.out.persistence;

import com.thelifestyle.travel.application.port.out.ItineraryDayRepository;
import com.thelifestyle.travel.domain.ItineraryDay;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcItineraryDayRepository implements ItineraryDayRepository {
    private static final String SELECT_COLUMNS = "id, trip_plan_id, day_number, title, detail";

    private final JdbcTemplate jdbcTemplate;

    public JdbcItineraryDayRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ItineraryDay create(UUID tripPlanId, int dayNumber, String title, String detail) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO travel.itinerary_day (trip_plan_id, day_number, title, detail)
            VALUES (?, ?, ?, ?)
            RETURNING %s
            """.formatted(SELECT_COLUMNS), JdbcItineraryDayRepository::mapRow, tripPlanId, dayNumber, title, detail);
    }

    @Override
    public List<ItineraryDay> findByTripPlan(UUID tripPlanId) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.itinerary_day WHERE trip_plan_id = ? ORDER BY day_number
            """.formatted(SELECT_COLUMNS), JdbcItineraryDayRepository::mapRow, tripPlanId);
    }

    @Override
    public Optional<ItineraryDay> findByTripPlanAndDayNumber(UUID tripPlanId, int dayNumber) {
        return jdbcTemplate.query("""
            SELECT %s FROM travel.itinerary_day WHERE trip_plan_id = ? AND day_number = ?
            """.formatted(SELECT_COLUMNS), JdbcItineraryDayRepository::mapRow, tripPlanId, dayNumber).stream().findFirst();
    }

    private static ItineraryDay mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ItineraryDay(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("trip_plan_id"),
            rs.getInt("day_number"),
            rs.getString("title"),
            rs.getString("detail"));
    }
}
