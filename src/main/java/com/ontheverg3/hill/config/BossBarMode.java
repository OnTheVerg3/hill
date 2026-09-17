package com.ontheverg3.hill.config;

import java.util.Locale;

public enum BossBarMode {
    OFF,
    ON,
    ALWAYS;

    public static BossBarMode parse(Object raw) throws ConfigException {
        if (raw == null) {
            return ON;
        }
        if (raw instanceof Boolean flag) {
            return flag ? ON : OFF;
        }
        String token = String.valueOf(raw).trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (token.isEmpty()) {
            return ON;
        }
        return switch (token) {
            case "true", "on", "yes", "enable", "enabled" -> ON;
            case "false", "off", "no", "disable", "disabled" -> OFF;
            case "always", "alwayson", "always-on", "everywhere" -> ALWAYS;
            default -> throw new ConfigException("display.boss-bar must be true, false, or always");
        };
    }

    public boolean enabled() {
        return this != OFF;
    }

    public boolean always() {
        return this == ALWAYS;
    }

    public Object yamlValue() {
        return switch (this) {
            case OFF -> false;
            case ON -> true;
            case ALWAYS -> "always";
        };
    }

    public String id() {
        return switch (this) {
            case OFF -> "off";
            case ON -> "on";
            case ALWAYS -> "alwayson";
        };
    }
}
