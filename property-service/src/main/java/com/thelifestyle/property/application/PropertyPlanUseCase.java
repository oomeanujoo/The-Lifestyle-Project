package com.thelifestyle.property.application;

import com.thelifestyle.property.application.port.out.PropertyPlanRepository;
import com.thelifestyle.property.domain.PropertyPlan;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// The first real write path into property's transaction schema —
// property.property_plan has existed since the schema was created, with
// zero application code touching it until now. Mirrors travel-service's
// TripPlanUseCase exactly (§21 phase M).
@Service
public class PropertyPlanUseCase {
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "ACTIVE", "ARCHIVED");

    private final PropertyPlanRepository propertyPlanRepository;

    public PropertyPlanUseCase(PropertyPlanRepository propertyPlanRepository) {
        this.propertyPlanRepository = propertyPlanRepository;
    }

    public Optional<PropertyPlan> create(String title) {
        if (!StringUtils.hasText(title)) return Optional.empty();
        return Optional.of(propertyPlanRepository.create(title.trim()));
    }

    public Optional<PropertyPlan> findById(UUID id) {
        return propertyPlanRepository.findById(id);
    }

    public List<PropertyPlan> findAll() {
        return propertyPlanRepository.findAll();
    }

    public Optional<PropertyPlan> updateStatus(UUID id, String status) {
        if (status == null || !VALID_STATUSES.contains(status.toUpperCase())) return Optional.empty();
        return propertyPlanRepository.updateStatus(id, status.toUpperCase());
    }
}
