package com.ontheverg3.hill.zone;

import java.util.Locale;
import java.util.Optional;

public enum HillShape {
    SQUARE,
    CIRCLE,
    CUBE,
    SPHERE,
    CYLINDER;

    public static Optional<HillShape> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "square", "sq" -> Optional.of(SQUARE);
            case "circle", "disk", "disc" -> Optional.of(CIRCLE);
            case "cube" -> Optional.of(CUBE);
            case "sphere", "ball" -> Optional.of(SPHERE);
            case "cylinder", "cyl" -> Optional.of(CYLINDER);
            default -> Optional.empty();
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean circularFootprint() {
        return this == CIRCLE || this == SPHERE || this == CYLINDER;
    }

    public static boolean contains(
            HillShape shape, double dx, double dy, double dz, double rx, double ry, double rz) {
        if (rx <= 0 || ry <= 0 || rz <= 0) {
            return false;
        }
        return switch (shape) {
            case SQUARE, CUBE -> Math.abs(dx) <= rx && Math.abs(dy) <= ry && Math.abs(dz) <= rz;
            case CIRCLE, CYLINDER -> (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz) <= 1.0 && Math.abs(dy) <= ry;
            case SPHERE -> (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz) <= 1.0;
        };
    }
}
