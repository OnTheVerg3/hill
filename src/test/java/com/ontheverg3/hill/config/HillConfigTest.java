package com.ontheverg3.hill.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
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
        HillConfig always = on.withBossBarMode(BossBarMode.ALWAYS);
        assertTrue(always.bossBarAlways());
        assertTrue(always.bossBar());
        assertFalse(on.bossBarAlways());
    }

    @Test
    void loadAcceptsAlwaysBossBar() throws Exception {
        YamlConfiguration yaml = baseConfig();
        yaml.set("display.boss-bar", "always");
        HillConfig loaded = HillConfig.load(yaml);
        assertTrue(loaded.bossBar());
        assertTrue(loaded.bossBarAlways());
        yaml.set("display.boss-bar", true);
        HillConfig on = HillConfig.load(yaml);
        assertTrue(on.bossBar());
        assertFalse(on.bossBarAlways());
    }

    @Test
    void loadRejectsNonFiniteScoringInterval() {
        YamlConfiguration yaml = baseConfig();
        yaml.set("scoring.interval-seconds", Double.NaN);
        assertThrows(ConfigException.class, () -> HillConfig.load(yaml));
        yaml.set("scoring.interval-seconds", Double.POSITIVE_INFINITY);
        assertThrows(ConfigException.class, () -> HillConfig.load(yaml));
    }

    private static YamlConfiguration baseConfig() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("locale", "en");
        yaml.set("scoring.interval-seconds", 5);
        yaml.set("scoring.points", 1);
        yaml.set("scoring.win-score", 0);
        yaml.set("display.update-ticks", 20);
        return yaml;
    }
}
