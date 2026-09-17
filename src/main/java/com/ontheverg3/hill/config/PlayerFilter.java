package com.ontheverg3.hill.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class PlayerFilter {
    private final boolean excludeDead;
    private final List<GameMode> excludeGamemodes;

    public PlayerFilter(boolean excludeDead, List<GameMode> excludeGamemodes) {
        this.excludeDead = excludeDead;
        this.excludeGamemodes = List.copyOf(excludeGamemodes == null ? List.of() : excludeGamemodes);
    }

    public static PlayerFilter load(
            FileConfiguration yaml, String path, boolean defaultDead, List<GameMode> defaultGamemodes)
            throws ConfigException {
        boolean dead = yaml.getBoolean(path + ".exclude-dead", defaultDead);
        List<GameMode> modes;
        if (yaml.contains(path + ".exclude-gamemodes")) {
            modes = new ArrayList<>();
            for (String raw : yaml.getStringList(path + ".exclude-gamemodes")) {
                try {
                    modes.add(GameMode.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    throw new ConfigException(path + ".exclude-gamemodes: unknown gamemode " + raw);
                }
            }
        } else {
            modes = defaultGamemodes == null ? List.of() : defaultGamemodes;
        }
        return new PlayerFilter(dead, modes);
    }

    public boolean excluded(Player player) {
        if (player == null || !player.isOnline()) {
            return true;
        }
        if (excludeDead && player.isDead()) {
            return true;
        }
        return excludeGamemodes.contains(player.getGameMode());
    }

    public boolean excludeDead() {
        return excludeDead;
    }

    public List<GameMode> excludeGamemodes() {
        return excludeGamemodes;
    }
}
