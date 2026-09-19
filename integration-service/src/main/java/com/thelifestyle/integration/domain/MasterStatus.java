package com.thelifestyle.integration.domain;

import java.time.Instant;

// Read straight from the database for Travel/Property's status view —
// record count and last-refresh outcome, never a value invented in code.
// lastRefreshedAt/lastStatus are null when a master has never been refreshed
// AND has a real source that just hasn't run yet; `lastStatus` is the
// synthetic "UNSUPPORTED" instead when no source/schema mapping exists at
// all (§17.3/§19 — see JdbcMasterRefreshLogRepository). `source` is a
// short, honest description of where this master's data comes from
// (GeoNames, Frankfurter, manual CRUD, AI proposal draft, or "none");
// `failureReason` carries the real error message from the last FAILED/
// PARTIAL attempt, null otherwise.
public record MasterStatus(String masterName, long recordCount, Instant lastRefreshedAt, String lastStatus,
                            String source, String failureReason) {}
