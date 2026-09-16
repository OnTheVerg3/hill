package com.ontheverg3.hill.command;

import com.ontheverg3.hill.config.HillConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class Players {
    private Players() {}

    public static boolean isSelector(String token) {
        return token != null && token.startsWith("@");
    }

    public static boolean overridesAssignFilter(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (!isSelector(token)) {
            return true;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.contains("gamemode") || lower.contains("dead=") || lower.contains("alive=");
    }

    public static List<Player> resolve(CommandSender sender, String token) {
        List<Player> found = new ArrayList<>();
        if (token == null || token.isBlank()) {
            return found;
        }
        if (isSelector(token)) {
            try {
                for (Entity entity : Bukkit.selectEntities(sender, token)) {
                    if (entity instanceof Player player && player.isOnline()) {
                        found.add(player);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                return found;
            }
            return found;
        }
        Player exact = Bukkit.getPlayerExact(token);
        if (exact != null && exact.isOnline()) {
            found.add(exact);
        }
        return found;
    }

    public static boolean excludedByDefault(Player player, HillConfig config) {
        if (config == null) {
            return player == null || !player.isOnline();
        }
        return config.assignExcluded(player);
    }
}
