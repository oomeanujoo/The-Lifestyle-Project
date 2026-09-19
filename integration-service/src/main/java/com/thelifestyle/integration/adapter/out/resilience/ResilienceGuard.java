package com.thelifestyle.integration.adapter.out.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// The one place every outbound call in this service goes through — every
// AI provider (Ollama/Groq/Mistral) and every external data client
// (GeoNames/Frankfurter/data.gov.in) is wrapped here instead of each
// hand-rolling its own RateLimiter/CircuitBreaker, per the generic-over-
// bespoke rule (§10). Also the single point where ProviderHealth gets
// updated — health reflects the outcome of the last real call made
// through this class, never a separate synthetic ping, and every call
// updates it, by construction. Logs every attempt at INFO/WARN — no
// credentials or raw request/response bodies, only name, duration, and
// outcome — since HealthStatusRegistry alone only shows the *latest*
// outcome, not a history a person can scroll back through in the console.
@Component
public class ResilienceGuard {
    private static final Logger log = LoggerFactory.getLogger(ResilienceGuard.class);

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final HealthStatusRegistry healthStatusRegistry;

    public ResilienceGuard(HealthStatusRegistry healthStatusRegistry) {
        this.healthStatusRegistry = healthStatusRegistry;
    }

    public <T> T call(String name, ResilienceConfig config, Supplier<T> action, T fallback) {
        var guarded = guard(name, config, action);
        var startedAt = System.currentTimeMillis();
        try {
            var result = guarded.get();
            healthStatusRegistry.recordSuccess(name);
            log.info("[{}] call succeeded in {}ms", name, System.currentTimeMillis() - startedAt);
            return result;
        } catch (RequestNotPermitted ex) {
            healthStatusRegistry.recordFailure(name, "rate limited");
            log.warn("[{}] call rate-limited after {}ms", name, System.currentTimeMillis() - startedAt);
            return fallback;
        } catch (CallNotPermittedException ex) {
            healthStatusRegistry.recordFailure(name, "circuit open — too many recent failures");
            log.warn("[{}] call rejected — circuit open (too many recent failures)", name);
            return fallback;
        } catch (Exception ex) {
            healthStatusRegistry.recordFailure(name, ex.getMessage());
            log.warn("[{}] call failed after {}ms: {}", name, System.currentTimeMillis() - startedAt, ex.toString());
            return fallback;
        }
    }

    // For callers that must not treat "the call failed" the same as "the
    // call succeeded with nothing to report" — a master-data refresh needs
    // to record an honest FAILED outcome, not a masked empty SUCCESS.
    // Health/rate-limit/circuit-breaker behavior is identical to call();
    // only the outcome on failure differs — this rethrows instead of
    // returning a fallback.
    public <T> T callOrThrow(String name, ResilienceConfig config, Supplier<T> action) {
        var guarded = guard(name, config, action);
        var startedAt = System.currentTimeMillis();
        try {
            var result = guarded.get();
            healthStatusRegistry.recordSuccess(name);
            log.info("[{}] call succeeded in {}ms", name, System.currentTimeMillis() - startedAt);
            return result;
        } catch (RequestNotPermitted ex) {
            healthStatusRegistry.recordFailure(name, "rate limited");
            log.warn("[{}] call rate-limited after {}ms", name, System.currentTimeMillis() - startedAt);
            throw new ExternalProviderException(name, "rate limited", ex);
        } catch (CallNotPermittedException ex) {
            healthStatusRegistry.recordFailure(name, "circuit open — too many recent failures");
            log.warn("[{}] call rejected — circuit open (too many recent failures)", name);
            throw new ExternalProviderException(name, "circuit open — too many recent failures", ex);
        } catch (ExternalProviderException ex) {
            // Already the right shape (e.g. GeoNamesClient detected an
            // error-shaped 200 OK response) — record and rethrow as-is,
            // don't double-wrap.
            healthStatusRegistry.recordFailure(name, ex.getMessage());
            log.warn("[{}] call failed after {}ms: {}", name, System.currentTimeMillis() - startedAt, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            healthStatusRegistry.recordFailure(name, ex.getMessage());
            log.warn("[{}] call failed after {}ms: {}", name, System.currentTimeMillis() - startedAt, ex.toString());
            throw new ExternalProviderException(name, ex.getMessage(), ex);
        }
    }

    private <T> Supplier<T> guard(String name, ResilienceConfig config, Supplier<T> action) {
        var rateLimiter = rateLimiters.computeIfAbsent(name, key -> RateLimiter.of(key, RateLimiterConfig.custom()
            .limitForPeriod(config.limitForPeriod())
            .limitRefreshPeriod(Duration.ofMillis(config.refreshPeriodMs()))
            // Fail fast rather than queue — a caller finding out immediately
            // that this call was skipped is more honest than silently
            // blocking a request thread waiting for quota to free up.
            .timeoutDuration(Duration.ZERO)
            .build()));
        var circuitBreaker = circuitBreakers.computeIfAbsent(name, key -> CircuitBreaker.of(key, CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .build()));

        return CircuitBreaker.decorateSupplier(circuitBreaker, RateLimiter.decorateSupplier(rateLimiter, action));
    }
}
