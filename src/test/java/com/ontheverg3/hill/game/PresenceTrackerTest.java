package com.ontheverg3.hill.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PresenceTrackerTest {
    @Test
    void snapshotIsExclusiveOccupancy() {
        PresenceTracker tracker = new PresenceTracker();
        UUID blue = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID yellow = UUID.fromString("00000000-0000-0000-0000-000000000002");
        tracker.remember(blue, TeamId.BLUE);
        assertEquals(PointState.CONTROLLED_BLUE, tracker.snapshot());
        tracker.remember(yellow, TeamId.YELLOW);
        assertEquals(PointState.CONTESTED, tracker.snapshot());
        tracker.remove(yellow);
        assertEquals(PointState.CONTROLLED_BLUE, tracker.snapshot());
        tracker.remove(blue);
        assertEquals(PointState.EMPTY, tracker.snapshot());
    }

    @Test
    void unassignedPlayersDoNotOccupyTheHill() {
        PresenceTracker tracker = new PresenceTracker();
        UUID unassigned = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID blue = UUID.fromString("00000000-0000-0000-0000-000000000001");
        tracker.remember(unassigned, null);
        assertEquals(0, tracker.size());
        assertEquals(PointState.EMPTY, tracker.snapshot());
        tracker.remember(blue, TeamId.BLUE);
        tracker.remember(unassigned, null);
        assertEquals(1, tracker.size());
        assertEquals(PointState.CONTROLLED_BLUE, tracker.snapshot());
        tracker.remove(unassigned);
        assertEquals(PointState.CONTROLLED_BLUE, tracker.snapshot());
    }
}
