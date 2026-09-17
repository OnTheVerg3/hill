# Hill

Folia King of the Hill. Two teams. A team only scores if they have a hill to themselves.

Needs Folia 1.21.11. `folia-supported` is already set.

## Setup

1. Drop `Hill.jar` in `plugins/`
2. Start once so it writes `plugins/Hill/config.yml` and `plugins/Hill/lang/en.yml`
3. Create a hill in-game with `/hill new`

Hills are stored in `plugins/Hill/hills.json` (position, size, shape, world, dimension, team pads). Teams and **global** scores are stored in `plugins/Hill/data.yml` (`scores.blue` / `scores.yellow`). Pause is per hill. Hill ids are global. World save and dimension are stored with each hill.

Default mode is **KotH**: at most one hill per world save and dimension (overworld, nether, and the end can each have one). **CTF** allows more than one hill. Switching modes deletes every hill and resets every score after a clickable confirm in chat. Team assignments are kept.

The 2D footprint of each hill is outlined with gold dust particles. The ring follows the true circle or rectangle and sits on top of the terrain, including uneven ground. A hill in the air, or over empty space below its volume, is drawn flat at the hill's height.

Players see scores on the action bar and on a split boss bar. The boss bar title is two colors by score ratio. The vanilla bar under it fills with the leader's share of the combined score, in that team's color. Vanilla cannot hide that bar. Admins turn the boss bar on or off for everyone with `display.boss-bar` or `/hill bossbar`. `display.scores` chooses whether this HUD is always on, or only while standing in a hill.

Stand on a team pad to join that team if you have no team, or to switch if you are on the other team. Standing on your own pad does nothing.

## Shapes

`/hill new` takes one radius or `rx ry rz`. Occupancy is the 3D volume. The gold outline is always the 2D footprint on the ground (or a flat ring in the air).

| Shape | Who is inside | Outline | One radius |
|---|---|---|---|
| `square` | Axis-aligned box on X and Z. Height defaults to 16. | Rectangle | Width and depth are the radius. Height 16 |
| `circle` | Ellipse on X and Z. Height defaults to 16. | Ellipse | Same as square, but round |
| `cube` | Axis-aligned box in X, Y, and Z | Rectangle on the ground | Height equals the radius |
| `sphere` | Ellipsoid | Ellipse on the ground | Height equals the radius |
| `cylinder` | Ellipse on X and Z, height equals the radius | Ellipse | Height equals the radius |

Three numbers set width, height, and depth separately (`rx ry rz`). Flat `square` / `circle` hills are tall so jumping or standing on a mound still counts. `cube`, `sphere`, and `cylinder` are as tall as they are wide unless you pass three radii.

## Scoring

Every `scoring.interval-seconds` (default 5), each hill is checked on its own. If exactly one team has eligible players in that hill, they get `scoring.points` (default 1) on the **global** score. Contested or empty: nothing for that hill. KotH and CTF share one Blue/Yellow total. CTF is several hills at once; the team that holds more hills gains points faster. Pause is still per hill. Display names and colors are `team1-displayname`, `team1-color`, `team2-displayname`, and `team2-color`. Commands also accept `blue`, `yellow`, `team1`, `team2`, and those display names. Scores in `data.yml` still use `blue` and `yellow`.

Unassigned players standing in a hill do not count for either team. Dead players do not count toward scoring by default. Spectator and creative are skipped through `eligibility.exclude-gamemodes` (default: spectator only, so creative can still score). Pause keeps the scores.

`scoring.win-score` (default 0) is an optional score cap. The split boss bar is always the two scores as a ratio of each other. It does not use `win-score`.

`/hill assign` and `/hill autodivide` skip players who fail `assign:` in `config.yml` (dead, plus `exclude-gamemodes`; default spectator and creative). Named players always assign. Selectors still skip those players unless the selector already filters by gamemode or dead/alive.

## Commands

`/hill` with no arguments is the same as `/hill help`. After `/hill` and a space, the client lists every subcommand you can use, the same way vanilla commands do. `/hill help assign` (any subcommand) opens a details page with usage, examples, notes, and the permission node. Command names, usages, and examples in help are clickable.

| Command | What it does |
|---|---|
| `/hill mode <ctf\|koth>` | Switch modes. Warns in chat with clickable Confirm / Cancel. Clears all hills and scores. KotH is the default. |
| `/hill new <x\|~> <y\|~> <z\|~> <radius\|rx ry rz> <square\|circle\|cube\|sphere\|cylinder> <id> ["display name"]` | Create a hill at those coordinates in your world. One radius is a circle/square footprint (height 16) or a cube/sphere/cylinder of that size. Three radii set x, y, and z independently. |
| `/hill remove <id\|all>` | Delete a hill, or every hill |
| `/hill reload` | Reload config, language, and `hills.json`. Does not load a new jar |
| `/hill help [command]` | Command list, or a details page for one command. Names are clickable |
| `/hill assign <player\|selector> <blue\|yellow>` | Assign to a team. You can also use `team1`, `team2`, or the configured display names. Named players always assign. Selectors skip players excluded by `assign:` in config unless the selector already mentions gamemode or dead |
| `/hill unassign <player\|selector\|all>` | Remove from a team |
| `/hill status <hill id>` | Global scores, that hill's control, roster |
| `/hill swapteams` | Swap team 1 and team 2 assignments (global) |
| `/hill swapscore` | Swap team 1 and team 2 scores (global) |
| `/hill pause <hill id\|all>` | Freeze awards on that hill |
| `/hill resume <hill id\|all>` | Resume awards on that hill |
| `/hill reset` | Global scores to 0 |
| `/hill perf <hill id\|all>` | Occupancy sample timing |
| `/hill autodivide <blue%> <yellow%>` | Shuffle-split players who pass `assign:` in config. Excluded players are not assigned and are removed from a team if they had one |
| `/hill score <add\|remove> <amount> <blue\|yellow>` | Change a team's global score |
| `/hill bossbar [on\|off\|toggle]` | Show or hide the split score boss bar for everyone. Writes `display.boss-bar`. Does not hide the action bar |
| `/hill pad <blue\|yellow\|remove> [x1 y1 z1 x2 y2 z2]` | Create a team pad. No coords uses the block you are standing on. Standing in a pad assigns you if you have no team or are on the other team. `remove` deletes the pad you are in |

Aliases: `/koth`. Relative `~` coordinates need a player.

## Permissions

These are normal Bukkit permission nodes. LuckPerms and vanilla operators both work. `default: op` is vanilla permission level 4. `hill.admin` includes every node below.

| Node | Default | What it allows |
|---|---|---|
| `hill.status` | true | `/hill status` |
| `hill.help` | op | `/hill`, `/hill help`, and `/hill help <command>` |
| `hill.reload` | op | `/hill reload` |
| `hill.mode` | op | `/hill mode` |
| `hill.new` | op | `/hill new` |
| `hill.remove` | op | `/hill remove` |
| `hill.assign` | op | `/hill assign` |
| `hill.unassign` | op | `/hill unassign` |
| `hill.swapteams` | op | `/hill swapteams` |
| `hill.swapscore` | op | `/hill swapscore` |
| `hill.pause` | op | `/hill pause` |
| `hill.resume` | op | `/hill resume` |
| `hill.reset` | op | `/hill reset` |
| `hill.perf` | op | `/hill perf` |
| `hill.autodivide` | op | `/hill autodivide` |
| `hill.score` | op | `/hill score` |
| `hill.bossbar` | op | `/hill bossbar` |
| `hill.pad` | op | `/hill pad` |
| `hill.admin` | op | All of the above |

## Config

`config.yml` holds scoring, eligibility, assign filters, team names and colors, HUD, outline, and locale. Hill geometry is not in this file.

| Key | Default | What it does |
|---|---|---|
| `locale` | en | Language file under `plugins/Hill/lang/` |
| `scoring.interval-seconds` | 5 | Seconds between occupancy awards |
| `scoring.points` | 1 | Points given when one team holds a hill |
| `scoring.win-score` | 0 | Optional score cap. 0 means no cap. Does not change the boss bar |
| `team1-displayname` | Blue | Visible name for the first team |
| `team1-color` | blue | Chat/HUD color. Minecraft name (`blue`, `gold`, `dark_aqua`, `light_purple`, ...) or hex (`#RGB`, `#RRGGBB`, `RRGGBB`). Names are read before hex, so `red` is vanilla red. Aliases include `grey`, `cyan`, `pink`, `orange`, `magenta`, `purple` |
| `team2-displayname` | Yellow | Visible name for the second team |
| `team2-color` | yellow | Same color rules as team 1 |
| `display.action-bar` | true | Action bar HUD |
| `display.boss-bar` | true | Split score boss bar for everyone. Title is the two-color ratio. The vanilla track under it is the leader's share. `/hill bossbar` writes this |
| `display.boss-bar-width` | 24 | Segments in the title fill (even, 8 to 64) |
| `display.scores` | in-zone | `always` shows the score HUD everywhere. `in-zone` only while standing in a hill |
| `display.update-ticks` | 20 | How often the HUD refreshes |
| `display.skip-without-address` | true | Skip HUD for connections with no client address (some NPCs) |
| `display.ignore-name-prefix` | empty | Skip HUD when the player name starts with this |
| `outline.enabled` | true | Gold dust footprint around each hill |
| `outline.interval-ticks` | 10 | How often the outline is redrawn |
| `outline.points` | 72 | Samples around the footprint (16 to 256) |
| `eligibility.exclude-dead` | true | Dead players do not score |
| `eligibility.exclude-gamemodes` | SPECTATOR | Gamemodes that cannot score |
| `assign.exclude-dead` | true | `/hill assign` and `/hill autodivide` skip dead players |
| `assign.exclude-gamemodes` | SPECTATOR, CREATIVE | Gamemodes those commands skip |
| `reset.clear-teams` | false | `/hill reset` also clears team assignments when true |
| `reload-resets` | false | `/hill reload` also resets scores when true |

Bad numbers keep the last good config on reload. If a hill's world is not loaded, that hill is unusable until it loads. Other hills still score.

Lang: copy `plugins/Hill/lang/<code>.yml` and set `locale:` to the file name. Missing keys fall back to English.

## Placeholders

Bukkit scoreboards are not used as they are broken on Folia. Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) and put these tokens in TAB, FeatherBoard, DecentHolograms, or any other scoreboard/hologram plugin that reads PlaceholderAPI. The plugin still functions normally without PlaceholderAPI.

Hill ids can contain underscores. Put the id at the end of the token.

| Placeholder | Value |
|---|---|
| `%hill_mode%` | `koth` or `ctf` |
| `%hill_count%` | number of hills |
| `%hill_ids%` | comma-separated hill ids |
| `%hill_team%` | viewer's team slot: `blue`, `yellow`, or empty |
| `%hill_team_blue%` / `%hill_team_yellow%` | configured display names |
| `%hill_team_color_blue%` / `%hill_team_color_yellow%` | configured colors as `#rrggbb` |
| `%hill_score_blue%` / `%hill_score_yellow%` | Global team scores |
| `%hill_score_blue_<id>%` / `%hill_score_yellow_<id>%` | Same global scores (id must exist) |
| `%hill_state_<id>%` | `empty`, `contested`, `blue`, `yellow`, or `unusable` |
| `%hill_paused_<id>%` | `true` or `false` |
| `%hill_display_<id>%` | display name |
| `%hill_shape_<id>%` | `square`, `circle`, `cube`, `sphere`, or `cylinder` |
| `%hill_world_<id>%` | Bukkit world name |
| `%hill_dimension_<id>%` | dimension token |
| `%hill_save_<id>%` | world save name |
| `%hill_here_id%` | hill the viewer currently occupies (assigned and eligible only) |
| `%hill_here_score_blue%` / `%hill_here_score_yellow%` | global scores if the viewer occupies a hill |
| `%hill_here_state%` | control state for that hill |
| `%hill_here_paused%` | pause flag for that hill |
| `%hill_here_display%` | display name for that hill |

Unknown ids resolve to an empty string. Unassigned players do not occupy a hill, so `%hill_here_*%` is empty for them.

Example: `%hill_score_blue%` and `%hill_state_castle_hill%`.

## Build

```
mvn -q package
```

Jar: `target/Hill.jar`

CI on `main` uploads the same artifact from GitHub Actions.

## License

All Rights Reserved. See `LICENSE`.
