package com.thelifestyle.property.domain;

import java.util.UUID;

// A row from property.property_plan. `status` is DRAFT/ACTIVE/ARCHIVED,
// the same three literals as the table's own CHECK constraint — a
// workflow/protocol state (same category as ai_recommendation's
// acceptance_status), not master reference data.
public record PropertyPlan(UUID id, String title, String status, String createdAt, String updatedAt) {}
