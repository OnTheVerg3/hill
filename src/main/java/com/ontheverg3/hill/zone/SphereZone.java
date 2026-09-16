package com.ontheverg3.hill.zone;

import org.bukkit.Location;
import org.bukkit.World;

public final class SphereZone implements CaptureZone {
    private final World world;
    private final Location center;
    private final double radiusSquared;

    public SphereZone(World world, Location center, double radius) {
        this.world = world;
        this.center = center.clone();
        this.radiusSquared = radius * radius;
    }

    @Override
    public boolean usable() {
        return world != null && center != null && radiusSquared >= 0.0;
    }

    @Override
    public String unusableReason() {
        return usable() ? "" : "sphere incomplete";
    }

    @Override
    public World worldOrNull() {
        return world;
    }

    @Override
    public Location schedulerAnchor() {
        return center.clone();
    }

    @Override
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || world == null) {
            return false;
        }
        if (!location.getWorld().equals(world)) {
            return false;
        }
        return location.distanceSquared(center) <= radiusSquared;
    }

    @Override
    public double coverRadius() {
        return ZoneMath.sphereCoverRadius(Math.sqrt(radiusSquared));
    }

    @Override
    public Location[] regionProbePoints() {
        double radius = Math.sqrt(radiusSquared);
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        return new Location[] {
            new Location(world, x - radius, y - radius, z - radius),
            new Location(world, x - radius, y - radius, z + radius),
            new Location(world, x - radius, y + radius, z - radius),
            new Location(world, x - radius, y + radius, z + radius),
            new Location(world, x + radius, y - radius, z - radius),
            new Location(world, x + radius, y - radius, z + radius),
            new Location(world, x + radius, y + radius, z - radius),
            new Location(world, x + radius, y + radius, z + radius)
        };
    }

    @Override
    public double minX() {
        return center.getX() - Math.sqrt(radiusSquared);
    }

    @Override
    public double minY() {
        return center.getY() - Math.sqrt(radiusSquared);
    }

    @Override
    public double minZ() {
        return center.getZ() - Math.sqrt(radiusSquared);
    }

    @Override
    public double maxX() {
        return center.getX() + Math.sqrt(radiusSquared);
    }

    @Override
    public double maxY() {
        return center.getY() + Math.sqrt(radiusSquared);
    }

    @Override
    public double maxZ() {
        return center.getZ() + Math.sqrt(radiusSquared);
    }
}
