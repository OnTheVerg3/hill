package com.ontheverg3.hill.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

class TeamLooksTest {
    @Test
    void namedMinecraftColorsAndAliases() throws Exception {
        TeamLooks looks = TeamLooks.of("Red", "red", "Aqua", "dark_aqua");
        assertEquals(NamedTextColor.RED, looks.team1().color());
        assertEquals(NamedTextColor.DARK_AQUA, looks.team2().color());
        assertEquals(NamedTextColor.RED, TeamLooks.parseColor("red"));
        assertEquals(NamedTextColor.RED, TeamLooks.parseColor("RED"));
        assertEquals(NamedTextColor.LIGHT_PURPLE, TeamLooks.parseColor("pink"));
        assertEquals(NamedTextColor.DARK_BLUE, TeamLooks.parseColor("dark blue"));
        assertEquals(NamedTextColor.GRAY, TeamLooks.parseColor("grey"));
    }

    @Test
    void hexCodesExpandAndNormalize() throws Exception {
        TeamLooks looks = TeamLooks.of("Crimson", "#F00", "Teal", "0x55FFFF");
        assertEquals(0xFF0000, looks.team1().color().value());
        assertEquals(0x55FFFF, looks.team2().color().value());
        assertEquals("#ff0000", looks.team1().hex());
        assertEquals(0x112233, TeamLooks.parseColor("112233").value());
    }

    @Test
    void rejectsBlankDuplicateAndCollidingNames() {
        assertThrows(ConfigException.class, () -> TeamLooks.of(" ", "blue", "Yellow", "yellow"));
        assertThrows(ConfigException.class, () -> TeamLooks.of("Blue", "blue", "blue", "yellow"));
        assertThrows(ConfigException.class, () -> TeamLooks.of("yellow", "blue", "Red", "red"));
        assertThrows(ConfigException.class, () -> TeamLooks.of("Red", "blue", "team1", "red"));
        assertThrows(ConfigException.class, () -> TeamLooks.of("Nope", "not-a-color", "Yellow", "yellow"));
    }

    @Test
    void bossBarPicksNearestVanillaColor() throws Exception {
        TeamLooks looks = TeamLooks.of("Blue", "blue", "Gold", "#FFAA00");
        assertEquals(BossBar.Color.BLUE, looks.team1().bossBarColor());
        assertEquals(BossBar.Color.YELLOW, looks.team2().bossBarColor());
        assertTrue(looks.team1().display().equals("Blue"));
    }
}
