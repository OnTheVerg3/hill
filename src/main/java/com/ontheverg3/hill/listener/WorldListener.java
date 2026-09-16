package com.ontheverg3.hill.listener;

import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.config.ConfigException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public final class WorldListener implements Listener {
    private final HillPlugin plugin;

    public WorldListener(HillPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        try {
            if (plugin.config() == null) {
                plugin.reloadMatchConfig();
            }
            plugin.hills().bindWorld(event.getWorld());
            plugin.getLogger()
                    .info("Bound hills for world " + event.getWorld().getName() + " (" + event.getWorld().getKey() + ")");
        } catch (ConfigException ex) {
            plugin.getLogger().warning("Could not bind hill after world load: " + ex.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (plugin.hills() == null) {
            return;
        }
        plugin.hills().unbindWorld(event.getWorld());
        plugin.getLogger().warning("World unloaded: " + event.getWorld().getName() + "; that hill is frozen until it loads again.");
    }
}
