package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.RouteOptionRepository;
import com.thelifestyle.travel.application.port.out.TripLegRepository;
import com.thelifestyle.travel.domain.TripLeg;
import com.thelifestyle.travel.domain.TripLegOutcome;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

// The first real caller of LocalMasterIdResolver (§21 phase M) — until
// now it only had a resolver test exercising it, no actual FK write. A
// leg names its cities/transport-mode by NAME (e.g. "Pune", "Flight"),
// resolved against the refreshed lifestyle_master masters into the local
// ids travel.trip_leg's FKs need; an unresolvable name is an honest error,
// never a fabricated id.
@Service
public class TripLegUseCase {
    // Only used as the fallback lookup key if integration-service has no
    // match for the name at all (LocalMasterIdResolver's "use whatever
    // local row already exists" path) — matches the DEFAULT_COUNTRY_CODE
    // convention already used for city upserts elsewhere in this service.
    private static final String DEFAULT_COUNTRY_CODE = "IN";

    private static final String ENTITY_TYPE = "trip_leg";

    private final TripLegRepository tripLegRepository;
    private final RouteOptionRepository routeOptionRepository;
    private final LocalMasterIdResolver masterIdResolver;
    private final AuditLogRepository auditLogRepository;

    public TripLegUseCase(TripLegRepository tripLegRepository, RouteOptionRepository routeOptionRepository,
                           LocalMasterIdResolver masterIdResolver, AuditLogRepository auditLogRepository) {
        this.tripLegRepository = tripLegRepository;
        this.routeOptionRepository = routeOptionRepository;
        this.masterIdResolver = masterIdResolver;
        this.auditLogRepository = auditLogRepository;
    }

    public TripLegOutcome create(UUID routeOptionId, String fromCity, String toCity, String transportMode, int legOrder) {
        if (routeOptionRepository.findById(routeOptionId).isEmpty()) {
            return TripLegOutcome.error("route option '" + routeOptionId + "' does not exist");
        }
        if (!StringUtils.hasText(fromCity) || !StringUtils.hasText(toCity) || !StringUtils.hasText(transportMode)) {
            return TripLegOutcome.error("fromCity, toCity, and transportMode are all required");
        }
        if (legOrder <= 0) {
            return TripLegOutcome.error("legOrder must be greater than 0");
        }

        var fromCityId = masterIdResolver.resolveCityId(fromCity, DEFAULT_COUNTRY_CODE);
        if (fromCityId.isEmpty()) {
            return TripLegOutcome.error("fromCity '" + fromCity + "' was not found in the refreshed lifestyle_master.city masters");
        }
        var toCityId = masterIdResolver.resolveCityId(toCity, DEFAULT_COUNTRY_CODE);
        if (toCityId.isEmpty()) {
            return TripLegOutcome.error("toCity '" + toCity + "' was not found in the refreshed lifestyle_master.city masters");
        }
        var transportModeId = masterIdResolver.resolveTransportModeId(transportMode);
        if (transportModeId.isEmpty()) {
            return TripLegOutcome.error("transportMode '" + transportMode
                + "' was not found — it must exist in lifestyle_master.transport_mode first (POST it via integration-service)");
        }

        var leg = tripLegRepository.create(routeOptionId, fromCityId.get(), toCityId.get(), transportModeId.get(), legOrder);
        auditLogRepository.record(ENTITY_TYPE, leg.id(), "CREATE");
        return TripLegOutcome.ok(leg);
    }

    public List<TripLeg> findByRouteOption(UUID routeOptionId) {
        return tripLegRepository.findByRouteOption(routeOptionId);
    }
}
