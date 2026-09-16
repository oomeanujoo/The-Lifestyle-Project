package com.thelifestyle.travel.adapter.in.web;

import java.time.Instant;

public record MasterStatusResponse(String masterName, long recordCount, Instant lastRefreshedAt, String lastStatus) {}
