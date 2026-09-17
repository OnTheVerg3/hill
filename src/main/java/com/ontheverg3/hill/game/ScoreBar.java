package com.ontheverg3.hill.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/** Two-sided score fill. Each side is that team's share of the combined score. */
public final class ScoreBar {
    public static final String FILL = "\u2588";
    public static final String EMPTY = "\u2591";

    private ScoreBar() {}

    public record Fill(int leftFilled, int rightFilled, int width) {
        public int empty() {
            return Math.max(0, width - leftFilled - rightFilled);
        }
    }

    public static Fill fill(int team1Score, int team2Score, int width) {
        int even = Math.max(2, width);
        if ((even & 1) != 0) {
            even++;
        }
        int leftScore = Math.max(0, team1Score);
        int rightScore = Math.max(0, team2Score);
        if (leftScore == 0 && rightScore == 0) {
            return new Fill(0, 0, even);
        }
        int total = leftScore + rightScore;
        int left = clamp(Math.round(even * (leftScore / (double) total)), 0, even);
        int right = even - left;
        return new Fill(left, right, even);
    }

    public static float trackProgress(int team1Score, int team2Score) {
        int left = Math.max(0, team1Score);
        int right = Math.max(0, team2Score);
        int total = left + right;
        if (total == 0) {
            return 0.0f;
        }
        return Math.max(left, right) / (float) total;
    }

    public static Component component(Fill fill, TextColor leftColor, TextColor rightColor) {
        TextColor left = leftColor == null ? NamedTextColor.BLUE : leftColor;
        TextColor right = rightColor == null ? NamedTextColor.YELLOW : rightColor;
        return Component.empty()
                .append(repeat(FILL, fill.leftFilled(), left))
                .append(repeat(EMPTY, fill.empty(), NamedTextColor.DARK_GRAY))
                .append(repeat(FILL, fill.rightFilled(), right));
    }

    private static Component repeat(String glyph, int count, TextColor color) {
        if (count <= 0) {
            return Component.empty();
        }
        return Component.text(glyph.repeat(count), color);
    }

    private static int clamp(long value, int min, int max) {
        return (int) Math.min(max, Math.max(min, value));
    }
}
