package com.ontheverg3.hill.game;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public final class TeamBoard {
    private final ConcurrentHashMap<UUID, TeamId> teams = new ConcurrentHashMap<>();
    private final AtomicInteger blueScore = new AtomicInteger();
    private final AtomicInteger yellowScore = new AtomicInteger();

    public void assign(UUID id, TeamId team) {
        teams.put(id, team);
    }

    public TeamId unassign(UUID id) {
        return teams.remove(id);
    }

    public TeamId teamOf(UUID id) {
        return teams.get(id);
    }

    public Map<UUID, TeamId> view() {
        return Map.copyOf(teams);
    }

    public void forEach(BiConsumer<UUID, TeamId> consumer) {
        teams.forEach(consumer);
    }

    public void replace(Map<UUID, TeamId> incoming) {
        teams.clear();
        if (incoming != null) {
            teams.putAll(incoming);
        }
    }

    public void clear() {
        teams.clear();
    }

    public void swap() {
        teams.replaceAll((id, team) -> team == TeamId.BLUE ? TeamId.YELLOW : TeamId.BLUE);
    }

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

    public void resetScores() {
        blueScore.set(0);
        yellowScore.set(0);
    }

    public boolean award(TeamId team, int points, int winScore) {
        AtomicInteger counter = team == TeamId.BLUE ? blueScore : yellowScore;
        int next = counter.addAndGet(points);
        return winScore > 0 && next >= winScore;
    }
}
