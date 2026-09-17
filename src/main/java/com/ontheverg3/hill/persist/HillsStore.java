package com.ontheverg3.hill.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class HillsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Logger log;
    private final File file;
    private final Object lock = new Object();
    private volatile boolean persistable;

    public HillsStore(JavaPlugin plugin) {
        this(plugin.getLogger(), new File(plugin.getDataFolder(), "hills.json"));
    }

    HillsStore(Logger log, File file) {
        this.log = log;
        this.file = file;
    }

    public boolean persistable() {
        return persistable;
    }

    public boolean load(HillRegistry registry) {
        synchronized (lock) {
            return loadUnlocked(registry, false);
        }
    }

    public boolean reload(HillRegistry registry) {
        synchronized (lock) {
            return loadUnlocked(registry, true);
        }
    }

    public void save(HillRegistry registry) {
        synchronized (lock) {
            saveUnlocked(registry);
        }
    }

    private boolean loadUnlocked(HillRegistry registry, boolean rewriteAfterRead) {
        ParsedFile parsed = readUnlocked();
        if (!parsed.ok) {
            persistable = false;
            log.severe("Could not read hills.json: " + parsed.error);
            log.severe("The file was left unchanged. Hill geometry will not be saved until a successful load.");
            return false;
        }
        applyUnlocked(parsed.model, registry);
        persistable = true;
        if (rewriteAfterRead) {
            saveUnlocked(registry);
        }
        return true;
    }

    private void applyUnlocked(FileModel model, HillRegistry registry) {
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
        for (PadModel pad : model.pads) {
            TeamPad parsed = toPad(pad);
            if (parsed != null) {
                pads.add(parsed);
            }
        }
        registry.replacePads(pads);
    }

    private void saveUnlocked(HillRegistry registry) {
        if (!persistable) {
            log.severe("Skipped writing hills.json because the last load failed.");
            return;
        }
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
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
            log.log(Level.SEVERE, "Could not save hills.json", ex);
        }
    }

    private ParsedFile readUnlocked() {
        if (!file.exists()) {
            return ParsedFile.ok(new FileModel());
        }
        try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return parse(raw);
        } catch (IOException ex) {
            return ParsedFile.fail(ex.getMessage() == null ? "unreadable" : ex.getMessage());
        }
    }

    static ParsedFile parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ParsedFile.fail("hills.json is empty or unreadable");
        }
        try {
            FileModel model = GSON.fromJson(raw, FileModel.class);
            if (model == null) {
                return ParsedFile.fail("hills.json decoded as null");
            }
            normalize(model);
            return ParsedFile.ok(model);
        } catch (RuntimeException ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return ParsedFile.fail(detail);
        }
    }

    static void normalize(FileModel model) {
        if (model.hills == null) {
            model.hills = new ArrayList<>();
        }
        if (model.pads == null) {
            model.pads = new ArrayList<>();
        }
        if (model.mode == null || model.mode.isBlank()) {
            model.mode = HillMode.KOTH.id();
        }
    }

    private HillSpec toSpec(HillModel model) {
        if (model == null || model.id == null) {
            log.warning("Ignored a hill in hills.json with no id");
            return null;
        }
        String id = HillIds.sanitizeId(model.id);
        if (!HillIds.isId(id)) {
            log.warning("Ignored invalid hill id in hills.json: " + model.id);
            return null;
        }
        HillShape shape = HillShape.parse(model.shape).orElse(null);
        if (shape == null) {
            log.warning("Ignored hill '" + id + "': unknown shape " + model.shape);
            return null;
        }
        if (!FiniteNumbers.allFinite(model.x, model.y, model.z, model.rx, model.ry, model.rz)) {
            log.warning("Ignored hill '" + id + "': coordinates and radius must be finite");
            return null;
        }
        if (model.rx <= 0 || model.ry <= 0 || model.rz <= 0) {
            log.warning("Ignored hill '" + id + "': radius must be positive");
            return null;
        }
        String world = model.world == null || model.world.isBlank() ? "world" : model.world;
        String save = model.save == null || model.save.isBlank() ? HillIds.saveName(world) : model.save;
        String dimension =
                model.dimension == null || model.dimension.isBlank() ? HillIds.OVERWORLD : model.dimension;
        String display = model.display == null || model.display.isBlank() ? id : model.display;
        return new HillSpec(
                id, display, world, save, dimension, shape, model.x, model.y, model.z, model.rx, model.ry, model.rz);
    }

    private TeamPad toPad(PadModel model) {
        if (model == null) {
            return null;
        }
        TeamId team = TeamId.parse(model.team).orElse(null);
        if (team == null) {
            log.warning("Ignored a team pad in hills.json with team " + model.team);
            return null;
        }
        String world = model.world == null || model.world.isBlank() ? "world" : model.world;
        String save = model.save == null || model.save.isBlank() ? HillIds.saveName(world) : model.save;
        String dimension =
                model.dimension == null || model.dimension.isBlank() ? HillIds.OVERWORLD : model.dimension;
        if (!FiniteNumbers.allFinite(
                model.minX, model.minY, model.minZ, model.maxX, model.maxY, model.maxZ)) {
            log.warning("Ignored a team pad in hills.json with non-finite coordinates");
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

    static final class ParsedFile {
        final boolean ok;
        final String error;
        final FileModel model;

        private ParsedFile(boolean ok, String error, FileModel model) {
            this.ok = ok;
            this.error = error;
            this.model = model;
        }

        static ParsedFile ok(FileModel model) {
            return new ParsedFile(true, "", model);
        }

        static ParsedFile fail(String error) {
            return new ParsedFile(false, error == null ? "unreadable" : error, null);
        }
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
