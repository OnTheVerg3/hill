package com.ontheverg3.hill.persist;

import com.ontheverg3.hill.game.HillInstance;
import com.ontheverg3.hill.game.HillRegistry;
import com.ontheverg3.hill.game.MatchState;
import com.ontheverg3.hill.game.TeamId;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class DataStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Object lock = new Object();
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile boolean dirty;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load(HillRegistry registry) {
        synchronized (lock) {
            loadUnlocked(registry);
        }
    }

    private void loadUnlocked(HillRegistry registry) {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<UUID, TeamId> teams = new HashMap<>();
        readTeams(yaml.getConfigurationSection("teams"), teams);
        int[] scores = new int[2];
        boolean haveGlobalScores = yaml.contains("scores");
        if (haveGlobalScores) {
            scores[0] = yaml.getInt("scores.blue", 0);
            scores[1] = yaml.getInt("scores.yellow", 0);
        }
        ConfigurationSection hills = yaml.getConfigurationSection("hills");
        if (hills != null) {
            for (String hillId : hills.getKeys(false)) {
                HillInstance instance = registry.byId(hillId);
                ConfigurationSection section = hills.getConfigurationSection(hillId);
                if (instance != null) {
                    applyPause(instance.match(), section);
                }
                if (!haveGlobalScores) {
                    int[] pair = scoresFrom(section);
                    scores[0] += pair[0];
                    scores[1] += pair[1];
                }
            }
        } else if (!haveGlobalScores) {
            int[] migrated = migrateLegacy(yaml, registry, teams);
            scores[0] = migrated[0];
            scores[1] = migrated[1];
        }
        registry.teams().replace(teams);
        registry.teams().setScore(TeamId.BLUE, scores[0]);
        registry.teams().setScore(TeamId.YELLOW, scores[1]);
    }

    private int[] migrateLegacy(YamlConfiguration yaml, HillRegistry registry, Map<UUID, TeamId> teams) {
        ConfigurationSection saves = yaml.getConfigurationSection("saves");
        if (saves == null) {
            readTeams(yaml.getConfigurationSection("teams"), teams);
            if (registry.all().size() == 1) {
                HillInstance only = registry.all().iterator().next();
                applyPause(only.match(), yaml);
                return scoresFrom(yaml);
            }
            return new int[] {0, 0};
        }
        int blue = 0;
        int yellow = 0;
        for (String save : saves.getKeys(false)) {
            ConfigurationSection hills = saves.getConfigurationSection(save + ".hills");
            if (hills == null) {
                continue;
            }
            for (String hillId : hills.getKeys(false)) {
                ConfigurationSection section = hills.getConfigurationSection(hillId);
                readTeams(section == null ? null : section.getConfigurationSection("teams"), teams);
                HillInstance instance = registry.byId(hillId);
                if (instance != null) {
                    applyPause(instance.match(), section);
                }
                int[] pair = scoresFrom(section);
                blue += pair[0];
                yellow += pair[1];
            }
        }
        return new int[] {blue, yellow};
    }

    private void readTeams(ConfigurationSection section, Map<UUID, TeamId> teams) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                TeamId.parse(section.getString(key)).ifPresent(team -> teams.put(id, team));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Ignored bad team uuid: " + key);
            }
        }
    }

    private void applyPause(MatchState state, ConfigurationSection section) {
        if (state == null || section == null) {
            return;
        }
        state.setPaused(section.getBoolean("paused", false));
    }

    private int[] scoresFrom(ConfigurationSection section) {
        if (section == null) {
            return new int[] {0, 0};
        }
        ConfigurationSection scores = section.getConfigurationSection("scores");
        if (scores == null) {
            return new int[] {section.getInt("scores.blue", 0), section.getInt("scores.yellow", 0)};
        }
        return new int[] {scores.getInt("blue", 0), scores.getInt("yellow", 0)};
    }

    public void save(HillRegistry registry) {
        synchronized (lock) {
            saveUnlocked(registry);
        }
    }

    private void saveUnlocked(HillRegistry registry) {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        registry.teams()
                .forEach((id, team) -> yaml.set("teams." + id, team.name().toLowerCase(Locale.ROOT)));
        yaml.set("scores.blue", registry.teams().score(TeamId.BLUE));
        yaml.set("scores.yellow", registry.teams().score(TeamId.YELLOW));
        for (HillInstance instance : registry.all()) {
            yaml.set("hills." + instance.hillId() + ".paused", instance.match().paused());
        }
        Path target = file.toPath();
        Path temp = target.resolveSibling("data.yml.tmp");
        try {
            yaml.save(temp.toFile());
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", ex);
        }
    }

    public void requestSave(HillRegistry registry) {
        dirty = true;
        if (!running.compareAndSet(false, true)) {
            return;
        }
        Bukkit.getAsyncScheduler().runNow(plugin, task -> drain(registry));
    }

    private void drain(HillRegistry registry) {
        try {
            while (true) {
                dirty = false;
                save(registry);
                running.set(false);
                if (!dirty) {
                    return;
                }
                if (!running.compareAndSet(false, true)) {
                    return;
                }
            }
        } catch (RuntimeException ex) {
            running.set(false);
            throw ex;
        }
    }
}
