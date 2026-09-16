package com.ontheverg3.hill.game;

import com.ontheverg3.hill.zone.CaptureZone;
import com.ontheverg3.hill.zone.HillSpec;
import com.ontheverg3.hill.zone.UnusableZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class HillInstance {
    private final AtomicReference<HillSpec> spec;
    private final MatchState match = new MatchState();
    private final PresenceTracker tracker = new PresenceTracker();
    private final AtomicReference<CaptureZone> zone = new AtomicReference<>(new UnusableZone("not bound"));
    private final AtomicInteger lastScheduled = new AtomicInteger();

    public HillInstance(HillSpec spec) {
        this.spec = new AtomicReference<>(spec);
    }

    public HillSpec spec() {
        return spec.get();
    }

    public void setSpec(HillSpec replacement) {
        spec.set(replacement);
    }

    public String save() {
        return spec.get().save();
    }

    public String hillId() {
        return spec.get().id();
    }

    public String display() {
        return spec.get().display();
    }

    public String dimension() {
        return spec.get().dimension();
    }

    public String worldName() {
        return spec.get().world();
    }

    public MatchState match() {
        return match;
    }

    public PresenceTracker tracker() {
        return tracker;
    }

    public CaptureZone zone() {
        return zone.get();
    }

    public void setZone(CaptureZone replacement) {
        zone.set(replacement == null ? new UnusableZone("not bound") : replacement);
    }

    public int lastScheduled() {
        return lastScheduled.get();
    }

    public void setLastScheduled(int value) {
        lastScheduled.set(value);
    }
}
