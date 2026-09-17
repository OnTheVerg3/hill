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

    @Test
    void awardsAccumulateOnTheGlobalBoard() {
        TeamBoard board = new TeamBoard();
        board.award(TeamId.BLUE, 1, 0);
        board.award(TeamId.BLUE, 1, 0);
        board.award(TeamId.YELLOW, 1, 0);
        assertEquals(2, board.score(TeamId.BLUE));
        assertEquals(1, board.score(TeamId.YELLOW));
    }

    @Test
    void swapScoresExchangesBlueAndYellow() {
        TeamBoard board = new TeamBoard();
        board.setScore(TeamId.BLUE, 7);
        board.setScore(TeamId.YELLOW, 2);
        board.swapScores();
        assertEquals(2, board.score(TeamId.BLUE));
        assertEquals(7, board.score(TeamId.YELLOW));
    }

    @Test
    void addScoreDoesNotGoBelowZero() {
        TeamBoard board = new TeamBoard();
        board.setScore(TeamId.BLUE, 3);
        board.addScore(TeamId.BLUE, -10);
        assertEquals(0, board.score(TeamId.BLUE));
    }

    @Test
    void resetScoresLeavesAssignments() {
        TeamBoard board = new TeamBoard();
        UUID player = UUID.randomUUID();
        board.assign(player, TeamId.YELLOW);
        board.award(TeamId.YELLOW, 4, 0);
        board.resetScores();
        assertEquals(0, board.score(TeamId.YELLOW));
        assertEquals(0, board.score(TeamId.BLUE));
        assertEquals(TeamId.YELLOW, board.teamOf(player));
    }
}
