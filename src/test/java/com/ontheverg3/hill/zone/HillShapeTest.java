package com.ontheverg3.hill.zone;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
