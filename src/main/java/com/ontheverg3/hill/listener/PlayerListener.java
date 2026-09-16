package com.ontheverg3.hill.listener;

import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.game.TeamId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final HillPlugin plugin;

    public PlayerListener(HillPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        plugin.hills().rememberPlayer(player);
        plugin.display().consider(player);
        var config = plugin.config();
        if (config != null) {
            if (config.skipWithoutAddress() && player.getAddress() == null) {
                return;
            }
            String prefix = config.ignoreNamePrefix();
            if (!prefix.isEmpty() && player.getName().startsWith(prefix)) {
                return;
            }
        }
        TeamId team = plugin.hills().teams().teamOf(player.getUniqueId());
        if (team == null) {
            return;
        }
        plugin.lang()
                .send(
                        player,
                        "join-team",
                        plugin.lang().component("team", plugin.lang().hud(team.langKey())));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.display().hide(event.getPlayer());
        plugin.hills().forgetPlayer(event.getPlayer().getUniqueId());
    }
}
