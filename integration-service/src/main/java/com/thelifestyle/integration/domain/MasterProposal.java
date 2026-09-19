package com.thelifestyle.integration.domain;

import java.util.UUID;

// A row from integration.master_proposal — an AI-produced idea for a master
// this service has no verified provider for (visa_requirement text, or the
// municipality/area/locality/pincode hierarchy). `proposedData` is the raw
// JSON an accepting human/reviewer reads to decide whether to manually
// create the real lifestyle_master row via the existing masters API — this
// record is never itself written into an authoritative master table.
// `verificationStatus` mirrors travel-service's ai_suggestion pattern
// (UNVERIFIED/VERIFIED/REJECTED); `acceptanceStatus` gates visibility
// (DRAFT/ACCEPTED/REJECTED) and starts and stays DRAFT until a human acts.
public record MasterProposal(
    UUID id, String masterType, String proposedData, String provenance,
    String verificationStatus, String acceptanceStatus, String createdAt, String acceptedAt) {}
