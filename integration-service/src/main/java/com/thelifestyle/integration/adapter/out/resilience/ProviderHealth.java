package com.thelifestyle.integration.adapter.out.resilience;

import java.time.Instant;

// UP/DOWN reflect the outcome of the last *real* call this process actually
// made — never a synthetic ping — per the explicit design decision to judge
// health from the last hit and update it on the next one, not spend quota
// on a separate health check. UNKNOWN means no call has been attempted yet
// this process's lifetime (e.g. right after startup, or never configured).
public record ProviderHealth(String name, Status status, Instant lastCheckedAt, String lastError) {
    public enum Status { UP, DOWN, UNKNOWN }

    public static ProviderHealth unknown(String name) {
        return new ProviderHealth(name, Status.UNKNOWN, null, null);
    }
}
