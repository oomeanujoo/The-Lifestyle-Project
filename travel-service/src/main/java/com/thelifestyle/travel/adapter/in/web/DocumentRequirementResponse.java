package com.thelifestyle.travel.adapter.in.web;

public record DocumentRequirementResponse(
    String id, String tripPlanId, String visaRequirementId, String title, boolean verified, String createdAt) {}
