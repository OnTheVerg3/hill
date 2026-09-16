package com.ontheverg3.hill.world;

import java.util.Locale;

public final class HillIds {
    public static final String OVERWORLD = "overworld";
    public static final String NETHER = "nether";
    public static final String THE_END = "the_end";

    private HillIds() {}

    public static String saveName(String worldFolderName) {
        if (worldFolderName == null || worldFolderName.isBlank()) {
            return "";
        }
        String name = worldFolderName.trim();
        if (name.endsWith("_nether") && name.length() > "_nether".length()) {
            return name.substring(0, name.length() - "_nether".length());
        }
        if (name.endsWith("_the_end") && name.length() > "_the_end".length()) {
            return name.substring(0, name.length() - "_the_end".length());
        }
        return name;
    }

    public static String dimensionToken(String environment, String namespace, String key) {
        String env = environment == null ? "CUSTOM" : environment.trim().toUpperCase(Locale.ROOT);
        return switch (env) {
            case "NORMAL" -> OVERWORLD;
            case "NETHER" -> NETHER;
            case "THE_END", "THEEND" -> THE_END;
            default -> customToken(namespace, key);
        };
    }

    public static String sanitizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = Character.toLowerCase(raw.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        String token = builder.toString();
        while (token.contains("__")) {
            token = token.replace("__", "_");
        }
        if (token.startsWith("_")) {
            token = token.substring(1);
        }
        if (token.endsWith("_")) {
            token = token.substring(0, token.length() - 1);
        }
        return token;
    }

    public static boolean isId(String token) {
        return token != null && token.matches("[a-z0-9_]+");
    }

    private static String customToken(String namespace, String key) {
        String ns = namespace == null || namespace.isBlank() ? "minecraft" : namespace;
        String path = key == null || key.isBlank() ? "custom" : key;
        if (ns.equalsIgnoreCase("minecraft")) {
            String token = sanitizeId(path);
            return token.isEmpty() ? "custom" : token;
        }
        String token = sanitizeId(ns + "_" + path);
        return token.isEmpty() ? "custom" : token;
    }
}
