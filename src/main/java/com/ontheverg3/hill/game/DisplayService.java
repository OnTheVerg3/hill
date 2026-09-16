package com.ontheverg3.hill.game;

import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.i18n.Lang;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class DisplayService {
    private final HillPlugin plugin;
    private final ConcurrentHashMap<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> lastBossHash = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> hudClients = new ConcurrentHashMap<>();
    private ScheduledTask task;
    private volatile int lastHudClients;

    public DisplayService(HillPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop(false);
        hudClients.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            consider(player);
        }
        long ticks = Math.max(1L, plugin.config().displayUpdateTicks());
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tick(), ticks, ticks);
    }

    public void consider(Player player) {
        if (player == null || !player.isOnline() || !shouldRender(player)) {
            if (player != null) {
                hudClients.remove(player.getUniqueId());
            }
            return;
        }
        hudClients.put(player.getUniqueId(), Boolean.TRUE);
    }

    public void stop() {
        stop(false);
    }

    public void shutdown() {
        stop(true);
    }

    private void stop(boolean synchronousHide) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (synchronousHide) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                hide(player);
            }
            bars.clear();
            lastBossHash.clear();
            hudClients.clear();
            return;
        }
        for (UUID id : java.util.List.copyOf(hudClients.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                hudClients.remove(id);
                continue;
            }
            player.getScheduler()
                    .run(plugin, scheduled -> hide(player), () -> {
                        bars.remove(id);
                        lastBossHash.remove(id);
                    });
        }
    }

    public void hide(Player player) {
        UUID id = player.getUniqueId();
        hudClients.remove(id);
        lastBossHash.remove(id);
        BossBar bar = bars.remove(id);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public int lastHudClients() {
        return lastHudClients;
    }

    private void tick() {
        int clients = 0;
        for (UUID id : hudClients.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                hudClients.remove(id);
                continue;
            }
            clients++;
            player.getScheduler().run(plugin, scheduled -> render(player), null);
        }
        lastHudClients = clients;
    }

    private boolean shouldRender(Player player) {
        var config = plugin.config();
        if (config == null) {
            return true;
        }
        if (config.skipWithoutAddress() && player.getAddress() == null) {
            return false;
        }
        String prefix = config.ignoreNamePrefix();
        return prefix.isEmpty() || !player.getName().startsWith(prefix);
    }

    private void render(Player player) {
        if (!player.isOnline()) {
            return;
        }
        HillInstance hill = plugin.hills().ofPlayer(player);
        HudFrame current = HudFrame.capture(plugin, hill);
        if (plugin.config().actionBar()) {
            player.sendActionBar(current.actionBar());
        }
        if (!plugin.config().bossBar()) {
            hide(player);
            return;
        }
        UUID id = player.getUniqueId();
        BossBar bar = bars.computeIfAbsent(id, ignored -> {
            BossBar created = BossBar.bossBar(
                    Component.empty(), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
            player.showBossBar(created);
            return created;
        });
        Integer previous = lastBossHash.put(id, current.hash);
        if (previous != null && previous == current.hash) {
            return;
        }
        bar.name(current.bossBar());
        bar.progress(current.progress());
        bar.color(current.color());
    }

    private record HudFrame(
            Component actionBar, Component bossBar, float progress, BossBar.Color color, int hash) {
        private static final HudFrame EMPTY = new HudFrame(
                Component.empty(), Component.empty(), 1.0f, BossBar.Color.WHITE, 0);

        private static HudFrame capture(HillPlugin plugin, HillInstance hill) {
            if (hill == null) {
                return EMPTY;
            }
            Lang lang = plugin.lang();
            MatchState match = hill.match();
            PointState state = match.pointState();
            int blueScore = match.score(TeamId.BLUE);
            int yellowScore = match.score(TeamId.YELLOW);
            Component point = pointComponent(lang, state);
            var blue = lang.number("score_blue", blueScore);
            var yellow = lang.number("score_yellow", yellowScore);
            var pointTag = lang.component("point", point);
            Component action = lang.hud("action-bar", blue, yellow, pointTag);
            Component boss = lang.hud("boss-bar", blue, yellow, pointTag);
            float progress = progress(plugin, match);
            BossBar.Color color = barColor(plugin, state);
            int hash = 31 * (31 * (31 * (31 * state.ordinal() + blueScore) + yellowScore)
                            + hill.hillId().hashCode())
                    + Float.hashCode(progress);
            return new HudFrame(action, boss, progress, color, hash);
        }

        private static Component pointComponent(Lang lang, PointState state) {
            String key = switch (state) {
                case EMPTY -> "status-point-empty";
                case CONTESTED -> "status-point-contested";
                case CONTROLLED_BLUE -> "status-point-blue";
                case CONTROLLED_YELLOW -> "status-point-yellow";
                case UNUSABLE -> "status-point-unusable";
            };
            return lang.hud(key);
        }

        private static float progress(HillPlugin plugin, MatchState match) {
            int cap = plugin.config().winScore();
            if (cap <= 0) {
                return 1.0f;
            }
            int lead = Math.max(match.score(TeamId.BLUE), match.score(TeamId.YELLOW));
            return Math.min(1.0f, lead / (float) cap);
        }

        private static BossBar.Color barColor(HillPlugin plugin, PointState state) {
            var teams = plugin.config() == null ? null : plugin.config().teams();
            return switch (state) {
                case CONTROLLED_BLUE -> teams == null ? BossBar.Color.BLUE : teams.team1().bossBarColor();
                case CONTROLLED_YELLOW -> teams == null ? BossBar.Color.YELLOW : teams.team2().bossBarColor();
                case CONTESTED -> BossBar.Color.RED;
                case UNUSABLE -> BossBar.Color.PURPLE;
                case EMPTY -> BossBar.Color.WHITE;
            };
        }
    }
}
