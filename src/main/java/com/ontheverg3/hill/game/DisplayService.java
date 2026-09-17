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
import org.bukkit.entity.Player;

public final class DisplayService {
    private final HillPlugin plugin;
    private final ConcurrentHashMap<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> lastBossHash = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> hiddenBossBars = new ConcurrentHashMap<>();
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

    public boolean bossBarVisible(UUID id) {
        return !hiddenBossBars.containsKey(id);
    }

    public boolean toggleBossBar(UUID id) {
        if (bossBarVisible(id)) {
            hiddenBossBars.put(id, Boolean.TRUE);
            return false;
        }
        hiddenBossBars.remove(id);
        return true;
    }

    public void setBossBarVisible(UUID id, boolean visible) {
        if (visible) {
            hiddenBossBars.remove(id);
        } else {
            hiddenBossBars.put(id, Boolean.TRUE);
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
            hiddenBossBars.clear();
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
        if (!config.actionBar() && !config.bossBar()) {
            return false;
        }
        String prefix = config.ignoreNamePrefix();
        return prefix.isEmpty() || !player.getName().startsWith(prefix);
    }

    private boolean wantsBossBar(Player player) {
        var config = plugin.config();
        return config != null && config.bossBar() && bossBarVisible(player.getUniqueId());
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
        if (!wantsBossBar(player) || hill == null) {
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
        bar.progress(0.0f);
        bar.color(BossBar.Color.WHITE);
    }

    private record HudFrame(Component actionBar, Component bossBar, int hash) {
        private static final HudFrame EMPTY = new HudFrame(Component.empty(), Component.empty(), 0);

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
            TeamLooks teams = plugin.config().teams();
            ScoreBar.Fill fill = ScoreBar.fill(
                    blueScore, yellowScore, plugin.config().bossBarWidth(), plugin.config().winScore());
            Component bar = ScoreBar.component(fill, teams.team1().color(), teams.team2().color());
            Component boss = lang.hud("boss-bar", blue, yellow, pointTag, lang.component("bar", bar));
            int hash = 31 * (31 * (31 * (31 * (31 * state.ordinal() + blueScore) + yellowScore)
                            + hill.hillId().hashCode())
                    + fill.leftFilled())
                    + fill.rightFilled();
            return new HudFrame(action, boss, hash);
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
