package com.ontheverg3.hill.config;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class HillConfig {
    private final String locale;
    private final int intervalSeconds;
    private final int points;
    private final int winScore;
    private final PlayerFilter scoringFilter;
    private final PlayerFilter assignFilter;
    private final TeamLooks teams;
    private final boolean actionBar;
    private final boolean bossBar;
    private final int bossBarWidth;
    private final ScoreHud scoresHud;
    private final int displayUpdateTicks;
    private final boolean skipWithoutAddress;
    private final String ignoreNamePrefix;
    private final boolean resetClearsTeams;
    private final boolean reloadResets;
    private final boolean outlineEnabled;
    private final int outlineIntervalTicks;
    private final int outlinePoints;

    private HillConfig(
            String locale,
            int intervalSeconds,
            int points,
            int winScore,
            PlayerFilter scoringFilter,
            PlayerFilter assignFilter,
            TeamLooks teams,
            boolean actionBar,
            boolean bossBar,
            int bossBarWidth,
            ScoreHud scoresHud,
            int displayUpdateTicks,
            boolean skipWithoutAddress,
            String ignoreNamePrefix,
            boolean resetClearsTeams,
            boolean reloadResets,
            boolean outlineEnabled,
            int outlineIntervalTicks,
            int outlinePoints) {
        this.locale = locale;
        this.intervalSeconds = intervalSeconds;
        this.points = points;
        this.winScore = winScore;
        this.scoringFilter = scoringFilter;
        this.assignFilter = assignFilter;
        this.teams = teams;
        this.actionBar = actionBar;
        this.bossBar = bossBar;
        this.bossBarWidth = bossBarWidth;
        this.scoresHud = scoresHud == null ? ScoreHud.IN_ZONE : scoresHud;
        this.displayUpdateTicks = displayUpdateTicks;
        this.skipWithoutAddress = skipWithoutAddress;
        this.ignoreNamePrefix = ignoreNamePrefix;
        this.resetClearsTeams = resetClearsTeams;
        this.reloadResets = reloadResets;
        this.outlineEnabled = outlineEnabled;
        this.outlineIntervalTicks = outlineIntervalTicks;
        this.outlinePoints = outlinePoints;
    }

    public static HillConfig load(FileConfiguration yaml) throws ConfigException {
        String locale = requireLocale(yaml, "locale");
        int interval = requirePositiveInt(yaml, "scoring.interval-seconds");
        int points = requireNonNegativeInt(yaml, "scoring.points");
        int winScore = requireNonNegativeInt(yaml, "scoring.win-score");
        PlayerFilter scoringFilter =
                PlayerFilter.load(yaml, "eligibility", true, List.of(GameMode.SPECTATOR));
        PlayerFilter assignFilter =
                PlayerFilter.load(yaml, "assign", true, List.of(GameMode.SPECTATOR, GameMode.CREATIVE));
        TeamLooks teams = loadTeams(yaml);
        boolean actionBar = yaml.getBoolean("display.action-bar", true);
        boolean bossBar = yaml.getBoolean("display.boss-bar", true);
        int bossBarWidth = FiniteNumbers.optionalInt(yaml, "display.boss-bar-width", 24);
        if (bossBarWidth < 8) {
            throw new ConfigException("display.boss-bar-width must be >= 8");
        }
        if (bossBarWidth > 64) {
            throw new ConfigException("display.boss-bar-width must be <= 64");
        }
        if ((bossBarWidth & 1) != 0) {
            bossBarWidth++;
        }
        ScoreHud scoresHud = ScoreHud.parse(yaml.getString("display.scores", "in-zone"));
        int updateTicks = requirePositiveInt(yaml, "display.update-ticks");
        boolean skipWithoutAddress = yaml.getBoolean("display.skip-without-address", true);
        String ignoreNamePrefix = yaml.getString("display.ignore-name-prefix", "");
        if (ignoreNamePrefix == null) {
            ignoreNamePrefix = "";
        }
        ignoreNamePrefix = ignoreNamePrefix.trim();
        if (!ignoreNamePrefix.isEmpty() && !ignoreNamePrefix.matches("[A-Za-z0-9_]+")) {
            throw new ConfigException(
                    "display.ignore-name-prefix must be empty or letters, digits, and underscore");
        }
        boolean resetClearsTeams = yaml.getBoolean("reset.clear-teams", false);
        boolean reloadResets = yaml.getBoolean("reload-resets", false);
        boolean outlineEnabled = yaml.getBoolean("outline.enabled", true);
        int outlineTicks = FiniteNumbers.optionalInt(yaml, "outline.interval-ticks", 10);
        if (outlineTicks < 1) {
            throw new ConfigException("outline.interval-ticks must be >= 1");
        }
        int outlinePoints = FiniteNumbers.optionalInt(yaml, "outline.points", 72);
        if (outlinePoints < 16) {
            throw new ConfigException("outline.points must be >= 16");
        }
        return new HillConfig(
                locale,
                interval,
                points,
                winScore,
                scoringFilter,
                assignFilter,
                teams,
                actionBar,
                bossBar,
                bossBarWidth,
                scoresHud,
                updateTicks,
                skipWithoutAddress,
                ignoreNamePrefix,
                resetClearsTeams,
                reloadResets,
                outlineEnabled,
                outlineTicks,
                outlinePoints);
    }

    public static HillConfig fallback(String reason) {
        return new HillConfig(
                "en",
                5,
                1,
                0,
                new PlayerFilter(true, List.of(GameMode.SPECTATOR)),
                new PlayerFilter(true, List.of(GameMode.SPECTATOR, GameMode.CREATIVE)),
                TeamLooks.defaults(),
                true,
                true,
                24,
                ScoreHud.IN_ZONE,
                20,
                true,
                "",
                false,
                false,
                true,
                10,
                72);
    }

    public String locale() {
        return locale;
    }

    public int intervalSeconds() {
        return intervalSeconds;
    }

    public long intervalTicks() {
        return intervalSeconds * 20L;
    }

    public int points() {
        return points;
    }

    public int winScore() {
        return winScore;
    }

    public PlayerFilter scoringFilter() {
        return scoringFilter;
    }

    public PlayerFilter assignFilter() {
        return assignFilter;
    }

    public boolean excludeDead() {
        return scoringFilter.excludeDead();
    }

    public List<GameMode> excludeGamemodes() {
        return scoringFilter.excludeGamemodes();
    }

    public boolean assignExcluded(Player player) {
        return assignFilter.excluded(player);
    }

    public TeamLooks teams() {
        return teams;
    }

    public String blueDisplay() {
        return teams.team1().display();
    }

    public String yellowDisplay() {
        return teams.team2().display();
    }

    public boolean actionBar() {
        return actionBar;
    }

    public boolean bossBar() {
        return bossBar;
    }

    public HillConfig withBossBar(boolean enabled) {
        return new HillConfig(
                locale,
                intervalSeconds,
                points,
                winScore,
                scoringFilter,
                assignFilter,
                teams,
                actionBar,
                enabled,
                bossBarWidth,
                scoresHud,
                displayUpdateTicks,
                skipWithoutAddress,
                ignoreNamePrefix,
                resetClearsTeams,
                reloadResets,
                outlineEnabled,
                outlineIntervalTicks,
                outlinePoints);
    }

    public int bossBarWidth() {
        return bossBarWidth;
    }

    public ScoreHud scoresHud() {
        return scoresHud;
    }

    public int displayUpdateTicks() {
        return displayUpdateTicks;
    }

    public boolean skipWithoutAddress() {
        return skipWithoutAddress;
    }

    public String ignoreNamePrefix() {
        return ignoreNamePrefix;
    }

    public boolean resetClearsTeams() {
        return resetClearsTeams;
    }

    public boolean reloadResets() {
        return reloadResets;
    }

    public boolean outlineEnabled() {
        return outlineEnabled;
    }

    public int outlineIntervalTicks() {
        return outlineIntervalTicks;
    }

    public int outlinePoints() {
        return outlinePoints;
    }

    private static TeamLooks loadTeams(FileConfiguration yaml) throws ConfigException {
        String team1Display = firstString(yaml, "team1-displayname", "teams.blue.display", "Blue");
        String team1Color = firstString(yaml, "team1-color", "teams.blue.color", "blue");
        String team2Display = firstString(yaml, "team2-displayname", "teams.yellow.display", "Yellow");
        String team2Color = firstString(yaml, "team2-color", "teams.yellow.color", "yellow");
        return TeamLooks.of(team1Display, team1Color, team2Display, team2Color);
    }

    private static String firstString(FileConfiguration yaml, String path, String fallbackPath, String fallback) {
        String value = yaml.getString(path);
        if (value != null && !value.isBlank()) {
            return value;
        }
        String legacy = yaml.getString(fallbackPath);
        if (legacy != null && !legacy.isBlank()) {
            return legacy;
        }
        return fallback;
    }

    private static String requireString(FileConfiguration yaml, String path) throws ConfigException {
        String value = yaml.getString(path);
        if (value == null || value.isBlank()) {
            throw new ConfigException(path + " is required");
        }
        return value;
    }

    private static String requireLocale(FileConfiguration yaml, String path) throws ConfigException {
        String value = requireString(yaml, path).trim();
        if (!value.matches("[A-Za-z0-9_-]+")) {
            throw new ConfigException(path + " must be letters, digits, underscore, or hyphen");
        }
        return value;
    }

    private static int requirePositiveInt(FileConfiguration yaml, String path) throws ConfigException {
        int value = (int) FiniteNumbers.fromObject(yaml.get(path), path);
        if (value < 1) {
            throw new ConfigException(path + " must be >= 1");
        }
        return value;
    }

    private static int requireNonNegativeInt(FileConfiguration yaml, String path) throws ConfigException {
        if (!yaml.contains(path)) {
            throw new ConfigException(path + " is required");
        }
        int value = (int) FiniteNumbers.fromObject(yaml.get(path), path);
        if (value < 0) {
            throw new ConfigException(path + " must be >= 0");
        }
        return value;
    }
}
