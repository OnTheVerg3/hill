package com.ontheverg3.hill.game;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MatchState {
    private final AtomicBoolean paused = new AtomicBoolean();
    private volatile PointState pointState = PointState.EMPTY;

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

    public PointState pointState() {
        return pointState;
    }

    public void setPointState(PointState state) {
        this.pointState = state;
    }
}
