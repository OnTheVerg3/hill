# Hill

Folia King of the Hill. Two teams. A team only scores if they have a hill to themselves.

Needs Folia 1.21.11. `folia-supported` is already set.

## Setup

1. Drop `Hill.jar` in `plugins/`
2. Start once so it writes `plugins/Hill/config.yml` and `plugins/Hill/lang/en.yml`
3. Create a hill in-game with `/hill new`

Hills are stored in `plugins/Hill/hills.json` (position, size, shape, world, dimension). Teams and scores are stored in `plugins/Hill/data.yml`. Hill ids are global. World save and dimension are stored with each hill; they are not part of the id.

Default mode is **KotH**: at most one hill per world save and dimension (overworld, nether, and the end can each have one). **CTF** allows more than one hill. Switching modes deletes every hill and resets every score after a clickable confirm in chat. Team assignments are kept.

The 2D footprint of each hill is outlined with gold dust. The ring follows the true circle or rectangle and sits on top of the terrain, including uneven ground. A hill in the air, or over empty space below its volume, is drawn as a flat ring at the hill's height.

Players see scores on the action bar and on a split boss bar. The boss bar is one bar with two colors: team 1 fills from the left, team 2 fills from the right. How full each side is depends on that team's score. Vanilla boss bars are a single color, so the fill is drawn in the title. Players can hide it for themselves with `/hill bossbar`.

## Scoring

Every `scoring.interval-seconds` (default 5), if exactly one team has eligible players in that hill, they get `scoring.points` (default 1). Contested or empty: nothing. Each hill has its own score. Teams are global. Display names and colors are `team1-displayname`, `team1-color`, `team2-displayname`, and `team2-color`. Commands also accept `blue`, `yellow`, `team1`, `team2`, and those display names. Scores in `data.yml` still use `blue` and `yellow`.

Unassigned players standing in a hill do not count for either team. Dead players and spectators do not count toward scoring by default. Creative players can still score unless you turn that off under `eligibility`. Pause keeps the scores.

`scoring.win-score` (default 0) is an optional cap. When it is set, each side of the split boss bar fills toward that number. When it is 0, each side fills toward the leading score.

`/hill assign` and `/hill autodivide` skip dead, spectator, and creative players by default. Those flags live under `assign:` in `config.yml`. Named players always assign. Selectors still skip those players unless the selector already filters by gamemode or dead/alive.

## Commands

| Command | What it does |
|---|---|
| `/hill mode <ctf\|koth>` | Switch modes. Warns in chat with clickable Confirm / Cancel. Clears all hills and scores. KotH is the default. |
| `/hill new <x\|~> <y\|~> <z\|~> <radius\|rx ry rz> <square\|circle\|cube\|sphere\|cylinder> <id> ["display name"]` | Create a hill at those coordinates in your world. One radius is a circle/square footprint (height 16) or a cube/sphere/cylinder of that size. Three radii set x, y, and z independently. |
| `/hill remove <id\|all>` | Delete a hill, or every hill |
| `/hill reload` | Reload config, language, and `hills.json`. Does not load a new jar |
| `/hill help` | Command list |
| `/hill assign <player\|selector> <blue\|yellow>` | Assign to a team. You can also use `team1`, `team2`, or the configured display names. Named players always assign. Selectors skip players excluded by `assign:` in config unless the selector already mentions gamemode or dead |
| `/hill unassign <player\|selector\|all>` | Remove from a team |
| `/hill status <hill id>` | Scores, control, roster |
| `/hill swapteams` | Swap team 1 and team 2 assignments (global) |
| `/hill swapscore <hill id>` | Swap team 1 and team 2 scores on that hill |
| `/hill pause <hill id\|all>` | Freeze awards |
| `/hill resume <hill id\|all>` | Resume awards |
| `/hill reset <hill id\|all>` | Scores to 0 |
| `/hill perf <hill id\|all>` | Occupancy sample timing |
| `/hill autodivide <blue%> <yellow%>` | Shuffle-split players who pass `assign:` in config. Excluded players are not assigned and are removed from a team if they had one |
| `/hill score <add\|remove> <amount> <blue\|yellow>` | Change a team's score. Uses the hill you are standing in, or the only hill if there is just one. You can also pass a hill id or `all` |
| `/hill bossbar [on\|off\|toggle]` | Show or hide the split score boss bar for yourself. Does not hide the action bar |

Aliases: `/koth`, `/hillkoth`. Relative `~` coordinates need a player.

## Permissions

These are normal Bukkit permission nodes. LuckPerms and vanilla operators both work. There is no separate Hill permission plugin. `default: op` is vanilla permission level 4. `hill.admin` includes every node below.

| Node | Default | What it allows |
|---|---|---|
| `hill.status` | true | `/hill status` |
| `hill.help` | op | `/hill help` |
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
| `hill.bossbar` | true | `/hill bossbar` |
| `hill.admin` | op | All of the above |

## Config

`config.yml` holds scoring, eligibility, assign filters, team names and colors, HUD, outline, and locale. Hill geometry is not in this file.

| Key | Default | What it does |
|---|---|---|
| `locale` | en | Language file under `plugins/Hill/lang/` |
| `scoring.interval-seconds` | 5 | Seconds between occupancy awards |
| `scoring.points` | 1 | Points given when one team holds a hill |
| `scoring.win-score` | 0 | Optional score cap. Also scales the split boss bar. 0 means no cap |
| `team1-displayname` | Blue | Visible name for the first team |
| `team1-color` | blue | Chat/HUD color. Minecraft name (`blue`, `gold`, `dark_aqua`, `light_purple`, ...) or hex (`#RGB`, `#RRGGBB`, `RRGGBB`). Names are read before hex, so `red` is vanilla red. Aliases include `grey`, `cyan`, `pink`, `orange`, `magenta`, `purple` |
| `team2-displayname` | Yellow | Visible name for the second team |
| `team2-color` | yellow | Same color rules as team 1 |
| `display.action-bar` | true | Action bar HUD |
| `display.boss-bar` | true | Split score boss bar. Left is team 1, right is team 2. Each side fills from its end based on score |
| `display.boss-bar-width` | 24 | Segments in that bar (even, 8 to 64) |
| `display.update-ticks` | 20 | How often the HUD refreshes |
| `display.skip-without-address` | true | Skip HUD for connections with no client address (some NPCs) |
| `display.ignore-name-prefix` | empty | Skip HUD when the player name starts with this |
| `outline.enabled` | true | Gold dust footprint around each hill |
| `outline.interval-ticks` | 10 | How often the outline is redrawn |
| `outline.points` | 72 | Samples around the footprint (16 to 256) |
| `eligibility.exclude-dead` | true | Dead players do not score |
| `eligibility.exclude-spectator` | true | Spectators do not score |
| `eligibility.exclude-creative` | false | Creative players do not score when true |
| `eligibility.exclude-gamemodes` | SPECTATOR | Extra gamemodes that cannot score |
| `assign.exclude-dead` | true | `/hill assign` and `/hill autodivide` skip dead players |
| `assign.exclude-spectator` | true | Skip spectators |
| `assign.exclude-creative` | true | Skip creative |
| `assign.exclude-gamemodes` | SPECTATOR, CREATIVE | Extra gamemodes those commands skip |
| `reset.clear-teams` | false | `/hill reset` also clears team assignments when true |
| `reload-resets` | false | `/hill reload` also resets scores when true |

Bad numbers keep the last good config on reload. If a hill's world is not loaded, that hill is unusable until it loads. Other hills still score.

Lang: copy `plugins/Hill/lang/<code>.yml` and set `locale:` to the file name. Missing keys fall back to English.

## Placeholders

Bukkit scoreboards are not used. They are broken on Folia. Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) and put these tokens in TAB, FeatherBoard, DecentHolograms, or any other scoreboard/hologram plugin that reads PlaceholderAPI. Hill is a soft depend. It still loads if PlaceholderAPI is missing.

Hill ids can contain underscores. Put the id at the end of the token.

| Placeholder | Value |
|---|---|
| `%hill_mode%` | `koth` or `ctf` |
| `%hill_count%` | number of hills |
| `%hill_ids%` | comma-separated hill ids |
| `%hill_team%` | viewer's team slot: `blue`, `yellow`, or empty |
| `%hill_team_blue%` / `%hill_team_yellow%` | configured display names |
| `%hill_team_color_blue%` / `%hill_team_color_yellow%` | configured colors as `#rrggbb` |
| `%hill_score_blue_<id>%` | Blue score on that hill |
| `%hill_score_yellow_<id>%` | Yellow score on that hill |
| `%hill_state_<id>%` | `empty`, `contested`, `blue`, `yellow`, or `unusable` |
| `%hill_paused_<id>%` | `true` or `false` |
| `%hill_display_<id>%` | display name |
| `%hill_shape_<id>%` | `square`, `circle`, `cube`, `sphere`, or `cylinder` |
| `%hill_world_<id>%` | Bukkit world name |
| `%hill_dimension_<id>%` | dimension token |
| `%hill_save_<id>%` | world save name |
| `%hill_here_id%` | hill the viewer currently occupies (assigned and eligible only) |
| `%hill_here_score_blue%` / `%hill_here_score_yellow%` | scores for that hill |
| `%hill_here_state%` | control state for that hill |
| `%hill_here_paused%` | pause flag for that hill |
| `%hill_here_display%` | display name for that hill |

Unknown ids resolve to an empty string. Unassigned players do not occupy a hill, so `%hill_here_*%` is empty for them.

Example: `%hill_score_blue_mid%` and `%hill_state_castle_hill%`.

## Build

```
mvn -q package
```

Jar: `target/Hill.jar`

CI on `main` uploads the same artifact from GitHub Actions.

## License

All Rights Reserved. See `LICENSE`.
