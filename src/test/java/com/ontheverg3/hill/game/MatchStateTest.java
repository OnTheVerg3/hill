package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MatchStateTest {
    @Test
    void pausePreservesScoresAndBlocksNothingInState() {
        MatchState match = new MatchState();
        match.award(TeamId.BLUE, 3, 0);
        match.award(TeamId.YELLOW, 1, 0);
        assertTrue(match.pause());
        assertTrue(match.paused());
        assertFalse(match.pause());
        assertEquals(3, match.score(TeamId.BLUE));
        assertEquals(1, match.score(TeamId.YELLOW));
        assertTrue(match.resume());
        assertFalse(match.paused());
    }

    @Test
    void swapScoresExchangesBlueAndYellow() {
        MatchState match = new MatchState();
        match.setScore(TeamId.BLUE, 7);
        match.setScore(TeamId.YELLOW, 2);
        match.swapScores();
        assertEquals(2, match.score(TeamId.BLUE));
        assertEquals(7, match.score(TeamId.YELLOW));
    }

    @Test
    void addScoreDoesNotGoBelowZero() {
        MatchState match = new MatchState();
        match.setScore(TeamId.BLUE, 3);
        match.addScore(TeamId.BLUE, -10);
        assertEquals(0, match.score(TeamId.BLUE));
    }

    @Test
    void resetScoresKeepsPauseFlagUntouchedUntilResetScoresOnly() {
        MatchState match = new MatchState();
        match.award(TeamId.YELLOW, 4, 0);
        match.resetScores();
        assertEquals(0, match.score(TeamId.YELLOW));
        assertEquals(0, match.score(TeamId.BLUE));
    }
}
