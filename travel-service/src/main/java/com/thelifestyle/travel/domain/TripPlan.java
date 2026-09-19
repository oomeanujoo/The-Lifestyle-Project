package com.thelifestyle.travel.domain;

import java.util.UUID;

// A row from travel.trip_plan (V2__domain_tables.sql). `status` is
// DRAFT/ACTIVE/ARCHIVED, the same three literals as the table's own CHECK
// constraint — a workflow/protocol state, not master reference data (the
// same category as ai_suggestion's acceptance_status elsewhere in this
// codebase), so mirroring it in TripPlanUseCase for a clean 400 isn't the
// kind of hardcoded business data the master-data cleanup targeted.
public record TripPlan(UUID id, String title, String status, String createdAt, String updatedAt) {}
