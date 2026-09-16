package com.ontheverg3.hill.zone;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.World;

final class ZoneMath {
    /** Folia region section size at grid-exponent 4 (16 chunks / 256 blocks). */
    static final int REGION_BLOCKS = 256;

    private ZoneMath() {}

    static double cuboidCoverRadius(
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double halfX = (maxX - minX) / 2.0;
        double halfY = (maxY - minY) / 2.0;
        double halfZ = (maxZ - minZ) / 2.0;
        return Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ) + 2.0;
    }

    static double sphereCoverRadius(double radius) {
        return Math.max(0.0, radius) + 2.0;
    }

    static Location[] tileAnchors(
            World world,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            int step) {
        if (world == null || step < 16) {
            return new Location[0];
        }
        double midY = (minY + maxY) / 2.0;
        double width = maxX - minX;
        double depth = maxZ - minZ;
        if (width <= step && depth <= step) {
            return new Location[] {new Location(world, (minX + maxX) / 2.0, midY, (minZ + maxZ) / 2.0)};
        }
        ArrayList<Location> tiles = new ArrayList<>();
        double x = minX;
        while (true) {
            double z = minZ;
            while (true) {
                tiles.add(new Location(world, x, midY, z));
                if (z >= maxZ) {
                    break;
                }
                z = Math.min(maxZ, z + step);
            }
            if (x >= maxX) {
                break;
            }
            x = Math.min(maxX, x + step);
        }
        return tiles.toArray(Location[]::new);
    }

    static double tileRadius(int step, double minY, double maxY) {
        return cuboidCoverRadius(0, minY, 0, step, maxY, step);
    }

    static int regionCoord(double block) {
        return Math.floorDiv((int) Math.floor(block), REGION_BLOCKS);
    }

    static RegionSample[] regionSamples(
            World world,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return new RegionSample[0];
        }
        int minRx = regionCoord(minX);
        int maxRx = regionCoord(maxX);
        int minRz = regionCoord(minZ);
        int maxRz = regionCoord(maxZ);
        ArrayList<RegionSample> samples = new ArrayList<>();
        for (int rx = minRx; rx <= maxRx; rx++) {
            for (int rz = minRz; rz <= maxRz; rz++) {
                double regionMinX = (double) rx * REGION_BLOCKS;
                double regionMaxX = regionMinX + REGION_BLOCKS;
                double regionMinZ = (double) rz * REGION_BLOCKS;
                double regionMaxZ = regionMinZ + REGION_BLOCKS;
                double intersectMinX = Math.max(minX, regionMinX);
                double intersectMaxX = Math.min(maxX, regionMaxX);
                double intersectMinZ = Math.max(minZ, regionMinZ);
                double intersectMaxZ = Math.min(maxZ, regionMaxZ);
                if (intersectMinX >= intersectMaxX || intersectMinZ >= intersectMaxZ) {
                    continue;
                }
                Location anchor = new Location(
                        world,
                        (intersectMinX + intersectMaxX) / 2.0,
                        (minY + maxY) / 2.0,
                        (intersectMinZ + intersectMaxZ) / 2.0);
                double radius = cuboidCoverRadius(
                        intersectMinX, minY, intersectMinZ, intersectMaxX, maxY, intersectMaxZ);
                samples.add(new RegionSample(anchor, radius));
            }
        }
        return samples.toArray(RegionSample[]::new);
    }
}
