package com.ontheverg3.hill.zone;

import com.ontheverg3.hill.config.ConfigException;
import com.ontheverg3.hill.config.FiniteNumbers;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class ZoneSpec {
    public enum Type {
        CUBOID,
        SPHERE
    }

    private final Type type;
    private final double x1;
    private final double y1;
    private final double z1;
    private final double x2;
    private final double y2;
    private final double z2;
    private final double cx;
    private final double cy;
    private final double cz;
    private final double radius;

    private ZoneSpec(
            Type type,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double cx,
            double cy,
            double cz,
            double radius) {
        this.type = type;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.radius = radius;
    }

    public static ZoneSpec cuboid(Location a, Location b) {
        return cuboid(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    public static ZoneSpec cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new ZoneSpec(Type.CUBOID, x1, y1, z1, x2, y2, z2, 0, 0, 0, 0);
    }

    public static ZoneSpec sphere(Location center, double radius) {
        return sphere(center.getX(), center.getY(), center.getZ(), radius);
    }

    public static ZoneSpec sphere(double x, double y, double z, double radius) {
        return new ZoneSpec(Type.SPHERE, 0, 0, 0, 0, 0, 0, x, y, z, radius);
    }

    public static ZoneSpec defaultCuboid() {
        return cuboid(0.0, 64.0, 0.0, 8.0, 80.0, 8.0);
    }

    public static ZoneSpec load(ConfigurationSection section, String path) throws ConfigException {
        if (section == null) {
            throw new ConfigException(path + " is missing");
        }
        String typeRaw = section.getString("type");
        if (typeRaw == null || typeRaw.isBlank()) {
            throw new ConfigException(path + ".type is required");
        }
        String type = typeRaw.trim().toUpperCase(Locale.ROOT);
        if (type.equals("SPHERE")) {
            double radius = section.getDouble("radius", Double.NaN);
            if (!FiniteNumbers.isFinite(radius) || radius < 0.0) {
                throw new ConfigException(path + ".radius must be a finite number >= 0");
            }
            double[] center = requireXyz(section.getConfigurationSection("center"), path + ".center");
            return sphere(center[0], center[1], center[2], radius);
        }
        if (!type.equals("CUBOID")) {
            throw new ConfigException(path + ".type must be CUBOID or SPHERE");
        }
        double[] pos1 = requireXyz(section.getConfigurationSection("pos1"), path + ".pos1");
        double[] pos2 = requireXyz(section.getConfigurationSection("pos2"), path + ".pos2");
        return cuboid(pos1[0], pos1[1], pos1[2], pos2[0], pos2[1], pos2[2]);
    }

    public void write(ConfigurationSection section) {
        section.set("type", type.name());
        if (type == Type.SPHERE) {
            section.set("center.x", cx);
            section.set("center.y", cy);
            section.set("center.z", cz);
            section.set("radius", radius);
            return;
        }
        section.set("pos1.x", x1);
        section.set("pos1.y", y1);
        section.set("pos1.z", z1);
        section.set("pos2.x", x2);
        section.set("pos2.y", y2);
        section.set("pos2.z", z2);
    }

    public ZoneSpec withCorner(String which, Location loc) {
        if (type == Type.SPHERE) {
            return cuboid(loc, loc);
        }
        if ("pos1".equals(which)) {
            return cuboid(loc.getX(), loc.getY(), loc.getZ(), x2, y2, z2);
        }
        return cuboid(x1, y1, z1, loc.getX(), loc.getY(), loc.getZ());
    }

    public CaptureZone bind(World world, String missingReason) {
        if (world == null) {
            return new UnusableZone(missingReason);
        }
        if (type == Type.SPHERE) {
            return new SphereZone(world, new Location(world, cx, cy, cz), radius);
        }
        return new CuboidZone(
                world, new Location(world, x1, y1, z1), new Location(world, x2, y2, z2));
    }

    public Type type() {
        return type;
    }

    private static double[] requireXyz(ConfigurationSection section, String path) throws ConfigException {
        if (section == null) {
            throw new ConfigException(path + " is missing");
        }
        if (!section.contains("x") || !section.contains("y") || !section.contains("z")) {
            throw new ConfigException(path + " needs x, y, z");
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        if (!FiniteNumbers.allFinite(x, y, z)) {
            throw new ConfigException(path + " coordinates must be finite");
        }
        return new double[] {x, y, z};
    }
}
