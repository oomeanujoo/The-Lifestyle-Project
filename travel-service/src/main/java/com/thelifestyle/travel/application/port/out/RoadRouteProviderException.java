package com.thelifestyle.travel.application.port.out;

// A genuine road-routing provider failure — never masked as "no route
// exists," which looks identical to a real NoRoute response otherwise.
// `reason` is a short, fixed category (TIMEOUT, NO_ROUTE, RATE_LIMITED,
// INVALID_COORDINATES, PROVIDER_ERROR) so a caller can react differently
// to "try again later" vs. "this pair genuinely has no road route."
public class RoadRouteProviderException extends RuntimeException {
    private final String reason;

    public RoadRouteProviderException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public RoadRouteProviderException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
