package com.thelifestyle.travel.domain;

// Pure, deterministic great-circle distance between two lat/lon points —
// no network call, no fabricated road route, no fare/time estimate. The
// Haversine formula is the standard choice for "how far apart are two
// points on a sphere" when only a straight-line measure is needed, which
// is exactly and only what the route-recommendation endpoint claims: it
// never represents an actual drivable/flyable route.
public final class GeoDistance {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoDistance() {}

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        var dLat = Math.toRadians(lat2 - lat1);
        var dLon = Math.toRadians(lon2 - lon1);
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // Presentation-only rounding, applied once at the edge of a calculation
    // — never affects the ranking math itself, which always uses the
    // unrounded value.
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
