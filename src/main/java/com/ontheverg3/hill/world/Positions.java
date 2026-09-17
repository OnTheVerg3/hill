package com.ontheverg3.hill.world;

import org.bukkit.Location;

/** Exact player position helpers. Capture zones use doubles, not block cells. */
public final class Positions {
    private Positions() {}

    public static boolean translation(Location from, Location to) {
        if (to == null) {
            return false;
        }
        if (from == null) {
            return true;
        }
        if (from.getWorld() != to.getWorld()) {
            return true;
        }
        return from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
    }
}
