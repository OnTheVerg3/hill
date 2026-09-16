package com.ontheverg3.hill.game;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class MatchState {
    private final AtomicInteger blueScore = new AtomicInteger();
    private final AtomicInteger yellowScore = new AtomicInteger();
    private final AtomicBoolean paused = new AtomicBoolean();
    private volatile PointState pointState = PointState.EMPTY;

    public int score(TeamId team) {
        return team == TeamId.BLUE ? blueScore.get() : yellowScore.get();
    }

    public void setScore(TeamId team, int value) {
        (team == TeamId.BLUE ? blueScore : yellowScore).set(Math.max(0, value));
    }

    public void addScore(TeamId team, int delta) {
        if (delta == 0) {
            return;
        }
        AtomicInteger counter = team == TeamId.BLUE ? blueScore : yellowScore;
        while (true) {
            int current = counter.get();
            int next = Math.max(0, current + delta);
            if (counter.compareAndSet(current, next)) {
                return;
            }
        }
    }

    public void swapScores() {
        int blue = blueScore.get();
        int yellow = yellowScore.get();
        blueScore.set(yellow);
        yellowScore.set(blue);
    }

    public boolean paused() {
        return paused.get();
    }

    public boolean pause() {
        return paused.compareAndSet(false, true);
    }

    public boolean resume() {
        return paused.compareAndSet(true, false);
    }

    public void setPaused(boolean value) {
        paused.set(value);
    }

    public void resetScores() {
        blueScore.set(0);
        yellowScore.set(0);
    }

    public PointState pointState() {
        return pointState;
    }

    public void setPointState(PointState state) {
        this.pointState = state;
    }

    public boolean award(TeamId team, int points, int winScore) {
        AtomicInteger counter = team == TeamId.BLUE ? blueScore : yellowScore;
        int next = counter.addAndGet(points);
        return winScore > 0 && next >= winScore;
    }
}
