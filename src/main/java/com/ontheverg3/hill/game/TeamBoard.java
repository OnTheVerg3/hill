package com.ontheverg3.hill.game;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class TeamBoard {
    private final ConcurrentHashMap<UUID, TeamId> teams = new ConcurrentHashMap<>();

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
}
