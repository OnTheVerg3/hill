package com.ontheverg3.hill.command;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PercentSplit {
    private static final Pattern TOKEN = Pattern.compile("^(\\d+(?:\\.\\d+)?)%?$");

    public record Ratio(double bluePercent, double yellowPercent) {}

    private PercentSplit() {}

    public static boolean looksLike(String token) {
        return parseOne(token) != null;
    }

    public static Optional<Ratio> parse(String blueToken, String yellowToken) {
        Double blue = parseOne(blueToken);
        Double yellow = parseOne(yellowToken);
        if (blue == null || yellow == null) {
            return Optional.empty();
        }
        if (Math.abs(blue + yellow - 100.0) > 0.051) {
            return Optional.empty();
        }
        return Optional.of(new Ratio(blue, yellow));
    }

    public static int[] counts(int players, Ratio ratio) {
        if (players < 0) {
            throw new IllegalArgumentException("players must be >= 0");
        }
        int blue = (int) Math.round(players * ratio.bluePercent() / 100.0);
        if (blue < 0) {
            blue = 0;
        }
        if (blue > players) {
            blue = players;
        }
        return new int[] {blue, players - blue};
    }

    public static String complement(String token) {
        Double value = parseOne(token);
        if (value == null) {
            return "50%";
        }
        double rest = Math.max(0.0, Math.min(100.0, 100.0 - value));
        if (Math.abs(rest - Math.rint(rest)) < 1e-9) {
            return Integer.toString((int) Math.rint(rest)) + "%";
        }
        return String.format(Locale.ROOT, "%.1f%%", rest);
    }

    static Double parseOne(String token) {
        if (token == null) {
            return null;
        }
        Matcher matcher = TOKEN.matcher(token.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            double value = Double.parseDouble(matcher.group(1));
            if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
