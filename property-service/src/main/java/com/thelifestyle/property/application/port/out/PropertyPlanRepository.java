package com.thelifestyle.property.application.port.out;

import com.thelifestyle.property.domain.PropertyPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyPlanRepository {
    PropertyPlan create(String title);
    Optional<PropertyPlan> findById(UUID id);
    List<PropertyPlan> findAll();
    Optional<PropertyPlan> updateStatus(UUID id, String status);
}
