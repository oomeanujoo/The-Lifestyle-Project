package com.thelifestyle.property.domain;

import java.time.Instant;

// Read straight from the database for the Settings page — record count and
// last-refresh outcome, never a value invented in code. lastRefreshedAt/
// lastStatus are null when a master has never been refreshed.
public record MasterStatus(String masterName, long recordCount, Instant lastRefreshedAt, String lastStatus) {}
