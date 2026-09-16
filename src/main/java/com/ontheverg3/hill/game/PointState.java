package com.ontheverg3.hill.game;

public enum PointState {
    EMPTY,
    CONTESTED,
    CONTROLLED_BLUE,
    CONTROLLED_YELLOW,
    UNUSABLE;

    /** Scores only if exactly one team is present. */
    public static PointState fromPresence(boolean bluePresent, boolean yellowPresent) {
        if (bluePresent && yellowPresent) {
            return CONTESTED;
        }
        if (bluePresent) {
            return CONTROLLED_BLUE;
        }
        if (yellowPresent) {
            return CONTROLLED_YELLOW;
        }
        return EMPTY;
    }

    public boolean awardsPoints() {
        return this == CONTROLLED_BLUE || this == CONTROLLED_YELLOW;
    }

    public TeamId controllingTeamOrNull() {
        return switch (this) {
            case CONTROLLED_BLUE -> TeamId.BLUE;
            case CONTROLLED_YELLOW -> TeamId.YELLOW;
            default -> null;
        };
    }
}
