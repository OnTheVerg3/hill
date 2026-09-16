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
}
