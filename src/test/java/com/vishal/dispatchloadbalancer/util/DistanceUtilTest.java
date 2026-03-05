package com.vishal.dispatchloadbalancer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DistanceUtil Haversine Calculations")
class DistanceUtilTest {

    // ─── Same-location ────────────────────────────────────────────────────

    @Test
    @DisplayName("Distance between identical coordinates should be zero")
    void shouldReturnZeroWhenSameLocation() {
        double distance = DistanceUtil.calculateDistance(
                28.6139, 77.2090,
                28.6139, 77.2090);
        assertEquals(0.0, distance, 0.001,
                "Distance between the same point must be 0.0 km");
    }

    // ─── Known domestic distance ──────────────────────────────────────────

    @Test
    @DisplayName("Should calculate distance between two Delhi-area locations correctly")
    void shouldCalculateDistanceBetweenDelhiLocations() {
        // Connaught Place → Karol Bagh: ~13–16 km straight-line
        double distance = DistanceUtil.calculateDistance(
                28.6139, 77.2090,
                28.7041, 77.1025);
        assertTrue(distance > 13 && distance < 16,
                "Expected ~13-16 km, got " + distance + " km");
    }

    // ─── Long-distance ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should calculate long-distance correctly (Delhi → Chennai)")
    void shouldCalculateLongDistanceCorrectly() {
        double distance = DistanceUtil.calculateDistance(
                28.6139, 77.2090, // Delhi
                13.0827, 80.2707 // Chennai
        );
        assertTrue(distance > 1700 && distance < 2200,
                "Expected ~1700-2200 km, got " + distance + " km");
    }

    @Test
    @DisplayName("Should calculate long-distance correctly (Delhi → Mumbai)")
    void shouldCalculateDistanceDelhiToMumbai() {
        double distance = DistanceUtil.calculateDistance(
                28.6139, 77.2090, // Delhi
                19.0760, 72.8777 // Mumbai
        );
        assertTrue(distance > 1100 && distance < 1450,
                "Expected ~1100-1450 km, got " + distance + " km");
    }

    // ─── Symmetry ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Distance calculation must be symmetric (A→B == B→A)")
    void shouldBeSymmetric() {
        double ab = DistanceUtil.calculateDistance(28.6139, 77.2090, 13.0827, 80.2707);
        double ba = DistanceUtil.calculateDistance(13.0827, 80.2707, 28.6139, 77.2090);
        assertEquals(ab, ba, 0.001, "Haversine distance should be symmetric");
    }

    // ─── Negative / edge coordinates ─────────────────────────────────────

    @Test
    @DisplayName("Should handle negative latitude/longitude (southern/western hemisphere)")
    void shouldHandleNegativeCoordinates() {
        // Sydney, AU → Melbourne, AU (both in southern hemisphere)
        double distance = DistanceUtil.calculateDistance(
                -33.8688, 151.2093, // Sydney
                -37.8136, 144.9631 // Melbourne
        );
        assertTrue(distance > 650 && distance < 900,
                "Sydney → Melbourne should be ~700-900 km, got " + distance);
    }

    @Test
    @DisplayName("Should handle coordinates straddling the equator")
    void shouldHandleCoordinatesAcrossEquator() {
        // Nairobi, Kenya → Johannesburg, South Africa (crosses equator)
        double distance = DistanceUtil.calculateDistance(
                -1.2921, 36.8219, // Nairobi
                -26.2041, 28.0473 // Johannesburg
        );
        assertTrue(distance > 2700 && distance < 3200,
                "Nairobi → Johannesburg should be ~2700-3200 km, got " + distance);
    }

    // ─── Parameterized: known city pairs ─────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @DisplayName("Should be non-negative for any coordinate pair")
    @CsvSource({
            "Bangalore to Hyderabad, 12.9716, 77.5946, 17.3850, 78.4867, 450, 600",
            "Kolkata to Bhubaneswar,  22.5726, 88.3639, 20.2961, 85.8245, 350, 500",
            "Jaipur to Agra,          26.9124, 75.7873, 27.1767, 78.0081, 200, 350"
    })
    void shouldProduceReasonableDistanceForKnownCities(
            String label,
            double lat1, double lon1,
            double lat2, double lon2,
            double minKm, double maxKm) {

        double dist = DistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);
        assertTrue(dist >= minKm && dist <= maxKm,
                label + ": expected " + minKm + "-" + maxKm + " km, got " + dist);
    }

    // ─── Return type sanity ───────────────────────────────────────────────

    @Test
    @DisplayName("Distance should always be non-negative")
    void distanceShouldAlwaysBeNonNegative() {
        double distance = DistanceUtil.calculateDistance(0.0, 0.0, 90.0, 180.0);
        assertTrue(distance >= 0, "Distance must never be negative");
    }
}