package com.thelifestyle.integration.adapter.in.web;

public record MasterRefreshOutcomeResponse(String masterName, String status, int recordsUpserted, String errorMessage) {}
