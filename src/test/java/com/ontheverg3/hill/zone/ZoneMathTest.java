package com.ontheverg3.hill.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZoneMathTest {
    @Test
    void cuboidCoverRadiusCoversCornersWithPadding() {
        double radius = ZoneMath.cuboidCoverRadius(0, 0, 0, 8, 16, 8);
        double corner = Math.sqrt(4 * 4 + 8 * 8 + 4 * 4);
        assertTrue(radius >= corner + 2.0 - 1e-9);
        assertEquals(corner + 2.0, radius, 1e-9);
    }

    @Test
    void sphereCoverRadiusAddsPadding() {
        assertEquals(7.0, ZoneMath.sphereCoverRadius(5.0), 1e-9);
    }

    @Test
    void tileAnchorsWithoutWorldAreEmpty() {
        assertEquals(0, ZoneMath.tileAnchors(null, 0, 0, 0, 8, 16, 8, 128).length);
    }

    @Test
    void smallCuboidFitsOneFoliaRegion() {
        RegionSample[] samples = ZoneMath.regionSamples(null, 0, 0, 0, 8, 16, 8);
        assertEquals(1, samples.length);
        RegionSample sample = samples[0];
        assertEquals(4.0, sample.anchor().getX(), 1e-9);
        assertEquals(4.0, sample.anchor().getZ(), 1e-9);
        double corner = Math.sqrt(4 * 4 + 8 * 8 + 4 * 4);
        assertTrue(sample.radius() >= corner + 2.0 - 1e-9);
    }

    @Test
    void eventCuboidUsesFourFoliaRegions() {
        RegionSample[] samples = ZoneMath.regionSamples(null, -192, -64, -192, 192, 32, 192);
        assertEquals(4, samples.length);
        for (RegionSample sample : samples) {
            assertTrue(sample.radius() > 0.0);
            assertTrue(Math.abs(sample.anchor().getX()) <= 192.0);
            assertTrue(Math.abs(sample.anchor().getZ()) <= 192.0);
        }
    }

    @Test
    void regionCoordMatchesFolia256Grid() {
        assertEquals(0, ZoneMath.regionCoord(0));
        assertEquals(0, ZoneMath.regionCoord(192));
        assertEquals(-1, ZoneMath.regionCoord(-1));
        assertEquals(-1, ZoneMath.regionCoord(-192));
    }
}
