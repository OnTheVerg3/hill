package com.ontheverg3.hill.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ontheverg3.hill.game.HillInstance;
import com.ontheverg3.hill.game.HillMode;
import com.ontheverg3.hill.game.PointState;
import com.ontheverg3.hill.game.TeamId;
import com.ontheverg3.hill.zone.HillShape;
import com.ontheverg3.hill.zone.HillSpec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlaceholderQueryTest {
    @Test
    void resolvesGlobalAndPerHillTokens() {
        HillInstance hill = instance("castle_hill", "Castle");
        hill.match().setPaused(true);
        hill.match().setPointState(PointState.CONTROLLED_BLUE);
        UUID viewer = UUID.fromString("00000000-0000-0000-0000-000000000010");
        PlaceholderQuery.Context context = context(hill, viewer, TeamId.YELLOW, 12, 4);
        assertEquals("koth", PlaceholderQuery.resolve("mode", viewer, context));
        assertEquals("1", PlaceholderQuery.resolve("count", viewer, context));
        assertEquals("castle_hill", PlaceholderQuery.resolve("ids", viewer, context));
        assertEquals("yellow", PlaceholderQuery.resolve("team", viewer, context));
        assertEquals("Blue", PlaceholderQuery.resolve("team_blue", viewer, context));
        assertEquals("#5555ff", PlaceholderQuery.resolve("team_color_blue", viewer, context));
        assertEquals("12", PlaceholderQuery.resolve("score_blue", viewer, context));
        assertEquals("4", PlaceholderQuery.resolve("score_yellow", viewer, context));
        assertEquals("12", PlaceholderQuery.resolve("score_blue_castle_hill", viewer, context));
        assertEquals("4", PlaceholderQuery.resolve("score_yellow_castle_hill", viewer, context));
        assertEquals("blue", PlaceholderQuery.resolve("state_castle_hill", viewer, context));
        assertEquals("true", PlaceholderQuery.resolve("paused_castle_hill", viewer, context));
        assertEquals("Castle", PlaceholderQuery.resolve("display_castle_hill", viewer, context));
        assertEquals("circle", PlaceholderQuery.resolve("shape_castle_hill", viewer, context));
        assertEquals("", PlaceholderQuery.resolve("score_blue_missing", viewer, context));
        assertNull(PlaceholderQuery.resolve("not_a_token", viewer, context));
    }

    @Test
    void hereTokensUseOccupyingHillAndIgnoreUnassigned() {
        HillInstance hill = instance("mid", "Mid");
        hill.match().setPointState(PointState.EMPTY);
        UUID onHill = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID unassigned = UUID.fromString("00000000-0000-0000-0000-000000000012");
        PlaceholderQuery.Context on = context(hill, onHill, TeamId.BLUE, 3, 0);
        PlaceholderQuery.Context off =
                new PlaceholderQuery.Context(
                        HillMode.CTF,
                        List.of("mid"),
                        id -> "mid".equalsIgnoreCase(id) ? hill : null,
                        id -> null,
                        id -> null,
                        "Blue",
                        "Yellow",
                        "#5555ff",
                        "#ffff55",
                        3,
                        0);
        assertEquals("mid", PlaceholderQuery.resolve("here_id", onHill, on));
        assertEquals("3", PlaceholderQuery.resolve("here_score_blue", onHill, on));
        assertEquals("", PlaceholderQuery.resolve("here_id", unassigned, off));
        assertEquals("", PlaceholderQuery.resolve("team", unassigned, off));
        assertEquals("ctf", PlaceholderQuery.resolve("mode", unassigned, off));
    }

    private static HillInstance instance(String id, String display) {
        return new HillInstance(
                new HillSpec(id, display, "world", "world", "overworld", HillShape.CIRCLE, 0, 64, 0, 8, 16, 8));
    }

    private static PlaceholderQuery.Context context(
            HillInstance hill, UUID viewer, TeamId team, int blueScore, int yellowScore) {
        Map<UUID, TeamId> teams = Map.of(viewer, team);
        return new PlaceholderQuery.Context(
                HillMode.KOTH,
                List.of(hill.hillId()),
                id -> hill.hillId().equalsIgnoreCase(id) ? hill : null,
                teams::get,
                id -> viewer.equals(id) ? hill : null,
                "Blue",
                "Yellow",
                "#5555ff",
                "#ffff55",
                blueScore,
                yellowScore);
    }
}
