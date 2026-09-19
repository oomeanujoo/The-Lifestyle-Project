package com.thelifestyle.integration.adapter.out.resilience;

// Thrown by ResilienceGuard.callOrThrow and by clients that detect an
// error-shaped 200 OK response (GeoNames does this — see GeoNamesClient) —
// the whole point is that a genuine provider failure must never look
// identical to "the provider was asked and found nothing." Callers that
// need a refresh job to record an honest FAILED outcome, not a masked
// empty-success, should let this propagate rather than catching it into a
// fallback value.
public class ExternalProviderException extends RuntimeException {
    public ExternalProviderException(String provider, String reason) {
        super("[" + provider + "] " + reason);
    }

    public ExternalProviderException(String provider, String reason, Throwable cause) {
        super("[" + provider + "] " + reason, cause);
    }
}
