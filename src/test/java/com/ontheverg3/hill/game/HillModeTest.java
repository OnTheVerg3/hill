package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HillModeTest {
    @Test
    void parsesAliases() {
        assertEquals(HillMode.KOTH, HillMode.parse("koth").orElseThrow());
        assertEquals(HillMode.KOTH, HillMode.parse("KingOfTheHill").orElseThrow());
        assertEquals(HillMode.CTF, HillMode.parse("ctf").orElseThrow());
        assertEquals(HillMode.CTF, HillMode.parse("capture-the-flag").orElseThrow());
    }
}
