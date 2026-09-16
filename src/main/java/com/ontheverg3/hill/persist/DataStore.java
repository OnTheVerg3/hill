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
        ConfigurationSection hills = yaml.getConfigurationSection("hills");
        if (hills != null) {
            for (String hillId : hills.getKeys(false)) {
                HillInstance instance = registry.byId(hillId);
                if (instance == null) {
                    continue;
                }
                applyScores(instance.match(), hills.getConfigurationSection(hillId));
            }
        } else {
            migrateLegacy(yaml, registry, teams);
        }
        registry.teams().replace(teams);
    }

    private void migrateLegacy(YamlConfiguration yaml, HillRegistry registry, Map<UUID, TeamId> teams) {
        ConfigurationSection saves = yaml.getConfigurationSection("saves");
        if (saves == null) {
            readTeams(yaml.getConfigurationSection("teams"), teams);
            if (registry.all().size() == 1) {
                applyScores(registry.all().iterator().next().match(), yaml);
            }
            return;
        }
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
                    applyScores(instance.match(), section);
                }
            }
        }
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

    private void applyScores(MatchState state, ConfigurationSection section) {
        if (state == null || section == null) {
            return;
        }
        ConfigurationSection scores = section.getConfigurationSection("scores");
        if (scores == null) {
            state.setScore(TeamId.BLUE, section.getInt("scores.blue", 0));
            state.setScore(TeamId.YELLOW, section.getInt("scores.yellow", 0));
        } else {
            state.setScore(TeamId.BLUE, scores.getInt("blue", 0));
            state.setScore(TeamId.YELLOW, scores.getInt("yellow", 0));
        }
        state.setPaused(section.getBoolean("paused", false));
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
        for (HillInstance instance : registry.all()) {
            String prefix = "hills." + instance.hillId();
            MatchState state = instance.match();
            yaml.set(prefix + ".scores.blue", state.score(TeamId.BLUE));
            yaml.set(prefix + ".scores.yellow", state.score(TeamId.YELLOW));
            yaml.set(prefix + ".paused", state.paused());
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
