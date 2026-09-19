package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.TripPlanRepository;
import com.thelifestyle.travel.domain.TripPlan;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// The first real write path into travel's transaction schema (§16/§21
// phase M) — travel.trip_plan has existed since the schema was created,
// with zero application code touching it until now. `VALID_STATUSES`
// mirrors the table's own CHECK constraint (a workflow/protocol state,
// same category as ai_suggestion's acceptance_status elsewhere in this
// codebase) purely so a bad status returns a clean error here instead of
// a raw DB constraint violation — the database's constraint remains the
// actual source of truth.
@Service
public class TripPlanUseCase {
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "ACTIVE", "ARCHIVED");

    private static final String ENTITY_TYPE = "trip_plan";

    private final TripPlanRepository tripPlanRepository;
    private final AuditLogRepository auditLogRepository;

    public TripPlanUseCase(TripPlanRepository tripPlanRepository, AuditLogRepository auditLogRepository) {
        this.tripPlanRepository = tripPlanRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Optional<TripPlan> create(String title) {
        if (!StringUtils.hasText(title)) return Optional.empty();
        var plan = tripPlanRepository.create(title.trim());
        auditLogRepository.record(ENTITY_TYPE, plan.id(), "CREATE");
        return Optional.of(plan);
    }

    public Optional<TripPlan> findById(UUID id) {
        return tripPlanRepository.findById(id);
    }

    public List<TripPlan> findAll() {
        return tripPlanRepository.findAll();
    }

    public Optional<TripPlan> updateStatus(UUID id, String status) {
        if (status == null || !VALID_STATUSES.contains(status.toUpperCase())) return Optional.empty();
        var updated = tripPlanRepository.updateStatus(id, status.toUpperCase());
        updated.ifPresent(plan -> auditLogRepository.record(ENTITY_TYPE, plan.id(), "UPDATE"));
        return updated;
    }
}
