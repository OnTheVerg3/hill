package com.ontheverg3.hill.game;

import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.config.TeamLooks;
import com.ontheverg3.hill.i18n.Lang;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        refreshEveryone();
        long ticks = Math.max(1L, plugin.config().displayUpdateTicks());
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tick(), ticks, ticks);
    }

    public void consider(Player player) {
        consider(player, player == null ? null : player.getLocation());
    }

    private void consider(Player player, Location at) {
        if (player == null || !player.isOnline() || !shouldRender(player) || !wantsHud(player, at)) {
            if (player != null) {
                hudClients.remove(player.getUniqueId());
            }
            return;
        }
        hudClients.put(player.getUniqueId(), Boolean.TRUE);
    }

    public void refreshEveryone() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(plugin, scheduled -> refresh(player), null);
        }
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
                    .run(plugin, scheduled -> hideBossBar(player), () -> {
                        bars.remove(id);
                        lastBossHash.remove(id);
                    });
        }
    }

    public void hide(Player player) {
        hudClients.remove(player.getUniqueId());
        hideBossBar(player);
    }

    public void hideBossBar(Player player) {
        UUID id = player.getUniqueId();
        lastBossHash.remove(id);
        BossBar bar = bars.remove(id);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void refresh(Player player) {
        refresh(player, player == null ? null : player.getLocation());
    }

    public void refresh(Player player, Location at) {
        if (player == null || !player.isOnline()) {
            return;
        }
        consider(player, at);
        if (!hudClients.containsKey(player.getUniqueId())) {
            if (plugin.config() != null && plugin.config().actionBar()) {
                player.sendActionBar(Component.empty());
            }
            hideBossBar(player);
            return;
        }
        render(player, at);
    }

    public int lastHudClients() {
        return lastHudClients;
    }

    private void tick() {
        var config = plugin.config();
        if (config != null && config.bossBarAlways()) {
            int clients = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                clients++;
                player.getScheduler().run(plugin, scheduled -> refresh(player), null);
            }
            lastHudClients = clients;
            return;
        }
        int clients = 0;
        for (UUID id : hudClients.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                hudClients.remove(id);
                continue;
            }
            clients++;
            player.getScheduler().run(plugin, scheduled -> refresh(player), null);
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
        if (!config.actionBar() && !config.bossBar()) {
            return false;
        }
        String prefix = config.ignoreNamePrefix();
        return prefix.isEmpty() || !player.getName().startsWith(prefix);
    }

    private boolean inScoreHud(Location loc) {
        var config = plugin.config();
        if (config == null) {
            return true;
        }
        return config.scoresHud().visible(plugin.hills().containing(loc) != null);
    }

    private boolean wantsActionBar(Location loc) {
        var config = plugin.config();
        return config != null && config.actionBar() && inScoreHud(loc);
    }

    private boolean wantsBossBar(Location loc) {
        var config = plugin.config();
        if (config == null || !config.bossBar()) {
            return false;
        }
        return config.bossBarAlways() || inScoreHud(loc);
    }

    private boolean wantsHud(Player player, Location at) {
        Location loc = at != null ? at : player.getLocation();
        return wantsActionBar(loc) || wantsBossBar(loc);
    }

    private void render(Player player, Location at) {
        if (!player.isOnline()) {
            return;
        }
        Location loc = at != null ? at : player.getLocation();
        HillInstance hill = plugin.hills().containing(loc);
        HudFrame current = HudFrame.capture(plugin, hill);
        if (plugin.config().actionBar()) {
            if (wantsActionBar(loc)) {
                player.sendActionBar(current.actionBar());
            } else {
                player.sendActionBar(Component.empty());
            }
        }
        if (!wantsBossBar(loc)) {
            hideBossBar(player);
            return;
        }
        UUID id = player.getUniqueId();
        BossBar bar = bars.computeIfAbsent(id, ignored -> {
            BossBar created = BossBar.bossBar(
                    Component.empty(), 0.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
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

    private record HudFrame(Component actionBar, Component bossBar, int hash, float progress, BossBar.Color color) {

        private static HudFrame capture(HillPlugin plugin, HillInstance hill) {
            Lang lang = plugin.lang();
            TeamLooks teams = plugin.config().teams();
            int blueScore = plugin.hills().teams().score(TeamId.BLUE);
            int yellowScore = plugin.hills().teams().score(TeamId.YELLOW);
            PointState state = hill == null ? null : hill.match().pointState();
            Component point = hill == null ? lang.hud("status-point-away") : pointComponent(lang, state);
            var blue = lang.number("score_blue", blueScore);
            var yellow = lang.number("score_yellow", yellowScore);
            var pointTag = lang.component("point", point);
            Component action = lang.hud("action-bar", blue, yellow, pointTag);
            ScoreBar.Fill fill = ScoreBar.fill(blueScore, yellowScore, plugin.config().bossBarWidth());
            Component bar = ScoreBar.component(fill, teams.team1().color(), teams.team2().color());
            Component boss = lang.hud("boss-bar", blue, yellow, pointTag, lang.component("bar", bar));
            float progress = ScoreBar.trackProgress(blueScore, yellowScore);
            BossBar.Color color = trackColor(teams, blueScore, yellowScore);
            int hash = 31 * (31 * (31 * (31 * (31 * (31 * (state == null ? -1 : state.ordinal()) + blueScore)
                                    + yellowScore)
                            + (hill == null ? 0 : hill.hillId().hashCode()))
                    + fill.leftFilled())
                    + fill.rightFilled())
                    + Float.floatToIntBits(progress)
                    + color.ordinal();
            return new HudFrame(action, boss, hash, progress, color);
        }

        private static BossBar.Color trackColor(TeamLooks teams, int blueScore, int yellowScore) {
            if (blueScore > yellowScore) {
                return teams.team1().bossBarColor();
            }
            if (yellowScore > blueScore) {
                return teams.team2().bossBarColor();
            }
            return BossBar.Color.WHITE;
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
    }
}
