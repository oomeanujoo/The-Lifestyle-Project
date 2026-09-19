package com.thelifestyle.integration.domain;

// One master's outcome from a single refresh run — logged as-is into
// integration.master_refresh_log (§17), never silently dropped even on
// failure. FAILED means the outcome came from an ExternalProviderException,
// not from a genuinely empty successful response — see GeoNamesClient/
// FrankfurterClient's explicit-failure handling.
public record MasterRefreshOutcome(String masterName, String status, int recordsUpserted, String errorMessage) {
    public static MasterRefreshOutcome success(String masterName, int recordsUpserted) {
        return new MasterRefreshOutcome(masterName, "SUCCESS", recordsUpserted, null);
    }

    public static MasterRefreshOutcome partial(String masterName, int recordsUpserted, String errorMessage) {
        return new MasterRefreshOutcome(masterName, "PARTIAL", recordsUpserted, errorMessage);
    }

    public static MasterRefreshOutcome failed(String masterName, String errorMessage) {
        return new MasterRefreshOutcome(masterName, "FAILED", 0, errorMessage);
    }
}
