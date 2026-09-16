package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PointStateTest {
    @Test
    void exclusiveBlueControls() {
        PointState state = PointState.fromPresence(true, false);
        assertEquals(PointState.CONTROLLED_BLUE, state);
        assertTrue(state.awardsPoints());
        assertEquals(TeamId.BLUE, state.controllingTeamOrNull());
    }

    @Test
    void exclusiveYellowControls() {
        PointState state = PointState.fromPresence(false, true);
        assertEquals(PointState.CONTROLLED_YELLOW, state);
        assertTrue(state.awardsPoints());
        assertEquals(TeamId.YELLOW, state.controllingTeamOrNull());
    }

    @Test
    void bothTeamsContestAndAwardNothing() {
        PointState state = PointState.fromPresence(true, true);
        assertEquals(PointState.CONTESTED, state);
        assertFalse(state.awardsPoints());
        assertNull(state.controllingTeamOrNull());
    }

    @Test
    void neitherTeamIsEmpty() {
        PointState state = PointState.fromPresence(false, false);
        assertEquals(PointState.EMPTY, state);
        assertFalse(state.awardsPoints());
        assertNull(state.controllingTeamOrNull());
    }
}
