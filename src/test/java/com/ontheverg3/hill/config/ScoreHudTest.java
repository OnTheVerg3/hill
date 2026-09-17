package com.ontheverg3.hill.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScoreHudTest {
    @Test
    void parsesAlwaysAndInZone() throws Exception {
        assertEquals(ScoreHud.ALWAYS, ScoreHud.parse("always"));
        assertEquals(ScoreHud.ALWAYS, ScoreHud.parse("EVERYWHERE"));
        assertEquals(ScoreHud.IN_ZONE, ScoreHud.parse("in-zone"));
        assertEquals(ScoreHud.IN_ZONE, ScoreHud.parse("zone"));
        assertEquals(ScoreHud.IN_ZONE, ScoreHud.parse(""));
        assertTrue(ScoreHud.ALWAYS.always());
        assertTrue(ScoreHud.ALWAYS.visible(false));
        assertTrue(ScoreHud.ALWAYS.visible(true));
        assertFalse(ScoreHud.IN_ZONE.visible(false));
        assertTrue(ScoreHud.IN_ZONE.visible(true));
    }

    @Test
    void rejectsUnknownTokens() {
        assertThrows(ConfigException.class, () -> ScoreHud.parse("sometimes"));
    }
}
