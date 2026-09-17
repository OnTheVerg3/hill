package com.ontheverg3.hill.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HillShapeTest {
    @Test
    void squareIsAxisAlignedBox() {
        assertTrue(HillShape.contains(HillShape.SQUARE, 4, 0, 4, 5, 16, 5));
        assertFalse(HillShape.contains(HillShape.SQUARE, 6, 0, 0, 5, 16, 5));
    }

    @Test
    void circleIsHorizontalDiskWithHeight() {
        assertTrue(HillShape.contains(HillShape.CIRCLE, 3, 2, 4, 5, 16, 5));
        assertFalse(HillShape.contains(HillShape.CIRCLE, 5, 0, 5, 5, 16, 5));
        assertFalse(HillShape.contains(HillShape.CIRCLE, 0, 20, 0, 5, 16, 5));
    }

    @Test
    void sphereIsEllipsoid() {
        assertTrue(HillShape.contains(HillShape.SPHERE, 0, 0, 0, 5, 5, 5));
        assertFalse(HillShape.contains(HillShape.SPHERE, 5, 5, 5, 5, 5, 5));
    }

    @Test
    void cylinderIsCircleWithHeight() {
        assertTrue(HillShape.contains(HillShape.CYLINDER, 0, 8, 0, 5, 10, 5));
        assertFalse(HillShape.contains(HillShape.CYLINDER, 6, 0, 0, 5, 10, 5));
        assertFalse(HillShape.contains(HillShape.CYLINDER, 0, 12, 0, 5, 10, 5));
    }

    @Test
    void fractionalRadiusSplitsASingleBlock() {
        double rx = 3.25;
        assertTrue(HillShape.contains(HillShape.CIRCLE, 3.20, 0, 0, rx, 16, rx));
        assertFalse(HillShape.contains(HillShape.CIRCLE, 3.30, 0, 0, rx, 16, rx));
        assertEquals(3, (int) Math.floor(3.20));
        assertEquals(3, (int) Math.floor(3.30));
        assertTrue(HillShape.contains(HillShape.SQUARE, 6.40, 0, 0, 6.5, 16, 6.5));
        assertFalse(HillShape.contains(HillShape.SQUARE, 6.60, 0, 0, 6.5, 16, 6.5));
        assertEquals(6, (int) Math.floor(6.40));
        assertEquals(6, (int) Math.floor(6.60));
    }

    @Test
    void nonFiniteRadiusIsNeverInside() {
        assertFalse(HillShape.contains(HillShape.CIRCLE, 0, 0, 0, Double.NaN, 16, 8));
        assertFalse(HillShape.contains(HillShape.SQUARE, 0, 0, 0, Double.POSITIVE_INFINITY, 16, 8));
        assertFalse(HillShape.contains(HillShape.CIRCLE, Double.NaN, 0, 0, 8, 16, 8));
    }

    @Test
    void specRejectsNonFiniteGeometry() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HillSpec(
                        "mid",
                        "Mid",
                        "world",
                        "world",
                        "overworld",
                        HillShape.CIRCLE,
                        0,
                        0,
                        0,
                        Double.NaN,
                        16,
                        8));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HillSpec(
                        "mid",
                        "Mid",
                        "world",
                        "world",
                        "overworld",
                        HillShape.SQUARE,
                        0,
                        0,
                        0,
                        Double.POSITIVE_INFINITY,
                        16,
                        8));
    }
}
