package com.ontheverg3.hill.zone;

import org.bukkit.Location;

public final class RegionSample {
    private final Location anchor;
    private final double radius;

    public RegionSample(Location anchor, double radius) {
        this.anchor = anchor;
        this.radius = radius;
    }

    public Location anchor() {
        return anchor;
    }

    public double radius() {
        return radius;
    }
}
