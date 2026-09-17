package com.ontheverg3.hill;

import com.ontheverg3.hill.command.HillCommand;
import com.ontheverg3.hill.config.ConfigException;
import com.ontheverg3.hill.config.HillConfig;
import com.ontheverg3.hill.game.DisplayService;
import com.ontheverg3.hill.game.HillRegistry;
import com.ontheverg3.hill.game.OutlineService;
import com.ontheverg3.hill.game.ScoringService;
import com.ontheverg3.hill.i18n.Lang;
import com.ontheverg3.hill.listener.PlayerListener;
import com.ontheverg3.hill.listener.PresenceListener;
import com.ontheverg3.hill.listener.WorldListener;
import com.ontheverg3.hill.persist.DataStore;
import com.ontheverg3.hill.persist.HillsStore;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.File;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class HillPlugin extends JavaPlugin {
    private final HillCommand hillCommand = new HillCommand(this);
    private final HillRegistry hills = new HillRegistry();
    private Lang lang;
    private HillConfig config;
    private DataStore dataStore;
    private HillsStore hillsStore;
    private ScoringService scoring;
    private DisplayService display;
    private OutlineService outline;
    private Runnable placeholderShutdown;

    public HillPlugin() {
        getLifecycleManager()
                .registerEventHandler(
                        LifecycleEvents.COMMANDS,
                        event -> event.registrar()
                                .register(hillCommand.node(), "Hill commands", List.of("koth")));
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File bundledLang = new File(getDataFolder(), "lang/en.yml");
        if (!bundledLang.exists()) {
            saveResource("lang/en.yml", false);
        }
        lang = new Lang(this);
        dataStore = new DataStore(this);
        hillsStore = new HillsStore(this);
        scoring = new ScoringService(this);
        display = new DisplayService(this);
        outline = new OutlineService(this);
        try {
            reloadMatchConfig();
        } catch (ConfigException ex) {
            getLogger().severe("Invalid config: " + ex.getMessage());
            getLogger().severe("Hill will stay loaded until /hill reload succeeds.");
            this.config = HillConfig.fallback(ex.getMessage());
            lang.load("en");
            lang.setDefaults(this.config.teams().resolvers());
        }
        hillsStore.load(hills);
        dataStore.load(hills);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new PresenceListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        scoring.start();
        display.start();
        outline.start();
        hookPlaceholders();
        getLogger().info("Hill enabled for Folia 1.21.11 (" + hills.mode().display() + ", " + hills.all().size() + " hills)");
    }

    @Override
    public void onDisable() {
        if (placeholderShutdown != null) {
            placeholderShutdown.run();
            placeholderShutdown = null;
        }
        if (scoring != null) {
            scoring.stop();
        }
        if (display != null) {
            display.shutdown();
        }
        if (outline != null) {
            outline.stop();
        }
        if (dataStore != null) {
            dataStore.save(hills);
        }
        if (hillsStore != null) {
            hillsStore.save(hills);
        }
    }

    public void reloadMatchConfig() throws ConfigException {
        reloadConfig();
        this.config = HillConfig.load(getConfig());
        lang.load(this.config.locale());
        lang.setDefaults(this.config.teams().resolvers());
    }

    public void reloadAll() throws ConfigException {
        persistMatch();
        persistHills();
        HillConfig previous = this.config;
        try {
            reloadMatchConfig();
        } catch (ConfigException ex) {
            this.config = previous;
            throw ex;
        }
        hillsStore.load(hills);
        if (config.reloadResets()) {
            hills.resetScores();
            dataStore.save(hills);
        }
        scoring.stop();
        display.stop();
        outline.stop();
        scoring.start();
        display.start();
        outline.start();
    }

    public Lang lang() {
        return lang;
    }

    public HillConfig config() {
        return config;
    }

    public HillRegistry hills() {
        return hills;
    }

    public DataStore dataStore() {
        return dataStore;
    }

    public HillsStore hillsStore() {
        return hillsStore;
    }

    public DisplayService display() {
        return display;
    }

    public ScoringService scoring() {
        return scoring;
    }

    public OutlineService outline() {
        return outline;
    }

    public void persistMatch() {
        if (dataStore == null) {
            return;
        }
        dataStore.requestSave(hills);
    }

    public void persistHills() {
        if (hillsStore == null) {
            return;
        }
        hillsStore.save(hills);
    }

    private void hookPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            Class<?> type = Class.forName("com.ontheverg3.hill.placeholder.HillPlaceholders");
            Object hook = type.getMethod("register", HillPlugin.class).invoke(null, this);
            placeholderShutdown = () -> {
                try {
                    type.getMethod("unregister", Object.class).invoke(null, hook);
                } catch (ReflectiveOperationException ignored) {
                }
            };
            getLogger().info("PlaceholderAPI hooked. Scoreboard plugins can use %hill_*% with a hill id.");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            getLogger().warning("Could not hook PlaceholderAPI: " + ex.getMessage());
        }
    }
}
