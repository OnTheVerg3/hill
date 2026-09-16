package com.ontheverg3.hill.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PercentSplitTest {
    @Test
    void parsesPercentTokens() {
        var ratio = PercentSplit.parse("50%", "50%").orElseThrow();
        assertEquals(50.0, ratio.bluePercent(), 1e-9);
        assertEquals(50.0, ratio.yellowPercent(), 1e-9);
        assertTrue(PercentSplit.parse("70", "30").isPresent());
        assertTrue(PercentSplit.parse("33.3%", "66.7%").isPresent());
    }

    @Test
    void rejectsNonPercentAndBadSums() {
        assertFalse(PercentSplit.looksLike("assign"));
        assertFalse(PercentSplit.parse("40%", "40%").isPresent());
        assertFalse(PercentSplit.parse("101%", "0%").isPresent());
        assertFalse(PercentSplit.parse("blue", "50%").isPresent());
    }

    @Test
    void countsRoundToPlayerTotal() {
        assertArrayEquals(new int[] {50, 50}, PercentSplit.counts(100, PercentSplit.parse("50%", "50%").orElseThrow()));
        assertArrayEquals(new int[] {0, 5}, PercentSplit.counts(5, PercentSplit.parse("0%", "100%").orElseThrow()));
        assertArrayEquals(new int[] {7, 3}, PercentSplit.counts(10, PercentSplit.parse("70%", "30%").orElseThrow()));
        int[] odd = PercentSplit.counts(3, PercentSplit.parse("50%", "50%").orElseThrow());
        assertEquals(3, odd[0] + odd[1]);
    }

    @Test
    void complementFillsTheRest() {
        assertEquals("50%", PercentSplit.complement("50%"));
        assertEquals("30%", PercentSplit.complement("70"));
        assertEquals("0%", PercentSplit.complement("100%"));
    }
}
