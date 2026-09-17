package com.ontheverg3.hill.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.ontheverg3.hill.config.FiniteNumbers;
import com.ontheverg3.hill.game.HillInstance;
import com.ontheverg3.hill.game.HillMode;
import com.ontheverg3.hill.game.HillRegistry;
import com.ontheverg3.hill.game.TeamId;
import com.ontheverg3.hill.game.TeamPad;
import com.ontheverg3.hill.world.HillIds;
import com.ontheverg3.hill.zone.HillShape;
import com.ontheverg3.hill.zone.HillSpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class HillsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final JavaPlugin plugin;
    private final File file;
    private final Object lock = new Object();

    public HillsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hills.json");
    }

    public void load(HillRegistry registry) {
        synchronized (lock) {
            FileModel model = readUnlocked();
            List<HillSpec> specs = new ArrayList<>();
            for (HillModel hill : model.hills) {
                HillSpec spec = toSpec(hill);
                if (spec == null) {
                    continue;
                }
                specs.add(spec);
            }
            registry.replaceAll(specs, HillMode.parse(model.mode).orElse(HillMode.KOTH));
            List<TeamPad> pads = new ArrayList<>();
            if (model.pads != null) {
                for (PadModel pad : model.pads) {
                    TeamPad parsed = toPad(pad);
                    if (parsed != null) {
                        pads.add(parsed);
                    }
                }
            }
            registry.replacePads(pads);
        }
    }

    public void save(HillRegistry registry) {
        synchronized (lock) {
            plugin.getDataFolder().mkdirs();
            FileModel model = new FileModel();
            model.mode = registry.mode().id();
            for (HillInstance instance : registry.all()) {
                model.hills.add(fromSpec(instance.spec()));
            }
            for (TeamPad pad : registry.pads()) {
                model.pads.add(fromPad(pad));
            }
            Path target = file.toPath();
            Path temp = target.resolveSibling("hills.json.tmp");
            try {
                Files.writeString(temp, GSON.toJson(model), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not save hills.json", ex);
            }
        }
    }

    private FileModel readUnlocked() {
        if (!file.exists()) {
            return new FileModel();
        }
        try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return new FileModel();
            }
            FileModel model = GSON.fromJson(raw, FileModel.class);
            return model == null ? new FileModel() : model;
        } catch (IOException | JsonParseException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not read hills.json", ex);
            return new FileModel();
        }
    }

    private HillSpec toSpec(HillModel model) {
        if (model == null || model.id == null) {
            plugin.getLogger().warning("Ignored a hill in hills.json with no id");
            return null;
        }
        String id = HillIds.sanitizeId(model.id);
        if (!HillIds.isId(id)) {
            plugin.getLogger().warning("Ignored invalid hill id in hills.json: " + model.id);
            return null;
        }
        HillShape shape = HillShape.parse(model.shape).orElse(null);
        if (shape == null) {
            plugin.getLogger().warning("Ignored hill '" + id + "': unknown shape " + model.shape);
            return null;
        }
        if (!FiniteNumbers.allFinite(model.x, model.y, model.z, model.rx, model.ry, model.rz)) {
            plugin.getLogger().warning("Ignored hill '" + id + "': coordinates and radius must be finite");
            return null;
        }
        if (model.rx <= 0 || model.ry <= 0 || model.rz <= 0) {
            plugin.getLogger().warning("Ignored hill '" + id + "': radius must be positive");
            return null;
        }
        String world = model.world == null || model.world.isBlank() ? "world" : model.world;
        String save = model.save == null || model.save.isBlank() ? HillIds.saveName(world) : model.save;
        String dimension =
                model.dimension == null || model.dimension.isBlank() ? HillIds.OVERWORLD : model.dimension;
        String display = model.display == null || model.display.isBlank() ? id : model.display;
        return new HillSpec(id, display, world, save, dimension, shape, model.x, model.y, model.z, model.rx, model.ry, model.rz);
    }

    private TeamPad toPad(PadModel model) {
        if (model == null) {
            return null;
        }
        TeamId team = TeamId.parse(model.team).orElse(null);
        if (team == null) {
            plugin.getLogger().warning("Ignored a team pad in hills.json with team " + model.team);
            return null;
        }
        String world = model.world == null || model.world.isBlank() ? "world" : model.world;
        String save = model.save == null || model.save.isBlank() ? HillIds.saveName(world) : model.save;
        String dimension =
                model.dimension == null || model.dimension.isBlank() ? HillIds.OVERWORLD : model.dimension;
        if (!FiniteNumbers.allFinite(
                model.minX, model.minY, model.minZ, model.maxX, model.maxY, model.maxZ)) {
            plugin.getLogger().warning("Ignored a team pad in hills.json with non-finite coordinates");
            return null;
        }
        return new TeamPad(
                team, world, save, dimension, model.minX, model.minY, model.minZ, model.maxX, model.maxY, model.maxZ);
    }

    private static PadModel fromPad(TeamPad pad) {
        PadModel model = new PadModel();
        model.team = pad.team() == TeamId.BLUE ? "blue" : "yellow";
        model.world = pad.world();
        model.save = pad.save();
        model.dimension = pad.dimension();
        model.minX = pad.minX();
        model.minY = pad.minY();
        model.minZ = pad.minZ();
        model.maxX = pad.maxX();
        model.maxY = pad.maxY();
        model.maxZ = pad.maxZ();
        return model;
    }

    private static HillModel fromSpec(HillSpec spec) {
        HillModel model = new HillModel();
        model.id = spec.id();
        model.display = spec.display();
        model.world = spec.world();
        model.save = spec.save();
        model.dimension = spec.dimension();
        model.shape = spec.shape().id();
        model.x = spec.x();
        model.y = spec.y();
        model.z = spec.z();
        model.rx = spec.rx();
        model.ry = spec.ry();
        model.rz = spec.rz();
        return model;
    }

    static final class FileModel {
        String mode = HillMode.KOTH.id();
        List<HillModel> hills = new ArrayList<>();
        List<PadModel> pads = new ArrayList<>();
    }

    static final class PadModel {
        String team;
        String world;
        String save;
        String dimension;
        double minX;
        double minY;
        double minZ;
        double maxX;
        double maxY;
        double maxZ;
    }

    static final class HillModel {
        String id;
        String display;
        String world;
        String save;
        String dimension;
        String shape;
        double x;
        double y;
        double z;
        double rx;
        double ry;
        double rz;
    }
}
