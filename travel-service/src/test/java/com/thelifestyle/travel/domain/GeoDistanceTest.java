package com.thelifestyle.travel.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pure-function correctness for the Haversine calculation this app relies
// on to label distances honestly as "approximate straight-line" — no
// mocking needed, no network involved.
class GeoDistanceTest {

    @Test void distanceFromAPointToItselfIsZero() {
        assertEquals(0.0, GeoDistance.haversineKm(18.5204, 73.8567, 18.5204, 73.8567), 0.0001);
    }

    @Test void distanceIsSymmetric() {
        var aToB = GeoDistance.haversineKm(18.5204, 73.8567, 19.0760, 72.8777);
        var bToA = GeoDistance.haversineKm(19.0760, 72.8777, 18.5204, 73.8567);
        assertEquals(aToB, bToA, 0.0001);
    }

    @Test void matchesTheKnownRealDistanceBetweenPuneAndMumbai() {
        // Pune (18.5204, 73.8567) to Mumbai (19.0760, 72.8777) — real
        // great-circle distance is well documented as ~119 km. A wide-ish
        // tolerance since this is checking the formula is right, not
        // pinning a specific decimal.
        var km = GeoDistance.haversineKm(18.5204, 73.8567, 19.0760, 72.8777);
        assertTrue(km > 110 && km < 130, "expected roughly 119km, got " + km);
    }

    @Test void round2RoundsToTwoDecimalPlacesDeterministically() {
        assertEquals(119.12, GeoDistance.round2(119.1234));
        assertEquals(119.13, GeoDistance.round2(119.126));
    }
}
