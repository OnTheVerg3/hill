package com.ontheverg3.hill.i18n;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Lang {
    private final JavaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private volatile TagResolver defaults = TagResolver.empty();
    private YamlConfiguration yaml;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(String locale) {
        String file = locale == null || locale.isBlank() ? "en" : locale.trim();
        plugin.getDataFolder().mkdirs();
        File folder = new File(plugin.getDataFolder(), "lang");
        folder.mkdirs();
        File disk = new File(folder, file + ".yml");
        if (!disk.exists()) {
            plugin.saveResource("lang/en.yml", false);
            disk = new File(folder, "en.yml");
        }
        yaml = YamlConfiguration.loadConfiguration(disk);
        try (InputStream in = plugin.getResource("lang/en.yml")) {
            if (in != null) {
                YamlConfiguration defaults =
                        YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                yaml.setDefaults(defaults);
                yaml.options().copyDefaults(true);
            }
        } catch (Exception ignored) {
        }
    }

    public void setDefaults(TagResolver defaults) {
        this.defaults = defaults == null ? TagResolver.empty() : defaults;
    }

    public Component chat(String key, TagResolver... extra) {
        String prefix = string("prefix");
        return mini.deserialize(prefix + string(key), merge(extra));
    }

    public Component hud(String key, TagResolver... extra) {
        return mini.deserialize(string(key), merge(extra));
    }

    public void send(CommandSender sender, String key, TagResolver... extra) {
        sender.sendMessage(chat(key, extra));
    }

    public TagResolver text(String name, String value) {
        return Placeholder.parsed(name, value == null ? "" : value);
    }

    public TagResolver unparsed(String name, String value) {
        return Placeholder.unparsed(name, value == null ? "" : value);
    }

    public TagResolver number(String name, int value) {
        return Placeholder.parsed(name, Integer.toString(value));
    }

    public TagResolver component(String name, Component component) {
        return Placeholder.component(name, component);
    }

    private String string(String key) {
        if (yaml == null) {
            return key;
        }
        String value = yaml.getString(key);
        return value == null || value.isBlank() ? key : value;
    }

    private TagResolver merge(TagResolver[] extra) {
        TagResolver base = defaults;
        if (extra == null || extra.length == 0) {
            return base;
        }
        return TagResolver.resolver(base, TagResolver.resolver(extra));
    }
}
