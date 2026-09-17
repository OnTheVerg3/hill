package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScoreBarTest {
    @Test
    void leaderFillsTheirHalfWhenUncapped() {
        ScoreBar.Fill fill = ScoreBar.fill(12, 4, 20, 0);
        assertEquals(20, fill.width());
        assertEquals(10, fill.leftFilled());
        assertEquals(3, fill.rightFilled());
    }

    @Test
    void bothEmptyStaysEmpty() {
        ScoreBar.Fill fill = ScoreBar.fill(0, 0, 24, 0);
        assertEquals(0, fill.leftFilled());
        assertEquals(0, fill.rightFilled());
        assertEquals(24, fill.width());
    }

    @Test
    void winScoreCapsEachSideIndependently() {
        ScoreBar.Fill fill = ScoreBar.fill(25, 75, 20, 100);
        assertEquals(3, fill.leftFilled());
        assertEquals(8, fill.rightFilled());
    }

    @Test
    void oddWidthRoundsUpToEven() {
        ScoreBar.Fill fill = ScoreBar.fill(1, 1, 15, 1);
        assertEquals(16, fill.width());
        assertEquals(8, fill.leftFilled());
        assertEquals(8, fill.rightFilled());
    }
}
