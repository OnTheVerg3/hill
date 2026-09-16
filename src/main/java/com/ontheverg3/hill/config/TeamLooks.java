package com.ontheverg3.hill.config;

import com.ontheverg3.hill.game.TeamId;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Visible names and colors for the two scoring slots. Persist keys stay blue/yellow. */
public final class TeamLooks {
    public static final int DISPLAY_MAX = 32;
    private static final Set<String> TEAM1_ALIASES = Set.of("blue", "b", "team1", "1");
    private static final Set<String> TEAM2_ALIASES = Set.of("yellow", "y", "team2", "2");
    private static final Map<String, NamedTextColor> COLOR_ALIASES = Map.ofEntries(
            Map.entry("grey", NamedTextColor.GRAY),
            Map.entry("dark_grey", NamedTextColor.DARK_GRAY),
            Map.entry("darkgrey", NamedTextColor.DARK_GRAY),
            Map.entry("darkgray", NamedTextColor.DARK_GRAY),
            Map.entry("cyan", NamedTextColor.AQUA),
            Map.entry("dark_cyan", NamedTextColor.DARK_AQUA),
            Map.entry("darkcyan", NamedTextColor.DARK_AQUA),
            Map.entry("pink", NamedTextColor.LIGHT_PURPLE),
            Map.entry("magenta", NamedTextColor.LIGHT_PURPLE),
            Map.entry("purple", NamedTextColor.DARK_PURPLE),
            Map.entry("orange", NamedTextColor.GOLD),
            Map.entry("darkblue", NamedTextColor.DARK_BLUE),
            Map.entry("darkgreen", NamedTextColor.DARK_GREEN),
            Map.entry("darkaqua", NamedTextColor.DARK_AQUA),
            Map.entry("darkred", NamedTextColor.DARK_RED),
            Map.entry("darkpurple", NamedTextColor.DARK_PURPLE),
            Map.entry("lightpurple", NamedTextColor.LIGHT_PURPLE),
            Map.entry("light_purple", NamedTextColor.LIGHT_PURPLE));
    private static final BossBarSwatch[] BOSS_SWATCHES = {
        new BossBarSwatch(BossBar.Color.PINK, 255, 85, 255),
        new BossBarSwatch(BossBar.Color.BLUE, 85, 85, 255),
        new BossBarSwatch(BossBar.Color.BLUE, 0, 0, 170),
        new BossBarSwatch(BossBar.Color.RED, 255, 85, 85),
        new BossBarSwatch(BossBar.Color.GREEN, 85, 255, 85),
        new BossBarSwatch(BossBar.Color.YELLOW, 255, 255, 85),
        new BossBarSwatch(BossBar.Color.YELLOW, 255, 170, 0),
        new BossBarSwatch(BossBar.Color.PURPLE, 170, 0, 170),
        new BossBarSwatch(BossBar.Color.WHITE, 255, 255, 255)
    };

    private final Style team1;
    private final Style team2;

    public TeamLooks(Style team1, Style team2) {
        this.team1 = team1;
        this.team2 = team2;
    }

    public static TeamLooks defaults() {
        return new TeamLooks(
                new Style("Blue", NamedTextColor.BLUE), new Style("Yellow", NamedTextColor.YELLOW));
    }

    public static TeamLooks of(
            String team1Display, String team1Color, String team2Display, String team2Color)
            throws ConfigException {
        Style one = style("team1-displayname", "team1-color", team1Display, team1Color);
        Style two = style("team2-displayname", "team2-color", team2Display, team2Color);
        if (one.display().equalsIgnoreCase(two.display())) {
            throw new ConfigException("team1-displayname and team2-displayname must be different");
        }
        rejectAliasCollision("team1-displayname", one.display(), TEAM2_ALIASES);
        rejectAliasCollision("team2-displayname", two.display(), TEAM1_ALIASES);
        return new TeamLooks(one, two);
    }

    public Style team1() {
        return team1;
    }

    public Style team2() {
        return team2;
    }

    public Style of(TeamId team) {
        return team == TeamId.YELLOW ? team2 : team1;
    }

    public TagResolver resolvers() {
        return TagResolver.resolver(
                TagResolver.resolver("team1_color", Tag.styling(team1.color())),
                TagResolver.resolver("team2_color", Tag.styling(team2.color())),
                Placeholder.unparsed("team1", team1.display()),
                Placeholder.unparsed("team2", team2.display()));
    }

    public static TextColor parseColor(String raw) throws ConfigException {
        return parseColor("color", raw);
    }

    static TextColor parseColor(String path, String raw) throws ConfigException {
        if (raw == null || raw.isBlank()) {
            throw new ConfigException(path + " is required");
        }
        String value = raw.trim();
        String named = normalizeColorName(value);
        NamedTextColor vanilla = NamedTextColor.NAMES.value(named);
        if (vanilla != null) {
            return vanilla;
        }
        NamedTextColor alias = COLOR_ALIASES.get(named);
        if (alias != null) {
            return alias;
        }
        NamedTextColor compact = COLOR_ALIASES.get(named.replace("_", ""));
        if (compact != null) {
            return compact;
        }
        TextColor hex = parseHex(value);
        if (hex != null) {
            return hex;
        }
        throw new ConfigException(
                path + " must be a Minecraft color name or hex (#RGB, #RRGGBB): " + raw.trim());
    }

    private static Style style(String displayPath, String colorPath, String displayRaw, String colorRaw)
            throws ConfigException {
        String display = requireDisplay(displayPath, displayRaw);
        TextColor color = parseColor(colorPath, colorRaw);
        return new Style(display, color);
    }

    private static String requireDisplay(String path, String raw) throws ConfigException {
        if (raw == null || raw.isBlank()) {
            throw new ConfigException(path + " is required");
        }
        String display = raw.trim();
        if (display.isEmpty()) {
            throw new ConfigException(path + " is required");
        }
        if (display.length() > DISPLAY_MAX) {
            throw new ConfigException(path + " must be at most " + DISPLAY_MAX + " characters");
        }
        return display;
    }

    private static void rejectAliasCollision(String path, String display, Set<String> otherAliases)
            throws ConfigException {
        String token = display.toLowerCase(Locale.ROOT);
        if (otherAliases.contains(token)) {
            throw new ConfigException(path + " collides with the other team's command name (" + display + ")");
        }
    }

    private static String normalizeColorName(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static TextColor parseHex(String raw) {
        String value = raw.trim();
        if (value.regionMatches(true, 0, "0x", 0, 2)) {
            value = "#" + value.substring(2);
        }
        if (!value.startsWith("#")) {
            if (value.matches("(?i)[0-9a-f]{3}") || value.matches("(?i)[0-9a-f]{6}")) {
                value = "#" + value;
            } else {
                return null;
            }
        }
        String digits = value.substring(1);
        if (digits.length() == 3 && digits.matches("(?i)[0-9a-f]{3}")) {
            char r = digits.charAt(0);
            char g = digits.charAt(1);
            char b = digits.charAt(2);
            digits = "" + r + r + g + g + b + b;
            value = "#" + digits;
        }
        if (digits.length() != 6 || !digits.matches("(?i)[0-9a-f]{6}")) {
            return null;
        }
        return TextColor.fromHexString("#" + digits.toLowerCase(Locale.ROOT));
    }

    static BossBar.Color nearestBossBar(TextColor color) {
        if (color instanceof NamedTextColor named) {
            return namedBossBar(named);
        }
        int rgb = color.value();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        BossBar.Color best = BossBar.Color.WHITE;
        long bestDist = Long.MAX_VALUE;
        for (BossBarSwatch swatch : BOSS_SWATCHES) {
            long dr = r - swatch.r;
            long dg = g - swatch.g;
            long db = b - swatch.b;
            long dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = swatch.bar;
            }
        }
        return best;
    }

    private static BossBar.Color namedBossBar(NamedTextColor named) {
        if (named.equals(NamedTextColor.BLUE)
                || named.equals(NamedTextColor.DARK_BLUE)
                || named.equals(NamedTextColor.AQUA)
                || named.equals(NamedTextColor.DARK_AQUA)) {
            return BossBar.Color.BLUE;
        }
        if (named.equals(NamedTextColor.GREEN) || named.equals(NamedTextColor.DARK_GREEN)) {
            return BossBar.Color.GREEN;
        }
        if (named.equals(NamedTextColor.YELLOW) || named.equals(NamedTextColor.GOLD)) {
            return BossBar.Color.YELLOW;
        }
        if (named.equals(NamedTextColor.RED) || named.equals(NamedTextColor.DARK_RED)) {
            return BossBar.Color.RED;
        }
        if (named.equals(NamedTextColor.LIGHT_PURPLE)) {
            return BossBar.Color.PINK;
        }
        if (named.equals(NamedTextColor.DARK_PURPLE)) {
            return BossBar.Color.PURPLE;
        }
        return BossBar.Color.WHITE;
    }

    public record Style(String display, TextColor color) {
        public Component name() {
            return Component.text(display, color);
        }

        public String hex() {
            return String.format(Locale.ROOT, "#%06x", color.value() & 0xFFFFFF);
        }

        public BossBar.Color bossBarColor() {
            return nearestBossBar(color);
        }
    }

    private record BossBarSwatch(BossBar.Color bar, int r, int g, int b) {}
}
