package com.thelifestyle.integration.adapter.out.resilience;

// How hard a named external call is allowed to be hit. limitForPeriod/
// refreshPeriodMs are deliberately separate from a single "per minute"
// number so a sub-second cap (Mistral's real 1 req/sec limit) can be
// expressed precisely — limitForPeriod=1, refreshPeriodMs=1200 means
// "at most 1 call per 1.2 seconds," which a naive 50-per-minute bucket
// could not enforce (it would let 50 calls fire in the first second).
public record ResilienceConfig(int limitForPeriod, long refreshPeriodMs) {}
