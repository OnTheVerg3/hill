package com.ontheverg3.hill.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BossBarModeTest {
    @Test
    void parseAcceptsBooleanAndAlwaysOn() throws Exception {
        assertEquals(BossBarMode.ON, BossBarMode.parse(null));
        assertEquals(BossBarMode.ON, BossBarMode.parse(true));
        assertEquals(BossBarMode.OFF, BossBarMode.parse(false));
        assertEquals(BossBarMode.ON, BossBarMode.parse("on"));
        assertEquals(BossBarMode.OFF, BossBarMode.parse("off"));
        assertEquals(BossBarMode.ALWAYS, BossBarMode.parse("alwayson"));
        assertEquals(BossBarMode.ALWAYS, BossBarMode.parse("always-on"));
        assertEquals(BossBarMode.ALWAYS, BossBarMode.parse("always"));
        assertTrue(BossBarMode.ALWAYS.enabled());
        assertTrue(BossBarMode.ALWAYS.always());
        assertFalse(BossBarMode.ON.always());
        assertEquals("always", BossBarMode.ALWAYS.yamlValue());
    }

    @Test
    void parseRejectsUnknownTokens() {
        assertThrows(ConfigException.class, () -> BossBarMode.parse("sometimes"));
    }
}
