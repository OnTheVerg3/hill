package com.ontheverg3.hill.game;

import com.ontheverg3.hill.world.HillDimensions;
import com.ontheverg3.hill.zone.HillSpec;
import com.ontheverg3.hill.config.HillConfig;
import com.ontheverg3.hill.zone.ShapedZone;
import com.ontheverg3.hill.zone.UnusableZone;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class HillRegistry {
    private final ConcurrentHashMap<String, HillInstance> instances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> playerSaves = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> playerDimensions = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<TeamPad> pads = new CopyOnWriteArrayList<>();
    private final TeamBoard teams = new TeamBoard();
    private volatile HillMode mode = HillMode.KOTH;

    public TeamBoard teams() {
        return teams;
    }

    public HillMode mode() {
        return mode;
    }

    public void setMode(HillMode next) {
        this.mode = next == null ? HillMode.KOTH : next;
    }

    public HillInstance byId(String hillId) {
        if (hillId == null || hillId.isBlank()) {
            return null;
        }
        HillInstance exact = instances.get(hillId);
        if (exact != null) {
            return exact;
        }
        String lower = hillId.toLowerCase(Locale.ROOT);
        for (HillInstance instance : instances.values()) {
            if (instance.hillId().equalsIgnoreCase(lower)) {
                return instance;
            }
        }
        return null;
    }

    public Collection<HillInstance> all() {
        return List.copyOf(instances.values());
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (HillInstance instance : instances.values()) {
            ids.add(instance.hillId());
        }
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    public HillInstance occupying(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        for (HillInstance instance : instances.values()) {
            if (instance.tracker().occupies(playerId)) {
                return instance;
            }
        }
        return null;
    }

    public HillInstance containing(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        for (HillInstance instance : instances.values()) {
            if (instance.zone().contains(location)) {
                return instance;
            }
        }
        return null;
    }

    public HillInstance ofPlayer(Player player) {
        if (player == null) {
            return null;
        }
        HillInstance inside = containing(player.getLocation());
        if (inside != null) {
            return inside;
        }
        World world = player.getWorld();
        List<HillInstance> inWorld = inWorld(world);
        if (inWorld.size() == 1) {
            return inWorld.get(0);
        }
        return null;
    }

    public List<HillInstance> inWorld(World world) {
        if (world == null) {
            return List.of();
        }
        String save = HillDimensions.saveName(world);
        String dimension = HillDimensions.dimensionToken(world);
        List<HillInstance> found = new ArrayList<>();
        for (HillInstance instance : instances.values()) {
            if (save.equals(instance.save()) && dimension.equals(instance.dimension())) {
                found.add(instance);
            }
        }
        return found;
    }

    public boolean hasHillIn(String save, String dimension) {
        return !hillsIn(save, dimension).isEmpty();
    }

    public List<HillInstance> hillsIn(String save, String dimension) {
        List<HillInstance> found = new ArrayList<>();
        for (HillInstance instance : instances.values()) {
            if (instance.save().equals(save) && instance.dimension().equals(dimension)) {
                found.add(instance);
            }
        }
        return found;
    }

    public String add(HillSpec spec) {
        if (spec == null) {
            return "hill is missing";
        }
        if (byId(spec.id()) != null) {
            return "id '" + spec.id() + "' is already used";
        }
        if (mode == HillMode.KOTH && hasHillIn(spec.save(), spec.dimension())) {
            return "KotH allows only one hill per dimension in a world save";
        }
        HillInstance instance = new HillInstance(spec);
        instances.put(spec.id(), instance);
        bindSpec(instance);
        return null;
    }

    public HillInstance remove(String hillId) {
        HillInstance instance = byId(hillId);
        if (instance == null) {
            return null;
        }
        instances.remove(instance.hillId());
        instance.tracker().clear();
        instance.setZone(new UnusableZone("removed"));
        return instance;
    }

    public void clearHills() {
        for (HillInstance instance : instances.values()) {
            instance.tracker().clear();
            instance.match().setPaused(false);
            instance.match().setPointState(PointState.EMPTY);
        }
        instances.clear();
        teams.resetScores();
    }

    public void resetScores() {
        teams.resetScores();
    }

    public void replaceAll(List<HillSpec> specs, HillMode nextMode) {
        this.mode = nextMode == null ? HillMode.KOTH : nextMode;
        ConcurrentHashMap<String, HillInstance> next = new ConcurrentHashMap<>();
        if (specs != null) {
            for (HillSpec spec : specs) {
                if (spec == null || next.containsKey(spec.id())) {
                    continue;
                }
                HillInstance existing = instances.get(spec.id());
                if (existing == null) {
                    existing = new HillInstance(spec);
                } else {
                    existing.setSpec(spec);
                }
                next.put(spec.id(), existing);
            }
        }
        instances.keySet().removeIf(id -> !next.containsKey(id));
        instances.putAll(next);
        for (World world : Bukkit.getWorlds()) {
            bindWorld(world);
        }
        for (HillInstance instance : instances.values()) {
            bindSpec(instance);
        }
    }

    public void bindWorld(World world) {
        if (world == null) {
            return;
        }
        for (HillInstance instance : instances.values()) {
            if (matchesWorld(instance.spec(), world)) {
                instance.setZone(new ShapedZone(world, instance.spec()));
                if (instance.match().pointState() == PointState.UNUSABLE) {
                    instance.match().setPointState(PointState.EMPTY);
                }
            }
        }
    }

    public void unbindWorld(World world) {
        if (world == null) {
            return;
        }
        for (HillInstance instance : instances.values()) {
            if (!matchesWorld(instance.spec(), world)) {
                continue;
            }
            instance.setZone(new UnusableZone("world unloaded: " + world.getName()));
            instance.match().setPointState(PointState.UNUSABLE);
            instance.tracker().clear();
        }
    }

    public void rememberPlayer(Player player) {
        if (player == null || !player.isOnline() || player.getWorld() == null) {
            forgetPlayer(player == null ? null : player.getUniqueId());
            return;
        }
        UUID id = player.getUniqueId();
        playerSaves.put(id, HillDimensions.saveName(player.getWorld()));
        playerDimensions.put(id, HillDimensions.dimensionToken(player.getWorld()));
    }

    public void forgetPlayer(UUID id) {
        if (id == null) {
            return;
        }
        playerSaves.remove(id);
        playerDimensions.remove(id);
        for (HillInstance instance : instances.values()) {
            instance.tracker().remove(id);
        }
    }

    public String saveOf(UUID id) {
        return id == null ? null : playerSaves.get(id);
    }

    public boolean sameSave(Player player, String save) {
        if (player == null || save == null) {
            return false;
        }
        String known = playerSaves.get(player.getUniqueId());
        return save.equals(known);
    }

    public List<TeamPad> pads() {
        return List.copyOf(pads);
    }

    public void replacePads(List<TeamPad> next) {
        pads.clear();
        if (next != null) {
            pads.addAll(next);
        }
    }

    public void addPad(TeamPad pad) {
        if (pad != null) {
            pads.add(pad);
        }
    }

    public TeamPad padAt(Location location) {
        if (location == null) {
            return null;
        }
        for (TeamPad pad : pads) {
            if (pad.contains(location)) {
                return pad;
            }
        }
        return null;
    }

    public boolean removePadAt(Location location) {
        TeamPad pad = padAt(location);
        if (pad == null) {
            return false;
        }
        return pads.remove(pad);
    }

    public boolean applyTeamPad(Player player, HillConfig config) {
        return applyTeamPad(player, config, player == null ? null : player.getLocation());
    }

    public boolean applyTeamPad(Player player, HillConfig config, Location at) {
        if (player == null || config == null || config.assignExcluded(player)) {
            return false;
        }
        Location here = at != null ? at : player.getLocation();
        TeamPad pad = padAt(here);
        if (pad == null) {
            return false;
        }
        UUID id = player.getUniqueId();
        if (teams.teamOf(id) == pad.team()) {
            return false;
        }
        teams.assign(id, pad.team());
        return true;
    }

    private void bindSpec(HillInstance instance) {
        HillSpec spec = instance.spec();
        World world = Bukkit.getWorld(spec.world());
        if (world == null) {
            world = HillDimensions.worldOrNull(spec.save(), spec.dimension());
        }
        if (world == null) {
            instance.setZone(new UnusableZone("world not loaded: " + spec.world()));
            instance.match().setPointState(PointState.UNUSABLE);
            return;
        }
        instance.setZone(new ShapedZone(world, spec));
        if (instance.match().pointState() == PointState.UNUSABLE) {
            instance.match().setPointState(PointState.EMPTY);
        }
    }

    private static boolean matchesWorld(HillSpec spec, World world) {
        if (spec == null || world == null) {
            return false;
        }
        if (spec.world() != null && spec.world().equalsIgnoreCase(world.getName())) {
            return true;
        }
        return spec.save().equals(HillDimensions.saveName(world))
                && spec.dimension().equals(HillDimensions.dimensionToken(world));
    }
}
