package com.ontheverg3.hill.zone;

import org.bukkit.Location;
import org.bukkit.World;

public final class CuboidZone implements CaptureZone {
    private final World world;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    public CuboidZone(World world, Location a, Location b) {
        this.world = world;
        this.minX = Math.min(a.getX(), b.getX());
        this.minY = Math.min(a.getY(), b.getY());
        this.minZ = Math.min(a.getZ(), b.getZ());
        this.maxX = Math.max(a.getX(), b.getX());
        this.maxY = Math.max(a.getY(), b.getY());
        this.maxZ = Math.max(a.getZ(), b.getZ());
    }

    @Override
    public boolean usable() {
        return world != null;
    }

    @Override
    public String unusableReason() {
        return usable() ? "" : "world missing";
    }

    @Override
    public World worldOrNull() {
        return world;
    }

    @Override
    public Location schedulerAnchor() {
        return new Location(world, (minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
    }

    @Override
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || world == null) {
            return false;
        }
        if (!location.getWorld().equals(world)) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    @Override
    public double coverRadius() {
        return ZoneMath.cuboidCoverRadius(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public Location[] regionProbePoints() {
        return new Location[] {
            new Location(world, minX, minY, minZ),
            new Location(world, minX, minY, maxZ),
            new Location(world, minX, maxY, minZ),
            new Location(world, minX, maxY, maxZ),
            new Location(world, maxX, minY, minZ),
            new Location(world, maxX, minY, maxZ),
            new Location(world, maxX, maxY, minZ),
            new Location(world, maxX, maxY, maxZ)
        };
    }

    @Override
    public double minX() {
        return minX;
    }

    @Override
    public double minY() {
        return minY;
    }

    @Override
    public double minZ() {
        return minZ;
    }

    @Override
    public double maxX() {
        return maxX;
    }

    @Override
    public double maxY() {
        return maxY;
    }

    @Override
    public double maxZ() {
        return maxZ;
    }
}
