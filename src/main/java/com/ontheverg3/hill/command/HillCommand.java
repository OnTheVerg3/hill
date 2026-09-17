package com.ontheverg3.hill.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ontheverg3.hill.HillPlugin;
import com.ontheverg3.hill.config.ConfigException;
import com.ontheverg3.hill.game.HillInstance;
import com.ontheverg3.hill.game.HillMode;
import com.ontheverg3.hill.game.PointState;
import com.ontheverg3.hill.game.TeamId;
import com.ontheverg3.hill.game.TeamPad;
import com.ontheverg3.hill.i18n.Lang;
import com.ontheverg3.hill.world.HillDimensions;
import com.ontheverg3.hill.world.HillIds;
import com.ontheverg3.hill.zone.HillShape;
import com.ontheverg3.hill.zone.HillSpec;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class HillCommand {
    private static final String[] PERMISSIONS = {
        "hill.status",
        "hill.help",
        "hill.reload",
        "hill.mode",
        "hill.new",
        "hill.remove",
        "hill.assign",
        "hill.unassign",
        "hill.swapteams",
        "hill.swapscore",
        "hill.pause",
        "hill.resume",
        "hill.reset",
        "hill.perf",
        "hill.autodivide",
        "hill.score",
        "hill.bossbar",
        "hill.pad",
        "hill.admin"
    };
    private static final long MODE_CONFIRM_NANOS = 60_000_000_000L;
    private static final double DEFAULT_FLAT_RY = 16.0;

    private final HillPlugin plugin;
    private final ConcurrentHashMap<String, PendingMode> pendingModes = new ConcurrentHashMap<>();

    public HillCommand(HillPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSourceStack stack, String[] args) {
        String joined = args == null || args.length == 0 ? "" : String.join(" ", args);
        List<String> tokens = Quoted.split(joined);
        dispatch(stack.getSender(), tokens.toArray(String[]::new));
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        var root = Commands.literal("hill")
                .requires(stack -> canUse(stack.getSender()))
                .executes(ctx -> {
                    execute(ctx.getSource(), new String[0]);
                    return 1;
                });
        for (HillHelp.Topic topic : HillHelp.all()) {
            root = root.then(leaf(topic));
        }
        return root.build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> leaf(HillHelp.Topic topic) {
        return Commands.literal(topic.name())
                .requires(stack -> stack.getSender().hasPermission(topic.permission()))
                .executes(ctx -> {
                    execute(ctx.getSource(), new String[] {topic.name()});
                    return 1;
                })
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) ->
                                suggestGreedy(ctx.getSource().getSender(), topic.name(), builder))
                        .executes(ctx -> {
                            String rest = StringArgumentType.getString(ctx, "args");
                            List<String> tokens = Quoted.split(topic.name() + " " + rest);
                            execute(ctx.getSource(), tokens.toArray(String[]::new));
                            return 1;
                        }));
    }

    private CompletableFuture<Suggestions> suggestGreedy(
            CommandSender sender, String sub, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        List<String> args = new ArrayList<>();
        args.add(sub);
        if (remaining.isEmpty()) {
            args.add("");
        } else {
            args.addAll(Quoted.split(remaining));
            if (remaining.endsWith(" ")) {
                args.add("");
            }
        }
        List<String> options = onTabComplete(sender, args.toArray(String[]::new));
        int lastSpace = remaining.lastIndexOf(' ');
        String prefix = lastSpace >= 0 ? remaining.substring(0, lastSpace + 1) : "";
        String current = lastSpace >= 0 ? remaining.substring(lastSpace + 1) : remaining;
        String lower = current.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                builder.suggest(prefix + option);
            }
        }
        return builder.buildFuture();
    }

    public boolean canUse(CommandSender sender) {
        for (String node : PERMISSIONS) {
            if (sender.hasPermission(node)) {
                return true;
            }
        }
        return false;
    }

    public boolean dispatch(CommandSender sender, String[] args) {
        Lang lang = plugin.lang();
        if (args.length == 0) {
            help(sender, null);
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            help(sender, args.length >= 2 ? args[1] : null);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "mode" -> mode(sender, args);
            case "new" -> create(sender, args);
            case "remove" -> remove(sender, args);
            case "assign" -> assign(sender, args);
            case "unassign" -> unassign(sender, args);
            case "status" -> status(sender, args);
            case "swapteams" -> swapTeams(sender);
            case "swapscore" -> swapScore(sender);
            case "pause" -> pause(sender, args);
            case "resume" -> resume(sender, args);
            case "reset" -> reset(sender);
            case "perf" -> perf(sender, args);
            case "autodivide" -> autodivide(sender, args);
            case "score" -> score(sender, args);
            case "bossbar" -> bossBar(sender, args);
            case "pad" -> pad(sender, args);
            default -> lang.send(sender, "error-unknown-subcommand", lang.unparsed("name", args[0]));
        }
        return true;
    }

    private void help(CommandSender sender, String topicName) {
        if (deny(sender, "hill.help")) {
            return;
        }
        Lang lang = plugin.lang();
        if (topicName == null || topicName.isBlank()) {
            lang.send(sender, "help-header");
            lang.send(sender, "help-hint");
            for (HillHelp.Topic topic : HillHelp.all()) {
                if (!sender.hasPermission(topic.permission())) {
                    continue;
                }
                lang.send(
                        sender,
                        "help-entry",
                        lang.unparsed("name", topic.name()),
                        lang.unparsed("summary", topic.summary()));
            }
            return;
        }
        var found = HillHelp.byName(topicName);
        if (found.isEmpty()) {
            lang.send(sender, "help-unknown", lang.unparsed("name", topicName));
            return;
        }
        HillHelp.Topic topic = found.get();
        lang.send(sender, "help-topic-name", lang.unparsed("name", topic.name()));
        lang.send(sender, "help-topic-summary", lang.unparsed("summary", topic.summary()));
        lang.send(sender, "help-topic-usage-title");
        lang.send(sender, "help-topic-usage", lang.unparsed("usage", topic.usage()));
        if (!topic.examples().isEmpty()) {
            lang.send(sender, "help-topic-examples-title");
            for (String example : topic.examples()) {
                lang.send(sender, "help-topic-example", lang.unparsed("example", example));
            }
        }
        if (!topic.notes().isBlank()) {
            lang.send(sender, "help-topic-notes-title");
            lang.send(sender, "help-topic-notes", lang.unparsed("notes", topic.notes()));
        }
        lang.send(sender, "help-topic-permission", lang.unparsed("node", topic.permission()));
        lang.send(sender, "help-topic-back");
    }

    private boolean deny(CommandSender sender, String node) {
        if (sender.hasPermission(node)) {
            return false;
        }
        plugin.lang().send(sender, "error-no-permission");
        return true;
    }

    private void reload(CommandSender sender) {
        if (deny(sender, "hill.reload")) {
            return;
        }
        try {
            plugin.reloadAll();
            plugin.lang().send(sender, "reload-ok");
        } catch (ConfigException ex) {
            plugin.lang().send(sender, "error-config", plugin.lang().text("detail", ex.getMessage()));
        }
    }

    private void mode(CommandSender sender, String[] args) {
        if (deny(sender, "hill.mode")) {
            return;
        }
        Lang lang = plugin.lang();
        if (args.length < 2) {
            lang.send(sender, "error-usage", lang.unparsed("usage", "/hill mode <ctf|koth>"));
            return;
        }
        var parsed = HillMode.parse(args[1]);
        if (parsed.isEmpty()) {
            lang.send(sender, "error-invalid-mode");
            return;
        }
        HillMode next = parsed.get();
        String action = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
        String token = args.length >= 4 ? args[3] : "";
        if (action.equals("confirm")) {
            confirmMode(sender, next, token);
            return;
        }
        if (action.equals("cancel")) {
            cancelMode(sender, next, token);
            return;
        }
        if (plugin.hills().mode() == next) {
            lang.send(sender, "error-mode-same", lang.text("mode", next.display()));
            return;
        }
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        pendingModes.put(nonce, new PendingMode(actorId(sender), next, nonce, System.nanoTime() + MODE_CONFIRM_NANOS));
        lang.send(sender, "mode-warn", lang.text("mode", next.display()));
        lang.send(
                sender,
                "mode-confirm-line",
                lang.unparsed("mode", next.id()),
                lang.unparsed("token", nonce));
    }

    private void confirmMode(CommandSender sender, HillMode next, String token) {
        Lang lang = plugin.lang();
        PendingMode pending = pendingModes.remove(token);
        if (pending == null || pending.expiresAt() < System.nanoTime() || pending.mode() != next) {
            lang.send(sender, "error-mode-expired");
            return;
        }
        if (!pending.actor().equals(actorId(sender))) {
            lang.send(sender, "error-mode-expired");
            return;
        }
        plugin.hills().clearHills();
        plugin.hills().resetScores();
        plugin.hills().setMode(next);
        plugin.persistHills();
        plugin.persistMatch();
        plugin.scoring().resyncOnline();
        lang.send(sender, "mode-ok", lang.text("mode", next.display()));
    }

    private void cancelMode(CommandSender sender, HillMode next, String token) {
        PendingMode pending = pendingModes.remove(token);
        if (pending != null && pending.mode() != next) {
            pendingModes.put(token, pending);
        }
        plugin.lang().send(sender, "mode-cancelled");
    }

    private void create(CommandSender sender, String[] args) {
        if (deny(sender, "hill.new")) {
            return;
        }
        Lang lang = plugin.lang();
        String usage = "/hill new <x|~> <y|~> <z|~> <radius|rx ry rz> <square|circle|cube|sphere|cylinder> <id> [\"display name\"]";
        if (args.length < 7) {
            lang.send(sender, "error-usage", lang.unparsed("usage", usage));
            return;
        }
        Location origin = originOf(sender);
        boolean relativeOk = sender instanceof Player;
        double x;
        double y;
        double z;
        try {
            x = parseCoord(args[1], origin == null ? 0 : origin.getX(), relativeOk);
            y = parseCoord(args[2], origin == null ? 0 : origin.getY(), relativeOk);
            z = parseCoord(args[3], origin == null ? 0 : origin.getZ(), relativeOk);
        } catch (IllegalArgumentException ex) {
            if ("relative".equals(ex.getMessage())) {
                lang.send(sender, "error-relative-console");
            } else {
                lang.send(sender, "error-usage", lang.unparsed("usage", usage));
            }
            return;
        }
        int shapeIndex;
        double rx;
        double ry;
        double rz;
        if (args.length >= 9 && looksNumber(args[4]) && looksNumber(args[5]) && looksNumber(args[6])) {
            rx = parsePositive(args[4]);
            ry = parsePositive(args[5]);
            rz = parsePositive(args[6]);
            shapeIndex = 7;
        } else {
            rx = parsePositive(args[4]);
            ry = -1;
            rz = rx;
            shapeIndex = 5;
        }
        if (rx <= 0 || (ry != -1 && ry <= 0) || rz <= 0) {
            lang.send(sender, "error-invalid-radius");
            return;
        }
        if (args.length <= shapeIndex + 1) {
            lang.send(sender, "error-usage", lang.unparsed("usage", usage));
            return;
        }
        var shape = HillShape.parse(args[shapeIndex]);
        if (shape.isEmpty()) {
            lang.send(sender, "error-invalid-shape");
            return;
        }
        if (ry < 0) {
            ry = shape.get() == HillShape.SQUARE || shape.get() == HillShape.CIRCLE ? DEFAULT_FLAT_RY : rx;
        }
        String idRaw = args[shapeIndex + 1];
        String id = HillIds.sanitizeId(idRaw);
        if (!HillIds.isId(id)) {
            lang.send(sender, "error-invalid-id");
            return;
        }
        String display = id;
        if (args.length > shapeIndex + 2) {
            display = String.join(" ", java.util.Arrays.copyOfRange(args, shapeIndex + 2, args.length)).trim();
            if (display.isEmpty()) {
                display = id;
            }
        }
        World world = worldOf(sender);
        if (world == null) {
            lang.send(sender, "error-point-unusable");
            return;
        }
        if (plugin.hills().byId(id) != null) {
            lang.send(sender, "error-duplicate-hill", lang.text("id", id));
            return;
        }
        HillSpec spec = new HillSpec(
                id,
                display,
                world.getName(),
                HillDimensions.saveName(world),
                HillDimensions.dimensionToken(world),
                shape.get(),
                x,
                y,
                z,
                rx,
                ry,
                rz);
        String error = plugin.hills().add(spec);
        if (error != null && error.contains("KotH")) {
            lang.send(sender, "error-koth-limit");
            return;
        }
        if (error != null) {
            lang.send(sender, "error-duplicate-hill", lang.text("id", id));
            return;
        }
        plugin.persistHills();
        plugin.persistMatch();
        plugin.scoring().resyncOnline();
        String locText = (int) Math.floor(x) + " " + (int) Math.floor(y) + " " + (int) Math.floor(z);
        lang.send(
                sender,
                "new-ok",
                lang.text("hill", display),
                lang.text("id", id),
                lang.text("shape", shape.get().id()),
                lang.text("loc", locText));
    }

    private void remove(CommandSender sender, String[] args) {
        if (deny(sender, "hill.remove")) {
            return;
        }
        Lang lang = plugin.lang();
        if (args.length < 2) {
            lang.send(sender, "error-usage", lang.unparsed("usage", "/hill remove <id|all>"));
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int count = plugin.hills().all().size();
            plugin.hills().clearHills();
            plugin.persistHills();
            plugin.persistMatch();
            plugin.scoring().resyncOnline();
            lang.send(sender, "remove-all-ok", lang.number("count", count));
            return;
        }
        HillInstance removed = plugin.hills().remove(args[1]);
        if (removed == null) {
            lang.send(sender, "error-unknown-hill", lang.text("id", args[1]));
            return;
        }
        plugin.persistHills();
        plugin.persistMatch();
        plugin.scoring().resyncOnline();
        lang.send(sender, "remove-ok", lang.text("id", removed.hillId()));
    }

    private void assign(CommandSender sender, String[] args) {
        if (deny(sender, "hill.assign")) {
            return;
        }
        Lang lang = plugin.lang();
        if (args.length < 3) {
            lang.send(sender, "error-usage", lang.unparsed("usage", "/hill assign <player|selector> <blue|yellow>"));
            return;
        }
        var team = TeamId.parse(args[2], plugin.config().teams());
        if (team.isEmpty()) {
            lang.send(sender, "error-invalid-team");
            return;
        }
        String selector = args[1];
        List<Player> targets = Players.resolve(sender, selector);
        if (targets.isEmpty() && !Players.isSelector(selector)) {
            lang.send(sender, "error-player-not-found", lang.text("name", selector));
            return;
        }
        if (targets.isEmpty()) {
            lang.send(sender, "error-selector");
            return;
        }
        boolean filter = !Players.overridesAssignFilter(selector);
        int assigned = 0;
        int skipped = 0;
        for (Player player : targets) {
            if (filter && Players.excludedByDefault(player, plugin.config())) {
                skipped++;
                continue;
            }
            plugin.hills().teams().assign(player.getUniqueId(), team.get());
            plugin.scoring().scheduleSync(player);
            assigned++;
        }
        plugin.persistMatch();
        lang.send(
                sender,
                "assign-ok",
                lang.number("count", assigned),
                lang.component("team", lang.hud(team.get().langKey())));
        if (skipped > 0) {
            lang.send(sender, "assign-skipped", lang.number("skipped", skipped));
        }
    }

    private void unassign(CommandSender sender, String[] args) {
        if (deny(sender, "hill.unassign")) {
            return;
        }
        Lang lang = plugin.lang();
        if (args.length < 2) {
            lang.send(sender, "error-usage", lang.unparsed("usage", "/hill unassign <player|selector|all>"));
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            plugin.hills().teams().clear();
            for (HillInstance hill : plugin.hills().all()) {
                plugin.scoring().clearPresence(hill);
            }
            plugin.persistMatch();
            plugin.scoring().resyncOnline();
            lang.send(sender, "unassignall-ok");
            return;
        }
        List<Player> targets = Players.resolve(sender, args[1]);
        if (targets.isEmpty() && !Players.isSelector(args[1])) {
            lang.send(sender, "error-player-not-found", lang.text("name", args[1]));
            return;
        }
        if (targets.isEmpty()) {
            lang.send(sender, "error-selector");
            return;
        }
        int count = 0;
        for (Player player : targets) {
            if (plugin.hills().teams().unassign(player.getUniqueId()) != null) {
                count++;
            }
            plugin.scoring().scheduleSync(player);
        }
        plugin.persistMatch();
        lang.send(sender, count == 0 ? "unassign-none" : "unassign-ok", lang.number("count", count));
    }

    private void status(CommandSender sender, String[] args) {
        if (deny(sender, "hill.status")) {
            return;
        }
        Lang lang = plugin.lang();
        if (args.length < 2) {
            lang.send(sender, "error-usage", lang.unparsed("usage", "/hill status <hill id>"));
            return;
        }
        HillInstance hill = plugin.hills().byId(args[1]);
        if (hill == null) {
            lang.send(sender, "error-unknown-hill", lang.text("id", args[1]));
            return;
        }
        var match = hill.match();
        var board = plugin.hills().teams();
        lang.send(sender, "status-mode", lang.text("mode", plugin.hills().mode().display()));
        lang.send(
                sender,
                "status-header",
                lang.text("hill", hill.display()),
                lang.text("id", hill.hillId()),
                lang.text("shape", hill.spec().shape().id()),
                lang.text("world", hill.worldName()));
        lang.send(sender, match.paused() ? "status-paused" : "status-running");
        lang.send(sender, "status-blue", lang.number("score", board.score(TeamId.BLUE)));
        lang.send(sender, "status-yellow", lang.number("score", board.score(TeamId.YELLOW)));
        String pointKey = switch (match.pointState()) {
            case EMPTY -> "status-point-empty";
            case CONTESTED -> "status-point-contested";
            case CONTROLLED_BLUE -> "status-point-blue";
            case CONTROLLED_YELLOW -> "status-point-yellow";
            case UNUSABLE -> "status-point-unusable";
        };
        sender.sendMessage(lang.chat(pointKey));
        int blueOnline = 0;
        int yellowOnline = 0;
        int unassigned = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            TeamId team = plugin.hills().teams().teamOf(player.getUniqueId());
            if (team == TeamId.BLUE) {
                blueOnline++;
            } else if (team == TeamId.YELLOW) {
                yellowOnline++;
            } else {
                unassigned++;
            }
        }
        lang.send(
                sender,
                "status-roster",
                lang.number("blue_online", blueOnline),
                lang.number("yellow_online", yellowOnline),
                lang.number("unassigned", unassigned));
        if (match.pointState() == PointState.UNUSABLE) {
            lang.send(sender, "error-point-unusable");
        }
    }

    private void swapTeams(CommandSender sender) {
        if (deny(sender, "hill.swapteams")) {
            return;
        }
        plugin.hills().teams().swap();
        plugin.persistMatch();
        plugin.scoring().resyncOnline();
        plugin.lang().send(sender, "swapteams-ok");
    }

    private void swapScore(CommandSender sender) {
        if (deny(sender, "hill.swapscore")) {
            return;
        }
        plugin.hills().teams().swapScores();
        plugin.persistMatch();
        plugin.lang().send(sender, "swapscore-ok");
    }

    private void pause(CommandSender sender, String[] args) {
        if (deny(sender, "hill.pause")) {
            return;
        }
        mutatePause(sender, args, true, "/hill pause <hill id|all>");
    }

    private void resume(CommandSender sender, String[] args) {
        if (deny(sender, "hill.resume")) {
            return;
        }
        mutatePause(sender, args, false, "/hill resume <hill id|all>");
    }

    private void mutatePause(CommandSender sender, String[] args, boolean pausing, String usage) {
        Lang lang = plugin.lang();
        List<HillInstance> hills = resolveHills(sender, args, 1, usage);
        if (hills == null) {
            return;
        }
        String keyOk = pausing ? "pause-ok" : "resume-ok";
        String keyAlready = pausing ? "pause-already" : "resume-already";
        boolean changed = false;
        for (HillInstance hill : hills) {
            boolean ok = pausing ? hill.match().pause() : hill.match().resume();
            lang.send(sender, ok ? keyOk : keyAlready, lang.text("hill", hill.display()));
            changed |= ok;
        }
        if (changed) {
            plugin.persistMatch();
        }
    }

    private void reset(CommandSender sender) {
        if (deny(sender, "hill.reset")) {
            return;
        }
        plugin.hills().resetScores();
        if (plugin.config().resetClearsTeams()) {
            plugin.hills().teams().clear();
            for (HillInstance hill : plugin.hills().all()) {
                plugin.scoring().clearPresence(hill);
            }
            plugin.scoring().resyncOnline();
        }
        plugin.persistMatch();
        plugin.lang().send(sender, "reset-ok");
    }

    private void perf(CommandSender sender, String[] args) {
        if (deny(sender, "hill.perf")) {
            return;
        }
        List<HillInstance> hills = resolveHills(sender, args, 1, "/hill perf <hill id|all>");
        if (hills == null) {
            return;
        }
        var scoring = plugin.scoring();
        Lang lang = plugin.lang();
        double ms = scoring.lastSampleNanos() / 1_000_000.0;
        double maxMs = scoring.maxSampleNanos() / 1_000_000.0;
        for (HillInstance hill : hills) {
            lang.send(
                    sender,
                    "perf-ok",
                    lang.text("hill", hill.display()),
                    lang.text("id", hill.hillId()),
                    lang.number("online", scoring.lastOnline()),
                    lang.number("scheduled", hill.lastScheduled()),
                    lang.number("hops", scoring.lastHops()),
                    lang.number("skipped", scoring.lastSkippedUnassigned()),
                    lang.text("sample_ms", String.format(Locale.ROOT, "%.3f", ms)),
                    lang.text("max_ms", String.format(Locale.ROOT, "%.3f", maxMs)),
                    lang.number("overlaps", scoring.overlapSkips()),
                    lang.text("inflight", scoring.inFlight() ? "yes" : "no"),
                    lang.number("hud", plugin.display().lastHudClients()),
                    lang.text("point", hill.match().pointState().name()),
                    lang.text("sample_mode", scoring.lastMode()));
        }
    }

    private void bossBar(CommandSender sender, String[] args) {
        if (deny(sender, "hill.bossbar")) {
            return;
        }
        Lang lang = plugin.lang();
        boolean toggle = args.length < 2 || args[1].equalsIgnoreCase("toggle");
        Boolean requested = null;
        if (!toggle && args.length >= 2) {
            String token = args[1].toLowerCase(Locale.ROOT);
            if (token.equals("on") || token.equals("true") || token.equals("enable")) {
                requested = true;
            } else if (token.equals("off") || token.equals("false") || token.equals("disable")) {
                requested = false;
            } else {
                lang.send(sender, "error-usage", lang.unparsed("usage", "/hill bossbar [on|off|toggle]"));
                return;
            }
        }
        boolean current = plugin.config() != null && plugin.config().bossBar();
        boolean visible = requested == null ? !current : requested;
        plugin.setBossBarEnabled(visible);
        lang.send(sender, visible ? "bossbar-on" : "bossbar-off");
    }

    private void pad(CommandSender sender, String[] args) {
        if (deny(sender, "hill.pad")) {
            return;
        }
        Lang lang = plugin.lang();
        String usage = "/hill pad <blue|yellow|remove> [x1 y1 z1 x2 y2 z2]";
        if (args.length < 2) {
            lang.send(sender, "error-usage", lang.unparsed("usage", usage));
            return;
        }
        if (args[1].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player player)) {
                lang.send(sender, "error-player-only");
                return;
            }
            if (!plugin.hills().removePadAt(player.getLocation())) {
                lang.send(sender, "pad-remove-none");
                return;
            }
            plugin.persistHills();
            lang.send(sender, "pad-remove-ok");
            return;
        }
        var team = TeamId.parse(args[1], plugin.config() == null ? null : plugin.config().teams());
        if (team.isEmpty()) {
            lang.send(sender, "error-invalid-team");
            return;
        }
        World world;
        double x1;
        double y1;
        double z1;
        double x2;
        double y2;
        double z2;
        if (args.length >= 8) {
            Location origin = originOf(sender);
            boolean relativeOk = sender instanceof Player;
            try {
                x1 = parseCoord(args[2], origin == null ? 0 : origin.getX(), relativeOk);
                y1 = parseCoord(args[3], origin == null ? 0 : origin.getY(), relativeOk);
                z1 = parseCoord(args[4], origin == null ? 0 : origin.getZ(), relativeOk);
                x2 = parseCoord(args[5], origin == null ? 0 : origin.getX(), relativeOk);
                y2 = parseCoord(args[6], origin == null ? 0 : origin.getY(), relativeOk);
                z2 = parseCoord(args[7], origin == null ? 0 : origin.getZ(), relativeOk);
            } catch (IllegalArgumentException ex) {
                if ("relative".equals(ex.getMessage())) {
                    lang.send(sender, "error-relative-console");
                } else {
                    lang.send(sender, "error-usage", lang.unparsed("usage", usage));
                }
                return;
            }
            world = origin != null ? origin.getWorld() : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0));
        } else if (sender instanceof Player player) {
            Location loc = player.getLocation();
            var block = loc.getBlock();
            if (block.isEmpty()) {
                block = loc.clone().subtract(0, 0.2, 0).getBlock();
            }
            world = player.getWorld();
            x1 = x2 = block.getX();
            y1 = y2 = block.getY();
            z1 = z2 = block.getZ();
        } else {
            lang.send(sender, "error-usage", lang.unparsed("usage", usage));
            return;
        }
        if (world == null) {
            lang.send(sender, "error-point-unusable");
            return;
        }
        plugin.hills().addPad(TeamPad.fromInclusiveBlocks(team.get(), world, x1, y1, z1, x2, y2, z2));
        plugin.persistHills();
        lang.send(sender, "pad-ok", lang.component("team", lang.hud(team.get().langKey())));
    }

    private void autodivide(CommandSender sender, String[] args) {
        if (deny(sender, "hill.autodivide")) {
            return;
        }
        Lang lang = plugin.lang();
        if (args.length < 3) {
            lang.send(sender, "error-usage", lang.unparsed("usage", "/hill autodivide <blue%> <yellow%>"));
            return;
        }
        if (!PercentSplit.looksLike(args[1]) || !PercentSplit.looksLike(args[2])) {
            lang.send(sender, "error-usage", lang.unparsed("usage", "/hill autodivide <blue%> <yellow%>"));
            return;
        }
        var ratio = PercentSplit.parse(args[1], args[2]);
        if (ratio.isEmpty()) {
            lang.send(sender, "error-percent-sum");
            return;
        }
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            sendAutodivideResult(sender, ratio.get(), 0, 0, 0);
            return;
        }
        Set<Player> eligible = ConcurrentHashMap.newKeySet();
        Set<Player> skipped = ConcurrentHashMap.newKeySet();
        AtomicInteger remaining = new AtomicInteger(online.size());
        for (Player player : online) {
            player.getScheduler()
                    .run(
                            plugin,
                            scheduled -> {
                                try {
                                    if (Players.excludedByDefault(player, plugin.config())) {
                                        skipped.add(player);
                                    } else {
                                        eligible.add(player);
                                    }
                                } finally {
                                    finishAutodivideIfDone(sender, ratio.get(), eligible, skipped, remaining);
                                }
                            },
                            () -> finishAutodivideIfDone(sender, ratio.get(), eligible, skipped, remaining));
        }
    }

    private void finishAutodivideIfDone(
            CommandSender sender,
            PercentSplit.Ratio ratio,
            Set<Player> eligible,
            Set<Player> skipped,
            AtomicInteger remaining) {
        if (remaining.decrementAndGet() != 0) {
            return;
        }
        Bukkit.getGlobalRegionScheduler()
                .run(plugin, scheduled -> applyAutodivide(sender, ratio, eligible, skipped));
    }

    private void applyAutodivide(
            CommandSender sender, PercentSplit.Ratio ratio, Set<Player> eligible, Set<Player> skipped) {
        for (Player player : skipped) {
            plugin.hills().teams().unassign(player.getUniqueId());
            plugin.scoring().scheduleSync(player);
        }
        List<Player> players = new ArrayList<>(eligible);
        Collections.shuffle(players);
        int[] counts = PercentSplit.counts(players.size(), ratio);
        int blue = 0;
        int yellow = 0;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            TeamId team = i < counts[0] ? TeamId.BLUE : TeamId.YELLOW;
            plugin.hills().teams().assign(player.getUniqueId(), team);
            plugin.scoring().scheduleSync(player);
            if (team == TeamId.BLUE) {
                blue++;
            } else {
                yellow++;
            }
        }
        plugin.persistMatch();
        sendAutodivideResult(sender, ratio, blue, yellow, skipped.size());
    }

    private void sendAutodivideResult(
            CommandSender sender, PercentSplit.Ratio ratio, int blue, int yellow, int skipped) {
        Lang lang = plugin.lang();
        lang.send(
                sender,
                "autodivide-ok",
                lang.number("blue_count", blue),
                lang.number("yellow_count", yellow),
                lang.number("total", blue + yellow),
                lang.text("blue_pct", formatPercent(ratio.bluePercent())),
                lang.text("yellow_pct", formatPercent(ratio.yellowPercent())));
        if (skipped > 0) {
            lang.send(sender, "autodivide-skipped", lang.number("skipped", skipped));
        }
    }

    private void score(CommandSender sender, String[] args) {
        if (deny(sender, "hill.score")) {
            return;
        }
        Lang lang = plugin.lang();
        String usage = "/hill score <add|remove> <amount> <blue|yellow>";
        if (args.length < 4) {
            lang.send(sender, "error-usage", lang.unparsed("usage", usage));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (!action.equals("add") && !action.equals("remove")) {
            lang.send(sender, "error-score-action");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            lang.send(sender, "error-invalid-amount");
            return;
        }
        if (amount < 0) {
            lang.send(sender, "error-invalid-amount");
            return;
        }
        var team = TeamId.parse(args[3], plugin.config().teams());
        if (team.isEmpty()) {
            lang.send(sender, "error-invalid-team");
            return;
        }
        int delta = action.equals("add") ? amount : -amount;
        plugin.hills().teams().addScore(team.get(), delta);
        plugin.persistMatch();
        lang.send(
                sender,
                "score-ok",
                lang.text("action", action.equals("add") ? "Added" : "Removed"),
                lang.number("amount", amount),
                lang.component("team", lang.hud(team.get().langKey())),
                lang.number("score", plugin.hills().teams().score(team.get())));
    }

    private List<HillInstance> resolveHills(CommandSender sender, String[] args, int index, String usage) {
        Lang lang = plugin.lang();
        if (args.length <= index) {
            lang.send(sender, "error-usage", lang.unparsed("usage", usage));
            return null;
        }
        if (args[index].equalsIgnoreCase("all")) {
            List<HillInstance> all = new ArrayList<>(plugin.hills().all());
            if (all.isEmpty()) {
                lang.send(sender, "error-no-hills");
                return null;
            }
            return all;
        }
        HillInstance hill = plugin.hills().byId(args[index]);
        if (hill == null) {
            lang.send(sender, "error-unknown-hill", lang.text("id", args[index]));
            return null;
        }
        return List.of(hill);
    }

    private World worldOf(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.hills().rememberPlayer(player);
            return player.getWorld();
        }
        if (Bukkit.getWorlds().isEmpty()) {
            return null;
        }
        return Bukkit.getWorlds().get(0);
    }

    private Location originOf(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getLocation();
        }
        World world = worldOf(sender);
        return world == null ? null : new Location(world, 0, 64, 0);
    }

    private static UUID actorId(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId();
        }
        return new UUID(0L, sender instanceof ConsoleCommandSender ? 1L : 2L);
    }

    private static double parseCoord(String token, double origin, boolean relativeOk) {
        if (token.equals("~")) {
            if (!relativeOk) {
                throw new IllegalArgumentException("relative");
            }
            return origin;
        }
        if (token.startsWith("~")) {
            if (!relativeOk) {
                throw new IllegalArgumentException("relative");
            }
            return origin + Double.parseDouble(token.substring(1));
        }
        return Double.parseDouble(token);
    }

    private static boolean looksNumber(String token) {
        if (token == null || token.isBlank() || token.startsWith("~")) {
            return false;
        }
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static double parsePositive(String token) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String formatPercent(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return Integer.toString((int) Math.rint(value)) + "%";
        }
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return subsFor(sender);
        }
        String current = args[args.length - 1];
        if (args.length == 1) {
            return filter(subsFor(sender), current);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return filter(tabOptions(sender, sub, args), current);
    }

    private List<String> tabOptions(CommandSender sender, String sub, String[] args) {
        List<String> ids = plugin.hills().ids();
        List<String> idsAll = new ArrayList<>(ids);
        idsAll.add("all");
        return switch (sub) {
            case "help" -> args.length == 2 ? helpTopics(sender) : List.of();
            case "mode" -> args.length == 2 ? List.of("ctf", "koth") : List.of();
            case "new" -> {
                if (args.length <= 4) {
                    yield List.of("~");
                }
                if (args.length == 5 || args.length == 6 || args.length == 7) {
                    yield List.of("8", "16", "32", "square", "circle", "cube", "sphere", "cylinder");
                }
                if (args.length == 8) {
                    yield List.of("square", "circle", "cube", "sphere", "cylinder");
                }
                yield List.of();
            }
            case "remove", "status" -> args.length == 2 ? ("remove".equals(sub) ? idsAll : ids) : List.of();
            case "pause", "resume", "perf" -> args.length == 2 ? idsAll : List.of();
            case "assign" -> {
                if (args.length == 2) {
                    List<String> names = new ArrayList<>();
                    names.add("@a");
                    names.add("@p");
                    names.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                    yield names;
                }
                yield args.length == 3 ? teamTokens() : List.of();
            }
            case "unassign" -> {
                if (args.length != 2) {
                    yield List.of();
                }
                List<String> names = new ArrayList<>();
                names.add("all");
                names.add("@a");
                names.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                yield names;
            }
            case "autodivide" -> args.length == 2 || args.length == 3 ? List.of("50%", "70%", "30%", "60%", "40%") : List.of();
            case "bossbar" -> args.length == 2 ? List.of("on", "off", "toggle") : List.of();
            case "pad" -> {
                if (args.length == 2) {
                    List<String> tokens = new ArrayList<>(teamTokens());
                    tokens.add("remove");
                    yield tokens;
                }
                if (args.length >= 3 && args.length <= 8) {
                    yield List.of("~");
                }
                yield List.of();
            }
            case "score" -> {
                if (args.length == 2) {
                    yield List.of("add", "remove");
                }
                if (args.length == 3) {
                    yield List.of("1", "5", "10");
                }
                if (args.length == 4) {
                    yield teamTokens();
                }
                yield List.of();
            }
            default -> List.of();
        };
    }

    private List<String> subsFor(CommandSender sender) {
        List<String> subs = new ArrayList<>();
        for (HillHelp.Topic topic : HillHelp.all()) {
            addIfPermitted(sender, subs, topic.name(), topic.permission());
        }
        return subs;
    }

    private List<String> helpTopics(CommandSender sender) {
        return subsFor(sender);
    }

    private static void addIfPermitted(CommandSender sender, List<String> subs, String name, String node) {
        if (sender.hasPermission(node)) {
            subs.add(name);
        }
    }

    private List<String> teamTokens() {
        List<String> tokens = new ArrayList<>();
        tokens.add("blue");
        tokens.add("yellow");
        var config = plugin.config();
        if (config == null) {
            return tokens;
        }
        String team1 = config.teams().team1().display();
        String team2 = config.teams().team2().display();
        if (tokens.stream().noneMatch(token -> token.equalsIgnoreCase(team1))) {
            tokens.add(team1);
        }
        if (tokens.stream().noneMatch(token -> token.equalsIgnoreCase(team2))) {
            tokens.add(team2);
        }
        return tokens;
    }

    private List<String> filter(List<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private record PendingMode(UUID actor, HillMode mode, String token, long expiresAt) {}
}
