package com.ontheverg3.hill.game;

import com.ontheverg3.hill.config.HillConfig;
import com.ontheverg3.hill.zone.CaptureZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PresenceTracker {
    private final ConcurrentHashMap<UUID, TeamId> inside = new ConcurrentHashMap<>();

    public void clear() {
        inside.clear();
    }

    public void remove(UUID id) {
        if (id != null) {
            inside.remove(id);
        }
    }

    void remember(UUID id, TeamId team) {
        if (id != null && team != null) {
            inside.put(id, team);
        }
    }

    public int size() {
        return inside.size();
    }

    public boolean occupies(UUID id) {
        return id != null && inside.containsKey(id);
    }

    public PointState snapshot() {
        boolean blue = false;
        boolean yellow = false;
        for (TeamId team : inside.values()) {
            if (team == TeamId.BLUE) {
                blue = true;
            } else if (team == TeamId.YELLOW) {
                yellow = true;
            }
            if (blue && yellow) {
                break;
            }
        }
        return PointState.fromPresence(blue, yellow);
    }

    public void sync(Player player, HillConfig config, CaptureZone zone, TeamBoard teams) {
        sync(player, config, zone, teams, player == null ? null : player.getLocation());
    }

    public void sync(Player player, HillConfig config, CaptureZone zone, TeamBoard teams, Location at) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        if (at == null) {
            at = player.getLocation();
        }
        if (!player.isOnline() || config == null || zone == null || teams == null || !zone.usable()) {
            inside.remove(id);
            return;
        }
        TeamId team = teams.teamOf(id);
        if (team == null) {
            inside.remove(id);
            return;
        }
        if (config.scoringFilter().excluded(player) || !zone.contains(at)) {
            inside.remove(id);
            return;
        }
        inside.put(id, team);
    }
}
