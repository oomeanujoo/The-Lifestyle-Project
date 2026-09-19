package com.thelifestyle.integration.adapter.in.web;

import java.time.Instant;

public record MasterStatusResponse(String masterName, long recordCount, Instant lastRefreshedAt, String lastStatus,
                                    String source, String failureReason) {}
