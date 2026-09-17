package com.ontheverg3.hill.command;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class HillHelp {
    public record Topic(
            String name,
            String permission,
            String summary,
            String usage,
            String notes,
            List<String> examples) {
        public Topic {
            examples = examples == null ? List.of() : List.copyOf(examples);
            notes = notes == null ? "" : notes;
        }
    }

    private static final List<Topic> TOPICS = List.of(
            new Topic(
                    "help",
                    "hill.help",
                    "Command list, or details for one command",
                    "/hill help [command]",
                    "Click a command in the list to open its page. This is the same as typing /hill with no arguments.",
                    List.of("/hill help", "/hill help assign", "/hill help new")),
            new Topic(
                    "reload",
                    "hill.reload",
                    "Reload config, language, and hills.json",
                    "/hill reload",
                    "Does not load a new jar. Restart the server after you replace Hill.jar.",
                    List.of("/hill reload")),
            new Topic(
                    "mode",
                    "hill.mode",
                    "Switch KotH or CTF",
                    "/hill mode <ctf|koth>",
                    "Chat will ask you to confirm. Confirm deletes every hill and resets scores. Team assignments stay. KotH allows one hill per world save and dimension. CTF allows many.",
                    List.of("/hill mode ctf", "/hill mode koth")),
            new Topic(
                    "new",
                    "hill.new",
                    "Create a hill in your world",
                    "/hill new <x|~> <y|~> <z|~> <radius|rx ry rz> <square|circle|cube|sphere|cylinder> <id> [\"display name\"]",
                    "One radius: square and circle are 16 blocks tall. Cube, sphere, and cylinder are as tall as they are wide. Three numbers set width, height, and depth. Relative ~ needs a player. Console uses the first loaded world.",
                    List.of("/hill new ~ ~ ~ 8 circle mid", "/hill new ~ ~ ~ 8 16 8 cube castle \"Castle Hill\"")),
            new Topic(
                    "remove",
                    "hill.remove",
                    "Delete a hill, or every hill",
                    "/hill remove <id|all>",
                    "Scores stay. Team pads stay. Use /hill reset if you also want scores at 0.",
                    List.of("/hill remove mid", "/hill remove all")),
            new Topic(
                    "assign",
                    "hill.assign",
                    "Put players on a team",
                    "/hill assign <player|selector> <blue|yellow>",
                    "Also accepts team1, team2, and the configured display names. A named player always assigns. Selectors skip players excluded by assign: in config unless the selector already mentions gamemode, dead, or alive.",
                    List.of("/hill assign Steve blue", "/hill assign @a[distance=..12] yellow")),
            new Topic(
                    "unassign",
                    "hill.unassign",
                    "Remove players from a team",
                    "/hill unassign <player|selector|all>",
                    "all clears every assignment.",
                    List.of("/hill unassign Steve", "/hill unassign @a", "/hill unassign all")),
            new Topic(
                    "status",
                    "hill.status",
                    "Global scores, that hill's control, and roster",
                    "/hill status <hill id>",
                    "Scores are global. The hill id only picks which control state to show.",
                    List.of("/hill status mid")),
            new Topic(
                    "swapteams",
                    "hill.swapteams",
                    "Swap the two teams' assignments",
                    "/hill swapteams",
                    "Everyone on team 1 becomes team 2 and the other way around. Scores do not move.",
                    List.of("/hill swapteams")),
            new Topic(
                    "swapscore",
                    "hill.swapscore",
                    "Swap the two teams' scores",
                    "/hill swapscore",
                    "Assignments stay. Scores are global.",
                    List.of("/hill swapscore")),
            new Topic(
                    "pause",
                    "hill.pause",
                    "Stop awards on a hill",
                    "/hill pause <hill id|all>",
                    "Presence still updates. Scores are kept. Other hills still score.",
                    List.of("/hill pause mid", "/hill pause all")),
            new Topic(
                    "resume",
                    "hill.resume",
                    "Start awards again on a hill",
                    "/hill resume <hill id|all>",
                    "",
                    List.of("/hill resume mid", "/hill resume all")),
            new Topic(
                    "reset",
                    "hill.reset",
                    "Set both scores to 0",
                    "/hill reset",
                    "Does not unpause hills. Set reset.clear-teams to true in config if you also want assignments cleared.",
                    List.of("/hill reset")),
            new Topic(
                    "perf",
                    "hill.perf",
                    "Occupancy sample timing",
                    "/hill perf <hill id|all>",
                    "For diagnosing hitch. Occupancy itself is a presence index, not getNearbyPlayers.",
                    List.of("/hill perf mid", "/hill perf all")),
            new Topic(
                    "autodivide",
                    "hill.autodivide",
                    "Shuffle-split online players by percent",
                    "/hill autodivide <blue%> <yellow%>",
                    "The two percents must add up to 100. Always uses assign: in config. Excluded players are left out and unassigned. Named selectors are not an override here.",
                    List.of("/hill autodivide 50 50", "/hill autodivide 70% 30%")),
            new Topic(
                    "score",
                    "hill.score",
                    "Add or remove global score",
                    "/hill score <add|remove> <amount> <blue|yellow>",
                    "Team also accepts team1, team2, and the configured display names. Amount is a whole number.",
                    List.of("/hill score add 5 blue", "/hill score remove 1 yellow")),
            new Topic(
                    "bossbar",
                    "hill.bossbar",
                    "Show or hide the score boss bar for everyone",
                    "/hill bossbar [on|off|toggle]",
                    "Writes display.boss-bar. Does not hide the action bar. display.scores still chooses always vs in-zone.",
                    List.of("/hill bossbar", "/hill bossbar off", "/hill bossbar on")),
            new Topic(
                    "pad",
                    "hill.pad",
                    "Create or remove a team assign pad",
                    "/hill pad <blue|yellow|remove> [x1 y1 z1 x2 y2 z2]",
                    "No coordinates uses the block you stand on. Inclusive corners plus 2 blocks of stand height. Standing in a pad assigns you if you have no team or are on the other team. assign: filters apply. Mode switch keeps pads.",
                    List.of("/hill pad blue", "/hill pad yellow ~ ~ ~ ~2 ~ ~2", "/hill pad remove")));

    private HillHelp() {}

    public static List<Topic> all() {
        return TOPICS;
    }

    public static Optional<Topic> byName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String name = raw.trim().toLowerCase(Locale.ROOT);
        for (Topic topic : TOPICS) {
            if (topic.name().equals(name)) {
                return Optional.of(topic);
            }
        }
        return Optional.empty();
    }
}
