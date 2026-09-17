package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ontheverg3.hill.zone.HillShape;
import com.ontheverg3.hill.zone.HillSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutlineServiceTest {
    @Test
    void circleFootprintStaysOnTheRadius() {
        HillSpec spec = spec(HillShape.CIRCLE, 32, 0, 8);
        List<double[]> points = OutlineService.footprint(spec, 72);
        assertEquals(72, points.size());
        for (double[] point : points) {
            assertEquals(8.0, Math.hypot(point[0] - 32.0, point[1] - 0.0), 1e-9);
        }
    }

    @Test
    void squareFootprintStaysOnTheRectangle() {
        HillSpec spec = spec(HillShape.SQUARE, 64, 0, 8);
        List<double[]> points = OutlineService.footprint(spec, 40);
        assertEquals(40, points.size());
        for (double[] point : points) {
            double dx = Math.abs(point[0] - 64.0);
            double dz = Math.abs(point[1] - 0.0);
            boolean onX = Math.abs(dx - 8.0) < 1e-9 && dz <= 8.0 + 1e-9;
            boolean onZ = Math.abs(dz - 8.0) < 1e-9 && dx <= 8.0 + 1e-9;
            assertTrue(onX || onZ, () -> point[0] + "," + point[1]);
        }
    }

    @Test
    void groundParticlesSitAboveTheBlockTop() {
        assertEquals(-59.88, OutlineService.spawnY(-60.5, false, -60.0), 1e-9);
    }

    @Test
    void airColumnsStayAtTheHillY() {
        assertEquals(-42.5, OutlineService.spawnY(-42.5, true, -61.0), 1e-9);
    }

    private static HillSpec spec(HillShape shape, double x, double z, double radius) {
        return new HillSpec("id", "id", "world", "save", "overworld", shape, x, -60.5, z, radius, 16, radius);
    }
}
