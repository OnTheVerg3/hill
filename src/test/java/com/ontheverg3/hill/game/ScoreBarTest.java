package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScoreBarTest {
    @Test
    void fillIsTheScoreRatio() {
        ScoreBar.Fill fill = ScoreBar.fill(12, 4, 20);
        assertEquals(20, fill.width());
        assertEquals(15, fill.leftFilled());
        assertEquals(5, fill.rightFilled());
        assertEquals(0, fill.empty());
    }

    @Test
    void bothEmptyStaysEmpty() {
        ScoreBar.Fill fill = ScoreBar.fill(0, 0, 24);
        assertEquals(0, fill.leftFilled());
        assertEquals(0, fill.rightFilled());
        assertEquals(24, fill.width());
        assertEquals(24, fill.empty());
    }

    @Test
    void onlyOneTeamFillsTheBar() {
        ScoreBar.Fill fill = ScoreBar.fill(7, 0, 24);
        assertEquals(24, fill.leftFilled());
        assertEquals(0, fill.rightFilled());
    }

    @Test
    void equalScoresSplitTheBarEvenly() {
        ScoreBar.Fill fill = ScoreBar.fill(25, 25, 20);
        assertEquals(10, fill.leftFilled());
        assertEquals(10, fill.rightFilled());
        assertEquals(0, fill.empty());
    }

    @Test
    void oddWidthRoundsUpToEven() {
        ScoreBar.Fill fill = ScoreBar.fill(1, 1, 15);
        assertEquals(16, fill.width());
        assertEquals(8, fill.leftFilled());
        assertEquals(8, fill.rightFilled());
    }

    @Test
    void trackProgressIsTheLeadersShare() {
        assertEquals(0.0f, ScoreBar.trackProgress(0, 0));
        assertEquals(1.0f, ScoreBar.trackProgress(7, 0));
        assertEquals(0.5f, ScoreBar.trackProgress(10, 10));
        assertEquals(22 / 40.0f, ScoreBar.trackProgress(18, 22));
    }
}
