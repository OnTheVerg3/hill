package com.ontheverg3.hill.placeholder;

import com.ontheverg3.hill.game.HillInstance;
import com.ontheverg3.hill.game.HillMode;
import com.ontheverg3.hill.game.PointState;
import com.ontheverg3.hill.game.TeamId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

public final class PlaceholderQuery {
    private static final String[] HILL_PREFIXES = {
        "score_yellow_",
        "score_blue_",
        "dimension_",
        "display_",
        "control_",
        "paused_",
        "shape_",
        "state_",
        "world_",
        "save_"
    };
    private static final String[] HERE_KEYS = {
        "here_score_yellow",
        "here_score_blue",
        "here_display",
        "here_paused",
        "here_state",
        "here_id"
    };

    private PlaceholderQuery() {}

    public static String resolve(String raw, UUID viewer, Context context) {
        if (raw == null || raw.isBlank() || context == null) {
            return null;
        }
        String params = raw.trim().toLowerCase(Locale.ROOT);
        return switch (params) {
            case "mode" -> context.mode().id();
            case "count" -> Integer.toString(context.ids().size());
            case "ids" -> String.join(",", context.ids());
            case "team" -> teamToken(context.teamOf().apply(viewer));
            case "team_blue" -> nullToEmpty(context.blueDisplay());
            case "team_yellow" -> nullToEmpty(context.yellowDisplay());
            case "team_color_blue" -> nullToEmpty(context.blueColor());
            case "team_color_yellow" -> nullToEmpty(context.yellowColor());
            default -> resolveRest(params, viewer, context);
        };
    }

    private static String resolveRest(String params, UUID viewer, Context context) {
        for (String key : HERE_KEYS) {
            if (params.equals(key)) {
                return hereValue(key, context.occupying().apply(viewer));
            }
        }
        for (String prefix : HILL_PREFIXES) {
            if (params.startsWith(prefix)) {
                String id = params.substring(prefix.length());
                return hillValue(prefix, context.byId().apply(id));
            }
        }
        return null;
    }

    private static String hereValue(String key, HillInstance hill) {
        if (hill == null) {
            return "";
        }
        return switch (key) {
            case "here_id" -> hill.hillId();
            case "here_display" -> hill.display();
            case "here_state" -> stateToken(hill.match().pointState());
            case "here_paused" -> Boolean.toString(hill.match().paused());
            case "here_score_blue" -> Integer.toString(hill.match().score(TeamId.BLUE));
            case "here_score_yellow" -> Integer.toString(hill.match().score(TeamId.YELLOW));
            default -> "";
        };
    }

    private static String hillValue(String prefix, HillInstance hill) {
        if (hill == null) {
            return "";
        }
        return switch (prefix) {
            case "score_blue_" -> Integer.toString(hill.match().score(TeamId.BLUE));
            case "score_yellow_" -> Integer.toString(hill.match().score(TeamId.YELLOW));
            case "state_", "control_" -> stateToken(hill.match().pointState());
            case "paused_" -> Boolean.toString(hill.match().paused());
            case "display_" -> hill.display();
            case "shape_" -> hill.spec().shape().id();
            case "world_" -> hill.worldName();
            case "dimension_" -> hill.dimension();
            case "save_" -> hill.save();
            default -> "";
        };
    }

    public static String stateToken(PointState state) {
        if (state == null) {
            return "empty";
        }
        return switch (state) {
            case EMPTY -> "empty";
            case CONTESTED -> "contested";
            case CONTROLLED_BLUE -> "blue";
            case CONTROLLED_YELLOW -> "yellow";
            case UNUSABLE -> "unusable";
        };
    }

    private static String teamToken(TeamId team) {
        if (team == null) {
            return "";
        }
        return team == TeamId.BLUE ? "blue" : "yellow";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record Context(
            HillMode mode,
            List<String> ids,
            Function<String, HillInstance> byId,
            Function<UUID, TeamId> teamOf,
            Function<UUID, HillInstance> occupying,
            String blueDisplay,
            String yellowDisplay,
            String blueColor,
            String yellowColor) {
        public Context {
            ids = ids == null ? List.of() : List.copyOf(ids);
            byId = byId == null ? id -> null : byId;
            teamOf = teamOf == null ? id -> null : teamOf;
            occupying = occupying == null ? id -> null : occupying;
            blueDisplay = blueDisplay == null ? "Blue" : blueDisplay;
            yellowDisplay = yellowDisplay == null ? "Yellow" : yellowDisplay;
            blueColor = blueColor == null ? "#5555ff" : blueColor;
            yellowColor = yellowColor == null ? "#ffff55" : yellowColor;
            mode = mode == null ? HillMode.KOTH : mode;
        }
    }
}
