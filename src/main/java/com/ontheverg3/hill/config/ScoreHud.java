package com.ontheverg3.hill.config;

import java.util.Locale;

public enum ScoreHud {
    ALWAYS,
    IN_ZONE;

    public static ScoreHud parse(String raw) throws ConfigException {
        if (raw == null || raw.isBlank()) {
            return IN_ZONE;
        }
        String token = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (token) {
            case "always", "everywhere", "all" -> ALWAYS;
            case "in-zone", "inzone", "zone", "capture", "hill" -> IN_ZONE;
            default -> throw new ConfigException("display.scores must be always or in-zone");
        };
    }

    public boolean always() {
        return this == ALWAYS;
    }

    public String id() {
        return this == ALWAYS ? "always" : "in-zone";
    }
}
