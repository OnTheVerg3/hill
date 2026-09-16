package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TeamIdTest {
    @Test
    void parseBlueAliases() {
        assertEquals(Optional.of(TeamId.BLUE), TeamId.parse("blue"));
        assertEquals(Optional.of(TeamId.BLUE), TeamId.parse("B"));
    }

    @Test
    void parseYellowAliases() {
        assertEquals(Optional.of(TeamId.YELLOW), TeamId.parse("yellow"));
        assertEquals(Optional.of(TeamId.YELLOW), TeamId.parse("y"));
    }

    @Test
    void parseRejectsGarbage() {
        assertTrue(TeamId.parse("green").isEmpty());
        assertTrue(TeamId.parse(null).isEmpty());
    }
}
