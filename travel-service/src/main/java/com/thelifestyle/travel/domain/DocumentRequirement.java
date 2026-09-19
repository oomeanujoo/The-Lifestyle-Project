package com.thelifestyle.travel.domain;

import java.util.UUID;

// A row from travel.document_requirement — one checklist item (e.g. a visa
// document) for a trip plan. visa_requirement_id is nullable: travel.visa_requirement
// is manually curated with no live source (§19), so a document_requirement can
// exist without linking to one yet.
public record DocumentRequirement(
    UUID id, UUID tripPlanId, UUID visaRequirementId, String title, boolean verified, String createdAt) {}
