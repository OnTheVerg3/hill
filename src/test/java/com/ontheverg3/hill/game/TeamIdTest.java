package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ontheverg3.hill.config.TeamLooks;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TeamIdTest {
    @Test
    void parseSlotAliases() {
        assertEquals(Optional.of(TeamId.BLUE), TeamId.parse("blue"));
        assertEquals(Optional.of(TeamId.BLUE), TeamId.parse("B"));
        assertEquals(Optional.of(TeamId.BLUE), TeamId.parse("team1"));
        assertEquals(Optional.of(TeamId.YELLOW), TeamId.parse("2"));
    }

    @Test
    void parseYellowAliases() {
        assertEquals(Optional.of(TeamId.YELLOW), TeamId.parse("yellow"));
        assertEquals(Optional.of(TeamId.YELLOW), TeamId.parse("y"));
    }

    @Test
    void parseConfiguredDisplayNames() throws Exception {
        var looks = TeamLooks.of("Crimson", "red", "Teal", "aqua");
        assertEquals(Optional.of(TeamId.BLUE), TeamId.parse("Crimson", looks));
        assertEquals(Optional.of(TeamId.YELLOW), TeamId.parse("teal", looks));
        assertEquals(Optional.of(TeamId.BLUE), TeamId.parse("blue", looks));
    }

    @Test
    void parseRejectsGarbage() {
        assertTrue(TeamId.parse("green").isEmpty());
        assertTrue(TeamId.parse(null).isEmpty());
    }
}
