package com.ontheverg3.hill.placeholder;

import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.config.HillConfig;
import com.ontheverg3.hill.game.HillRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class HillPlaceholders {
    private HillPlaceholders() {}

    public static Object register(HillPlugin plugin) {
        Expansion expansion = new Expansion(plugin);
        if (!expansion.register()) {
            throw new IllegalStateException("PlaceholderAPI rejected the Hill expansion");
        }
        return expansion;
    }

    public static void unregister(Object handle) {
        if (handle instanceof Expansion expansion) {
            expansion.unregister();
        }
    }

    public static final class Expansion extends PlaceholderExpansion {
        private final HillPlugin plugin;

        public Expansion(HillPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() {
            return "hill";
        }

        @Override
        public String getAuthor() {
            return "OnTheVerg3";
        }

        @Override
        public String getVersion() {
            return plugin.getPluginMeta().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            HillRegistry hills = plugin.hills();
            HillConfig config = plugin.config();
            if (hills == null || config == null) {
                return "";
            }
            return PlaceholderQuery.resolve(
                    params,
                    player == null ? null : player.getUniqueId(),
                    new PlaceholderQuery.Context(
                            hills.mode(),
                            hills.ids(),
                            hills::byId,
                            hills.teams()::teamOf,
                            hills::occupying,
                            config.blueDisplay(),
                            config.yellowDisplay(),
                            config.teams().team1().hex(),
                            config.teams().team2().hex()));
        }

        @Override
        public String onPlaceholderRequest(Player player, String params) {
            return onRequest(player, params);
        }
    }
}
