package com.ontheverg3.hill.zone;

import org.bukkit.Location;
import org.bukkit.World;

public interface CaptureZone {
    boolean usable();

    String unusableReason();

    World worldOrNull();

    Location schedulerAnchor();

    boolean contains(Location location);

    double coverRadius();

    Location[] regionProbePoints();

    double minX();

    double minY();

    double minZ();

    double maxX();

    double maxY();

    double maxZ();

    default Location[] tileAnchors(int step) {
        if (!usable()) {
            return new Location[0];
        }
        return ZoneMath.tileAnchors(worldOrNull(), minX(), minY(), minZ(), maxX(), maxY(), maxZ(), step);
    }

    default double tileSearchRadius(int step) {
        return ZoneMath.tileRadius(step, minY(), maxY());
    }

    default RegionSample[] regionSamples() {
        if (!usable()) {
            return new RegionSample[0];
        }
        return ZoneMath.regionSamples(worldOrNull(), minX(), minY(), minZ(), maxX(), maxY(), maxZ());
    }
}
