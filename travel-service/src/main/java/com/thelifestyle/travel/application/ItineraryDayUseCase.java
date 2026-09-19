package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.ItineraryDayRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.ItineraryDay;
import com.thelifestyle.travel.domain.ItineraryDayOutcome;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

// travel.itinerary_day has existed since the schema was created (§16),
// with zero application code touching it until now.
@Service
public class ItineraryDayUseCase {
    private static final String ENTITY_TYPE = "itinerary_day";

    private final ItineraryDayRepository itineraryDayRepository;
    private final TripPlanRepository tripPlanRepository;
    private final AuditLogRepository auditLogRepository;

    public ItineraryDayUseCase(ItineraryDayRepository itineraryDayRepository, TripPlanRepository tripPlanRepository,
                                AuditLogRepository auditLogRepository) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.tripPlanRepository = tripPlanRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public ItineraryDayOutcome create(UUID tripPlanId, int dayNumber, String title, String detail) {
        if (tripPlanRepository.findById(tripPlanId).isEmpty()) {
            return ItineraryDayOutcome.error("trip plan '" + tripPlanId + "' does not exist");
        }
        if (dayNumber <= 0) {
            return ItineraryDayOutcome.error("dayNumber must be greater than 0");
        }
        if (!StringUtils.hasText(title)) {
            return ItineraryDayOutcome.error("title is required");
        }
        if (itineraryDayRepository.findByTripPlanAndDayNumber(tripPlanId, dayNumber).isPresent()) {
            return ItineraryDayOutcome.error("day " + dayNumber + " already exists for this trip plan");
        }

        var day = itineraryDayRepository.create(tripPlanId, dayNumber, title.trim(), detail);
        auditLogRepository.record(ENTITY_TYPE, day.id(), "CREATE");
        return ItineraryDayOutcome.ok(day);
    }

    public List<ItineraryDay> findByTripPlan(UUID tripPlanId) {
        return itineraryDayRepository.findByTripPlan(tripPlanId);
    }
}
