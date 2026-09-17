package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
