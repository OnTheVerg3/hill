package com.ontheverg3.hill.game;

import java.util.Locale;
import java.util.Optional;

public enum HillMode {
    KOTH,
    CTF;

    public static Optional<HillMode> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "koth", "king", "kingofthehill", "kot-h" -> Optional.of(KOTH);
            case "ctf", "capture", "capturetheflag", "capture-the-flag" -> Optional.of(CTF);
            default -> Optional.empty();
        };
    }

    public String id() {
        return this == KOTH ? "koth" : "ctf";
    }

    public String display() {
        return this == KOTH ? "KotH" : "CTF";
    }
}
