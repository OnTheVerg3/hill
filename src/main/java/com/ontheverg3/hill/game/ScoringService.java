package com.ontheverg3.hill.game;

import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.config.HillConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class ScoringService {
    private static final long AWARD_PERSIST_NANOS = 3_000_000_000L;

    private final HillPlugin plugin;
    private ScheduledTask task;
    private final AtomicBoolean inFlight = new AtomicBoolean();
    private final AtomicLong lastSampleNanos = new AtomicLong();
    private final AtomicLong maxSampleNanos = new AtomicLong();
    private final AtomicLong lastAwardPersistNanos = new AtomicLong();
    private final AtomicInteger lastScheduled = new AtomicInteger();
    private final AtomicInteger lastSkippedUnassigned = new AtomicInteger();
    private final AtomicInteger lastHops = new AtomicInteger();
    private final AtomicInteger overlapSkips = new AtomicInteger();
    private final AtomicInteger lastOnline = new AtomicInteger();
    private final AtomicReference<String> lastMode = new AtomicReference<>("idle");
    private final AtomicLong sampleLogs = new AtomicLong();

    public ScoringService(HillPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        overlapSkips.set(0);
        maxSampleNanos.set(0);
        lastSampleNanos.set(0);
        lastHops.set(0);
        lastMode.set("idle");
        resyncOnline();
        long ticks = Math.max(1L, plugin.config().intervalTicks());
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> sample(), ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public long lastSampleNanos() {
        return lastSampleNanos.get();
    }

    public long maxSampleNanos() {
        return maxSampleNanos.get();
    }

    public int lastScheduled() {
        return lastScheduled.get();
    }

    public int lastSkippedUnassigned() {
        return lastSkippedUnassigned.get();
    }

    public int lastHops() {
        return lastHops.get();
    }

    public int overlapSkips() {
        return overlapSkips.get();
    }

    public int lastOnline() {
        return lastOnline.get();
    }

    public String lastMode() {
        return lastMode.get();
    }

    public boolean inFlight() {
        return inFlight.get();
    }

    private void sample() {
        HillConfig config = plugin.config();
        if (config == null) {
            return;
        }
        int online = Bukkit.getOnlinePlayers().size();
        lastOnline.set(online);
        lastHops.set(0);
        lastSkippedUnassigned.set(0);
        int scheduled = 0;
        long started = System.nanoTime();
        try {
            lastMode.set("index");
            for (HillInstance hill : plugin.hills().all()) {
                if (!hill.zone().usable()) {
                    hill.match().setPointState(PointState.UNUSABLE);
                    hill.setLastScheduled(0);
                    continue;
                }
                int size = hill.tracker().size();
                hill.setLastScheduled(size);
                scheduled += size;
                finish(hill, hill.tracker().snapshot());
            }
            lastScheduled.set(scheduled);
        } finally {
            completeSample(started);
        }
    }

    public void scheduleSync(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.getScheduler().run(
                plugin,
                scheduled -> {
                    syncHere(player);
                    plugin.display().refresh(player);
                },
                null);
    }

    public boolean syncHere(Player player) {
        return syncHere(player, player == null ? null : player.getLocation());
    }

    public boolean syncHere(Player player, Location at) {
        HillConfig config = plugin.config();
        if (config == null || player == null) {
            return false;
        }
        plugin.hills().rememberPlayer(player);
        boolean assigned = plugin.hills().applyTeamPad(player, config, at);
        if (assigned) {
            plugin.persistMatch();
            TeamId team = plugin.hills().teams().teamOf(player.getUniqueId());
            if (team != null) {
                plugin.lang()
                        .send(
                                player,
                                "pad-join",
                                plugin.lang().component("team", plugin.lang().hud(team.langKey())));
            }
        }
        World world = at != null && at.getWorld() != null ? at.getWorld() : player.getWorld();
        for (HillInstance hill : plugin.hills().all()) {
            if (world != null && world.equals(hill.zone().worldOrNull())) {
                hill.tracker().sync(player, config, hill.zone(), plugin.hills().teams(), at);
            } else {
                hill.tracker().remove(player.getUniqueId());
            }
        }
        return assigned;
    }

    public void forget(UUID id) {
        plugin.hills().forgetPlayer(id);
    }

    public void clearPresence(HillInstance hill) {
        if (hill != null) {
            hill.tracker().clear();
        }
    }

    public void resyncOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleSync(player);
        }
    }

    public void resyncOnline(HillInstance hill) {
        if (hill == null) {
            resyncOnline();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.hills().sameSave(player, hill.save())) {
                scheduleSync(player);
            }
        }
    }

    private void finish(HillInstance hill, PointState state) {
        MatchState match = hill.match();
        match.setPointState(state);
        if (match.paused() || plugin.config().points() <= 0) {
            return;
        }
        TeamId winner = state.controllingTeamOrNull();
        if (winner == null) {
            return;
        }
        plugin.hills().teams().award(winner, plugin.config().points(), plugin.config().winScore());
        persistAward();
    }

    private void persistAward() {
        long now = System.nanoTime();
        long previous = lastAwardPersistNanos.get();
        if (now - previous < AWARD_PERSIST_NANOS) {
            return;
        }
        if (!lastAwardPersistNanos.compareAndSet(previous, now)) {
            return;
        }
        plugin.persistMatch();
    }

    private void completeSample(long started) {
        long elapsed = System.nanoTime() - started;
        lastSampleNanos.set(elapsed);
        maxSampleNanos.updateAndGet(previous -> Math.max(previous, elapsed));
        inFlight.set(false);
        logIfBusy(elapsed);
    }

    private void logIfBusy(long elapsedNanos) {
        boolean slow = elapsedNanos >= 10_000_000L;
        boolean overlapping = overlapSkips.get() > 0;
        if (!slow && !overlapping) {
            return;
        }
        long n = sampleLogs.incrementAndGet();
        if (!overlapping && n % 6 != 0L) {
            return;
        }
        plugin.getLogger()
                .info("Hill sample online="
                        + lastOnline.get()
                        + " scheduled="
                        + lastScheduled.get()
                        + " hills="
                        + plugin.hills().all().size()
                        + " mode="
                        + lastMode.get()
                        + " hops="
                        + lastHops.get()
                        + " skipped="
                        + lastSkippedUnassigned.get()
                        + " ms="
                        + String.format(java.util.Locale.ROOT, "%.3f", elapsedNanos / 1_000_000.0)
                        + " maxMs="
                        + String.format(java.util.Locale.ROOT, "%.3f", maxSampleNanos.get() / 1_000_000.0)
                        + " overlaps="
                        + overlapSkips.get());
    }
}
