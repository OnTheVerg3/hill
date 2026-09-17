package com.ontheverg3.hill.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PlayerFilterTest {
    @Test
    void missingGamemodeListUsesDefaults() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("eligibility.exclude-dead", true);
        PlayerFilter filter =
                PlayerFilter.load(yaml, "eligibility", true, List.of(GameMode.SPECTATOR));
        assertTrue(filter.excludeDead());
        assertEquals(List.of(GameMode.SPECTATOR), filter.excludeGamemodes());
    }

    @Test
    void presentGamemodeListDoesNotAddSpectatorOrCreativeFlags() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("assign.exclude-dead", false);
        yaml.set("assign.exclude-gamemodes", List.of("ADVENTURE"));
        PlayerFilter filter =
                PlayerFilter.load(yaml, "assign", true, List.of(GameMode.SPECTATOR, GameMode.CREATIVE));
        assertFalse(filter.excludeDead());
        assertEquals(List.of(GameMode.ADVENTURE), filter.excludeGamemodes());
    }

    @Test
    void unknownGamemodeIsRejected() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("eligibility.exclude-gamemodes", List.of("FLYING"));
        assertThrows(
                ConfigException.class,
                () -> PlayerFilter.load(yaml, "eligibility", true, List.of(GameMode.SPECTATOR)));
    }
}
