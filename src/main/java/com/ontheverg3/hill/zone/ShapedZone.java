package com.ontheverg3.hill.zone;

import org.bukkit.Location;
import org.bukkit.World;

public final class ShapedZone implements CaptureZone {
    private final World world;
    private final HillSpec spec;

    public ShapedZone(World world, HillSpec spec) {
        this.world = world;
        this.spec = spec;
    }

    public HillSpec spec() {
        return spec;
    }

    @Override
    public boolean usable() {
        return world != null && spec != null && spec.rx() > 0 && spec.ry() > 0 && spec.rz() > 0;
    }

    @Override
    public String unusableReason() {
        if (spec == null) {
            return "hill missing";
        }
        if (world == null) {
            return "world not loaded: " + spec.world();
        }
        return usable() ? "" : "invalid size";
    }

    @Override
    public World worldOrNull() {
        return world;
    }

    @Override
    public Location schedulerAnchor() {
        return new Location(world, spec.x(), spec.y(), spec.z());
    }

    @Override
    public boolean contains(Location location) {
        if (!usable() || location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().equals(world)) {
            return false;
        }
        return HillShape.contains(
                spec.shape(),
                location.getX() - spec.x(),
                location.getY() - spec.y(),
                location.getZ() - spec.z(),
                spec.rx(),
                spec.ry(),
                spec.rz());
    }

    @Override
    public double coverRadius() {
        return Math.hypot(Math.hypot(spec.rx(), spec.ry()), spec.rz()) + 8.0;
    }

    @Override
    public Location[] regionProbePoints() {
        return new Location[] {
            new Location(world, spec.x() - spec.rx(), spec.y() - spec.ry(), spec.z() - spec.rz()),
            new Location(world, spec.x() + spec.rx(), spec.y() + spec.ry(), spec.z() + spec.rz())
        };
    }

    @Override
    public double minX() {
        return spec.x() - spec.rx();
    }

    @Override
    public double minY() {
        return spec.y() - spec.ry();
    }

    @Override
    public double minZ() {
        return spec.z() - spec.rz();
    }

    @Override
    public double maxX() {
        return spec.x() + spec.rx();
    }

    @Override
    public double maxY() {
        return spec.y() + spec.ry();
    }

    @Override
    public double maxZ() {
        return spec.z() + spec.rz();
    }
}
