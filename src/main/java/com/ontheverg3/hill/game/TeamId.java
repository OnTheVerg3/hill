package com.ontheverg3.hill.game;

import com.ontheverg3.hill.config.TeamLooks;
import java.util.Locale;
import java.util.Optional;

public enum TeamId {
    BLUE,
    YELLOW;

    public static Optional<TeamId> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "blue", "b", "team1", "1" -> Optional.of(BLUE);
            case "yellow", "y", "team2", "2" -> Optional.of(YELLOW);
            default -> Optional.empty();
        };
    }

    public static Optional<TeamId> parse(String raw, TeamLooks looks) {
        Optional<TeamId> slot = parse(raw);
        if (slot.isPresent() || looks == null || raw == null) {
            return slot;
        }
        String name = raw.trim();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        if (name.equalsIgnoreCase(looks.team1().display())) {
            return Optional.of(BLUE);
        }
        if (name.equalsIgnoreCase(looks.team2().display())) {
            return Optional.of(YELLOW);
        }
        return Optional.empty();
    }

    public String langKey() {
        return this == BLUE ? "team-blue" : "team-yellow";
    }
}
