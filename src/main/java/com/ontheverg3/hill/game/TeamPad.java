package com.ontheverg3.hill.game;

import com.ontheverg3.hill.config.FiniteNumbers;
import com.ontheverg3.hill.world.HillDimensions;
import org.bukkit.Location;
import org.bukkit.World;

/** AABB that assigns a team when a player stands in it. */
public final class TeamPad {
    private final TeamId team;
    private final String world;
    private final String save;
    private final String dimension;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    public TeamPad(
            TeamId team,
            String world,
            String save,
            String dimension,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        this.team = team;
        this.world = world;
        this.save = save;
        this.dimension = dimension;
        if (!FiniteNumbers.allFinite(minX, minY, minZ, maxX, maxY, maxZ)) {
            throw new IllegalArgumentException("pad coordinates must be finite");
        }
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public static TeamPad fromInclusiveBlocks(
            TeamId team,
            World world,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2) {
        double minX = Math.floor(Math.min(x1, x2));
        double minZ = Math.floor(Math.min(z1, z2));
        double maxX = Math.floor(Math.max(x1, x2)) + 1.0;
        double maxZ = Math.floor(Math.max(z1, z2)) + 1.0;
        double minY = Math.floor(Math.min(y1, y2));
        double maxY = Math.floor(Math.max(y1, y2)) + 2.0;
        return new TeamPad(
                team,
                world.getName(),
                HillDimensions.saveName(world),
                HillDimensions.dimensionToken(world),
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ);
    }

    public TeamId team() {
        return team;
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

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double minZ() {
        return minZ;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public double maxZ() {
        return maxZ;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!matchesWorld(location.getWorld())) {
            return false;
        }
        return containsPoint(location.getX(), location.getY(), location.getZ());
    }

    boolean containsPoint(double x, double y, double z) {
        return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
    }

    private boolean matchesWorld(World world) {
        if (this.world != null && this.world.equalsIgnoreCase(world.getName())) {
            return true;
        }
        return save.equals(HillDimensions.saveName(world))
                && dimension.equals(HillDimensions.dimensionToken(world));
    }
}
