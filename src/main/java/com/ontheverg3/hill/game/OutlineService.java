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

public final class OutlineService {
    private static final Particle.DustOptions DUST = new Particle.DustOptions(Color.fromRGB(255, 196, 48), 1.15f);

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
            Map<Long, List<int[]>> byChunk = new HashMap<>();
            for (double[] xz : footprint(hill.spec(), count)) {
                int bx = (int) Math.floor(xz[0]);
                int bz = (int) Math.floor(xz[1]);
                long key = (((long) (bx >> 4)) << 32) ^ Integer.toUnsignedLong(bz >> 4);
                byChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new int[] {bx, bz});
            }
            for (List<int[]> points : byChunk.values()) {
                int[] first = points.get(0);
                Bukkit.getRegionScheduler()
                        .run(plugin, world, first[0] >> 4, first[1] >> 4, scheduled -> spawnChunk(world, hill.spec(), points));
            }
        }
    }

    private void spawnChunk(World world, HillSpec spec, List<int[]> points) {
        int minY = (int) Math.floor(spec.y() - spec.ry());
        for (int[] point : points) {
            if (!world.isChunkLoaded(point[0] >> 4, point[1] >> 4)) {
                continue;
            }
            int surface = world.getHighestBlockYAt(point[0], point[1], HeightMap.MOTION_BLOCKING);
            Block block = world.getBlockAt(point[0], surface, point[1]);
            boolean floating = block.isEmpty() || block.isLiquid() || surface < minY - 1;
            double x = point[0] + 0.5;
            double z = point[1] + 0.5;
            double y = floating ? spec.y() : surface + 0.12;
            world.spawnParticle(Particle.DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, DUST, true);
        }
    }

    private static List<double[]> footprint(HillSpec spec, int count) {
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
