package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MatchStateTest {
    @Test
    void pausePreservesPointState() {
        MatchState match = new MatchState();
        match.setPointState(PointState.CONTROLLED_BLUE);
        assertTrue(match.pause());
        assertTrue(match.paused());
        assertFalse(match.pause());
        assertEquals(PointState.CONTROLLED_BLUE, match.pointState());
        assertTrue(match.resume());
        assertFalse(match.paused());
    }
}
