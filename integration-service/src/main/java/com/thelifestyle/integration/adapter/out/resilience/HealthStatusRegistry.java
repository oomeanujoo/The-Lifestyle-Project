package com.thelifestyle.integration.adapter.out.resilience;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// In-memory only, per process — resets on restart, which is fine: health
// here means "did our last real call to this provider work," not a
// persisted audit trail. Written to exclusively by ResilienceGuard, so
// every guarded call updates health as a side effect, automatically.
@Component
public class HealthStatusRegistry {
    private final Map<String, ProviderHealth> health = new ConcurrentHashMap<>();

    public void recordSuccess(String name) {
        health.put(name, new ProviderHealth(name, ProviderHealth.Status.UP, Instant.now(), null));
    }

    public void recordFailure(String name, String reason) {
        health.put(name, new ProviderHealth(name, ProviderHealth.Status.DOWN, Instant.now(), reason));
    }

    public ProviderHealth get(String name) {
        return health.getOrDefault(name, ProviderHealth.unknown(name));
    }

    public List<ProviderHealth> all() {
        return List.copyOf(health.values());
    }
}
