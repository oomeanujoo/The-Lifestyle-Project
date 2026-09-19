package com.thelifestyle.integration.adapter.in.web;

public record MasterProposalResponse(
    String id, String masterType, String proposedData, String provenance,
    String verificationStatus, String acceptanceStatus, String createdAt, String acceptedAt) {}
