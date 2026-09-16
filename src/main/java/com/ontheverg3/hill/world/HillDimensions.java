package com.ontheverg3.hill.world;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

public final class HillDimensions {
    public static final String OVERWORLD = HillIds.OVERWORLD;
    public static final String NETHER = HillIds.NETHER;
    public static final String THE_END = HillIds.THE_END;

    private HillDimensions() {}

    public static String saveName(World world) {
        if (world == null) {
            return "";
        }
        return HillIds.saveName(world.getName());
    }

    public static String saveName(String worldFolderName) {
        return HillIds.saveName(worldFolderName);
    }

    public static String dimensionToken(World world) {
        if (world == null) {
            return OVERWORLD;
        }
        NamespacedKey key = world.getKey();
        return HillIds.dimensionToken(
                world.getEnvironment() == null ? "CUSTOM" : world.getEnvironment().name(),
                key == null ? "minecraft" : key.getNamespace(),
                key == null ? world.getName() : key.getKey());
    }

    public static String sanitizeId(String raw) {
        return HillIds.sanitizeId(raw);
    }

    public static boolean isId(String token) {
        return HillIds.isId(token);
    }

    public static World worldOrNull(String save, String dimensionId) {
        if (save == null || dimensionId == null) {
            return null;
        }
        for (World world : Bukkit.getWorlds()) {
            if (save.equals(saveName(world)) && dimensionId.equals(dimensionToken(world))) {
                return world;
            }
        }
        return null;
    }
}
