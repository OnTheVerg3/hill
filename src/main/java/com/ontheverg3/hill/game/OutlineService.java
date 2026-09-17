package com.ontheverg3.hill.game;

import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.config.HillConfig;
import com.ontheverg3.hill.zone.HillSpec;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.HeightMap;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

public final class OutlineService {
    private static final Particle.DustOptions DUST = new Particle.DustOptions(Color.fromRGB(255, 196, 48), 1.15f);
    static final double SURFACE_LIFT = 0.12;

    private final HillPlugin plugin;
    private ScheduledTask task;

    public OutlineService(HillPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        HillConfig config = plugin.config();
        if (config == null || !config.outlineEnabled()) {
            return;
        }
        long ticks = Math.max(1L, config.outlineIntervalTicks());
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tick(), ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        HillConfig config = plugin.config();
        if (config == null || !config.outlineEnabled()) {
            return;
        }
        int count = Math.max(16, Math.min(256, config.outlinePoints()));
        for (HillInstance hill : plugin.hills().all()) {
            if (!hill.zone().usable()) {
                continue;
            }
            World world = hill.zone().worldOrNull();
            if (world == null) {
                continue;
            }
            Map<Long, List<double[]>> byChunk = new HashMap<>();
            for (double[] xz : footprint(hill.spec(), count)) {
                int bx = (int) Math.floor(xz[0]);
                int bz = (int) Math.floor(xz[1]);
                long key = (((long) (bx >> 4)) << 32) ^ Integer.toUnsignedLong(bz >> 4);
                byChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(xz);
            }
            for (List<double[]> points : byChunk.values()) {
                double[] first = points.get(0);
                int chunkX = (int) Math.floor(first[0]) >> 4;
                int chunkZ = (int) Math.floor(first[1]) >> 4;
                Bukkit.getRegionScheduler()
                        .run(plugin, world, chunkX, chunkZ, scheduled -> spawnChunk(world, hill.spec(), points));
            }
        }
    }

    private void spawnChunk(World world, HillSpec spec, List<double[]> points) {
        int minY = (int) Math.floor(spec.y() - spec.ry());
        for (double[] point : points) {
            int bx = (int) Math.floor(point[0]);
            int bz = (int) Math.floor(point[1]);
            if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
                continue;
            }
            int surface = world.getHighestBlockYAt(bx, bz, HeightMap.MOTION_BLOCKING);
            Block block = world.getBlockAt(bx, surface, bz);
            if (block.isEmpty()) {
                block = world.getBlockAt(bx, surface - 1, bz);
                surface = surface - 1;
            }
            boolean floating = block.isEmpty() || block.isLiquid() || surface < minY - 1;
            double y = spawnY(spec.y(), floating, topOf(block, surface));
            world.spawnParticle(Particle.DUST, point[0], y, point[1], 1, 0.0, 0.0, 0.0, 0.0, DUST, true);
        }
    }

    static double spawnY(double specY, boolean floating, double surfaceTopY) {
        return floating ? specY : surfaceTopY + SURFACE_LIFT;
    }

    static double topOf(Block block, int surfaceY) {
        BoundingBox box = block.getBoundingBox();
        if (box.getHeight() <= 0.0) {
            return surfaceY + 1.0;
        }
        return box.getMaxY();
    }

    static List<double[]> footprint(HillSpec spec, int count) {
        List<double[]> points = new ArrayList<>(count);
        if (spec.shape().circularFootprint()) {
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0 * i) / count;
                points.add(new double[] {spec.x() + Math.cos(angle) * spec.rx(), spec.z() + Math.sin(angle) * spec.rz()});
            }
            return points;
        }
        double minX = spec.x() - spec.rx();
        double maxX = spec.x() + spec.rx();
        double minZ = spec.z() - spec.rz();
        double maxZ = spec.z() + spec.rz();
        double width = Math.max(0.001, maxX - minX);
        double depth = Math.max(0.001, maxZ - minZ);
        double perimeter = 2.0 * (width + depth);
        int n = Math.max(16, count);
        for (int i = 0; i < n; i++) {
            double d = (perimeter * i) / n;
            double x;
            double z;
            if (d <= width) {
                x = minX + d;
                z = minZ;
            } else if (d <= width + depth) {
                x = maxX;
                z = minZ + (d - width);
            } else if (d <= width + depth + width) {
                x = maxX - (d - width - depth);
                z = maxZ;
            } else {
                x = minX;
                z = maxZ - (d - width - depth - width);
            }
            points.add(new double[] {x, z});
        }
        return points;
    }
}
