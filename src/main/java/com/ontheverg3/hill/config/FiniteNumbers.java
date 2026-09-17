package com.ontheverg3.hill.config;

public final class FiniteNumbers {
    private FiniteNumbers() {}

    public static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    public static boolean allFinite(double... values) {
        if (values == null) {
            return false;
        }
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public static double parse(String token) {
        if (token == null) {
            throw new NumberFormatException("null");
        }
        double value = Double.parseDouble(token);
        if (!Double.isFinite(value)) {
            throw new NumberFormatException("non-finite");
        }
        return value;
    }

    public static boolean looksFinite(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parse(token);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static double fromObject(Object raw, String path) throws ConfigException {
        if (raw == null) {
            throw new ConfigException(path + " is required");
        }
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            if (!Double.isFinite(value)) {
                throw new ConfigException(path + " must be a finite number");
            }
            return value;
        }
        if (raw instanceof String text) {
            try {
                return parse(text.trim());
            } catch (NumberFormatException ex) {
                throw new ConfigException(path + " must be a number");
            }
        }
        throw new ConfigException(path + " must be a number");
    }

    public static int optionalInt(org.bukkit.configuration.ConfigurationSection yaml, String path, int fallback)
            throws ConfigException {
        if (yaml == null || !yaml.contains(path)) {
            return fallback;
        }
        return (int) fromObject(yaml.get(path), path);
    }

    public static int finiteInt(org.bukkit.configuration.ConfigurationSection section, String path, int fallback) {
        if (section == null || !section.contains(path)) {
            return fallback;
        }
        Object raw = section.get(path);
        if (!(raw instanceof Number number)) {
            return fallback;
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return (int) value;
    }
}
