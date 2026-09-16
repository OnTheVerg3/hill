package com.ontheverg3.hill.zone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class UnusableZone implements CaptureZone {
    private final String reason;

    public UnusableZone(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean usable() {
        return false;
    }

    @Override
    public String unusableReason() {
        return reason;
    }

    @Override
    public World worldOrNull() {
        return null;
    }

    @Override
    public Location schedulerAnchor() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            return null;
        }
        return world.getSpawnLocation();
    }

    @Override
    public boolean contains(Location location) {
        return false;
    }

    @Override
    public double coverRadius() {
        return 0.0;
    }

    @Override
    public Location[] regionProbePoints() {
        Location anchor = schedulerAnchor();
        return anchor == null ? new Location[0] : new Location[] {anchor};
    }

    @Override
    public double minX() {
        return 0.0;
    }

    @Override
    public double minY() {
        return 0.0;
    }

    @Override
    public double minZ() {
        return 0.0;
    }

    @Override
    public double maxX() {
        return 0.0;
    }

    @Override
    public double maxY() {
        return 0.0;
    }

    @Override
    public double maxZ() {
        return 0.0;
    }
}
