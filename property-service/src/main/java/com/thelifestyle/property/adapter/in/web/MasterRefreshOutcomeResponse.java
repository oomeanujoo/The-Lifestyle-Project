package com.thelifestyle.property.adapter.in.web;

public record MasterRefreshOutcomeResponse(String masterName, String status, int recordsUpserted, String errorMessage) {}
