package com.ontheverg3.hill.game;

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
            case "blue", "b" -> Optional.of(BLUE);
            case "yellow", "y" -> Optional.of(YELLOW);
            default -> Optional.empty();
        };
    }

    public String langKey() {
        return this == BLUE ? "team-blue" : "team-yellow";
    }
}
