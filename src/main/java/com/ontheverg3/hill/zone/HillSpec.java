package com.ontheverg3.hill.zone;

import com.ontheverg3.hill.config.FiniteNumbers;

public final class HillSpec {
    private final String id;
    private final String display;
    private final String world;
    private final String save;
    private final String dimension;
    private final HillShape shape;
    private final double x;
    private final double y;
    private final double z;
    private final double rx;
    private final double ry;
    private final double rz;

    public HillSpec(
            String id,
            String display,
            String world,
            String save,
            String dimension,
            HillShape shape,
            double x,
            double y,
            double z,
            double rx,
            double ry,
            double rz) {
        this.id = id;
        this.display = display == null || display.isBlank() ? id : display;
        this.world = world;
        this.save = save;
        this.dimension = dimension;
        this.shape = shape;
        if (!FiniteNumbers.allFinite(x, y, z, rx, ry, rz) || rx <= 0 || ry <= 0 || rz <= 0) {
            throw new IllegalArgumentException("hill geometry must use finite positive radii");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public String world() {
        return world;
    }

    public String save() {
        return save;
    }

    public String dimension() {
        return dimension;
    }

    public HillShape shape() {
        return shape;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double rx() {
        return rx;
    }

    public double ry() {
        return ry;
    }

    public double rz() {
        return rz;
    }
}
