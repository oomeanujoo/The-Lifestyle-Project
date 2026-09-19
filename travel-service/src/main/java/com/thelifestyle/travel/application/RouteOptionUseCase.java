package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.RouteOptionRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.RouteOption;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RouteOptionUseCase {
    private static final String ENTITY_TYPE = "route_option";

    private final RouteOptionRepository routeOptionRepository;
    private final TripPlanRepository tripPlanRepository;
    private final AuditLogRepository auditLogRepository;

    public RouteOptionUseCase(RouteOptionRepository routeOptionRepository, TripPlanRepository tripPlanRepository,
                               AuditLogRepository auditLogRepository) {
        this.routeOptionRepository = routeOptionRepository;
        this.tripPlanRepository = tripPlanRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // Optional.empty() covers both "trip plan doesn't exist" and "blank
    // label" — the controller doesn't need to tell them apart to return an
    // honest 400/404, and neither failure should create a dangling
    // route_option with no valid parent.
    public Optional<RouteOption> create(UUID tripPlanId, String label) {
        if (!StringUtils.hasText(label)) return Optional.empty();
        if (tripPlanRepository.findById(tripPlanId).isEmpty()) return Optional.empty();
        var option = routeOptionRepository.create(tripPlanId, label.trim());
        auditLogRepository.record(ENTITY_TYPE, option.id(), "CREATE");
        return Optional.of(option);
    }

    public List<RouteOption> findByTripPlan(UUID tripPlanId) {
        return routeOptionRepository.findByTripPlan(tripPlanId);
    }
}
