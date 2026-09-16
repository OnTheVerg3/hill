package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamBoardTest {
    @Test
    void swapExchangesBlueAndYellow() {
        TeamBoard board = new TeamBoard();
        UUID blue = UUID.randomUUID();
        UUID yellow = UUID.randomUUID();
        board.assign(blue, TeamId.BLUE);
        board.assign(yellow, TeamId.YELLOW);
        board.swap();
        assertEquals(TeamId.YELLOW, board.teamOf(blue));
        assertEquals(TeamId.BLUE, board.teamOf(yellow));
    }

    @Test
    void clearRemovesAssignments() {
        TeamBoard board = new TeamBoard();
        UUID player = UUID.randomUUID();
        board.assign(player, TeamId.BLUE);
        board.clear();
        assertNull(board.teamOf(player));
    }
}
