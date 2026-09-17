package com.ontheverg3.hill.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HillConfigTest {
    @Test
    void withBossBarDoesNotMutateTheOriginal() {
        HillConfig on = HillConfig.fallback("test");
        assertTrue(on.bossBar());
        HillConfig off = on.withBossBar(false);
        assertFalse(off.bossBar());
        assertTrue(on.bossBar());
        assertEquals(on.actionBar(), off.actionBar());
        assertEquals(on.bossBarWidth(), off.bossBarWidth());
        assertEquals(on.scoresHud(), off.scoresHud());
    }
}
