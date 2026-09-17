package com.ontheverg3.hill.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class PositionsTest {
    @Test
    void lookOnlyIsNotATranslation() {
        Location from = new Location(null, 3.2, 64.0, 0.0, 0.0f, 0.0f);
        Location to = new Location(null, 3.2, 64.0, 0.0, 90.0f, 12.0f);
        assertFalse(Positions.translation(from, to));
    }

    @Test
    void sameBlockStepIsATranslation() {
        Location from = new Location(null, 3.20, 64.0, 0.0);
        Location to = new Location(null, 3.30, 64.0, 0.0);
        assertTrue(Positions.translation(from, to));
        assertTrue(Math.floor(from.getX()) == Math.floor(to.getX()));
    }
}
