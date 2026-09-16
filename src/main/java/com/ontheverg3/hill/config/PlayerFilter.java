package com.ontheverg3.hill.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class PlayerFilter {
    private final boolean excludeDead;
    private final boolean excludeSpectator;
    private final boolean excludeCreative;
    private final List<GameMode> excludeGamemodes;

    public PlayerFilter(
            boolean excludeDead,
            boolean excludeSpectator,
            boolean excludeCreative,
            List<GameMode> excludeGamemodes) {
        this.excludeDead = excludeDead;
        this.excludeSpectator = excludeSpectator;
        this.excludeCreative = excludeCreative;
        this.excludeGamemodes = List.copyOf(excludeGamemodes == null ? List.of() : excludeGamemodes);
    }

    public static PlayerFilter load(
            FileConfiguration yaml, String path, boolean defaultDead, boolean defaultSpectator, boolean defaultCreative)
            throws ConfigException {
        boolean dead = yaml.getBoolean(path + ".exclude-dead", defaultDead);
        boolean spectator = yaml.getBoolean(path + ".exclude-spectator", defaultSpectator);
        boolean creative = yaml.getBoolean(path + ".exclude-creative", defaultCreative);
        List<GameMode> modes = new ArrayList<>();
        for (String raw : yaml.getStringList(path + ".exclude-gamemodes")) {
            try {
                modes.add(GameMode.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new ConfigException(path + ".exclude-gamemodes: unknown gamemode " + raw);
            }
        }
        return new PlayerFilter(dead, spectator, creative, modes);
    }

    public boolean excluded(Player player) {
        if (player == null || !player.isOnline()) {
            return true;
        }
        if (excludeDead && player.isDead()) {
            return true;
        }
        GameMode mode = player.getGameMode();
        if (excludeSpectator && mode == GameMode.SPECTATOR) {
            return true;
        }
        if (excludeCreative && mode == GameMode.CREATIVE) {
            return true;
        }
        return excludeGamemodes.contains(mode);
    }

    public boolean excludeDead() {
        return excludeDead;
    }

    public boolean excludeSpectator() {
        return excludeSpectator;
    }

    public boolean excludeCreative() {
        return excludeCreative;
    }

    public List<GameMode> excludeGamemodes() {
        return excludeGamemodes;
    }
}
