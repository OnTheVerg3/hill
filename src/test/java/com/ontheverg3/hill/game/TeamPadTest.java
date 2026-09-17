package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamPadTest {
    @Test
    void standingOnTheBlockIsInside() {
        TeamPad pad = new TeamPad(TeamId.BLUE, "world", "world", "overworld", -7, -61, -2, -4, -59, 3);
        assertTrue(pad.containsPoint(-6.5, -60.0, 0.0));
        assertTrue(pad.containsPoint(-7.0, -61.0, -2.0));
        assertFalse(pad.containsPoint(-4.0, -60.0, 0.0));
        assertFalse(pad.containsPoint(-6.5, -59.0, 0.0));
        assertFalse(pad.containsPoint(6.0, -60.0, 0.0));
    }

    @Test
    void nonFiniteBoundsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TeamPad(
                        TeamId.BLUE, "world", "world", "overworld", Double.NaN, 0, 0, 1, 1, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new TeamPad(
                        TeamId.YELLOW,
                        "world",
                        "world",
                        "overworld",
                        0,
                        0,
                        0,
                        Double.POSITIVE_INFINITY,
                        1,
                        1));
    }
}
