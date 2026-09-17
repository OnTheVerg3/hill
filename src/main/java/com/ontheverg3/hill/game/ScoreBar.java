package com.ontheverg3.hill.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/** Two-sided score fill. Left grows with team 1, right grows with team 2. */
public final class ScoreBar {
    public static final String FILL = "\u2588";
    public static final String EMPTY = "\u2591";

    private ScoreBar() {}

    public record Fill(int leftFilled, int rightFilled, int width) {
        public int half() {
            return width / 2;
        }
    }

    public static Fill fill(int team1Score, int team2Score, int width, int cap) {
        int even = Math.max(2, width);
        if ((even & 1) != 0) {
            even++;
        }
        int half = even / 2;
        int leftScore = Math.max(0, team1Score);
        int rightScore = Math.max(0, team2Score);
        int scale = cap > 0 ? cap : Math.max(1, Math.max(leftScore, rightScore));
        int left = clamp(Math.round(half * (Math.min(leftScore, scale) / (double) scale)), 0, half);
        int right = clamp(Math.round(half * (Math.min(rightScore, scale) / (double) scale)), 0, half);
        return new Fill(left, right, even);
    }

    public static Component component(Fill fill, TextColor leftColor, TextColor rightColor) {
        TextColor left = leftColor == null ? NamedTextColor.BLUE : leftColor;
        TextColor right = rightColor == null ? NamedTextColor.YELLOW : rightColor;
        int half = fill.half();
        return Component.empty()
                .append(repeat(FILL, fill.leftFilled(), left))
                .append(repeat(EMPTY, half - fill.leftFilled(), NamedTextColor.DARK_GRAY))
                .append(repeat(EMPTY, half - fill.rightFilled(), NamedTextColor.DARK_GRAY))
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
