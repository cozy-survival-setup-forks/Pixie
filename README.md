# Pixie

Light, colourful particle trails for Paper 1.21+. Every trail is written in one file, `trails.yml`, and there is no
built-in menu: build one with DeluxeMenus (or any menu plugin) using the commands and placeholders below.

34 trails come with the plugin, from soft colour mixes (Yellow Pink, Sunset, Aurora, Cotton Candy) and gems to fire,
frost, galaxy, cherry blossom and shapes such as a halo, a rainbow ring and a twin helix.

## Files

| File | What it holds |
| --- | --- |
| `trails.yml` | The trails, and nothing else |
| `config.yml` | Permissions, disabled worlds, limits that keep the server light |
| `lang.yml` | Every message |

## Writing a trail

```yaml
trails:
  yellow_pink:
    display: "Yellow Pink"
    interval: 2
    layers:
      - particle: DUST
        colors: ["#FFC0CB", "#FFF200", "#FFFFFF"]   # one after the other on every puff
        size: 1.1
        count: 4
        spread: 0.22
        height: 0.12
      - particle: END_ROD
        count: 1
        spread: 0.15
```

A trail has an `interval` (ticks between two puffs) and layers. Each layer is one kind of particle. The top of
`trails.yml` explains every setting. In short:

- `shape`: `SCATTER` a cloud around the feet (default), `RING`, `HELIX` or `ORBIT`
- `DUST` takes `colors`, and `DUST_COLOR_TRANSITION` takes `colors` and `to` to fade from one to the other
- `BLOCK` and `FALLING_DUST` take `block`, `ITEM` takes `item`, `NOTE` takes `notes`
- `moving: false` shows a trail even when the player stands still, which suits a halo or an orbit

## Commands

| Command | Use |
| --- | --- |
| `/trail <name>` | Pick a trail |
| `/trail off` | Remove it |
| `/trail toggle` | Pause or bring back your trail without forgetting which one it is |
| `/trail visibility` | Stop seeing all trails, yours too, or see them again. It also saves that player's connection |
| `/trail list` | The trails you can use |
| `/trail set <player> <name\|off>` | Admin: give or remove a player's trail |
| `/trail reload` | Admin: reload the files |

`/trails` works the same as `/trail`.

## Permissions

- `pixie.trail.<id>`: use one trail (or set your own with `permission:`)
- `pixie.trail.*`: use all of them (op by default)
- `pixie.use`: use `/trail` (everybody)
- `pixie.admin`: reload, set

Set `use-permissions: false` in `config.yml` to let everybody use every trail.

## Placeholders (PlaceholderAPI)

`%pixie_id%`, `%pixie_display%`, `%pixie_paused%`, `%pixie_hidden%`, and for menus `%pixie_equipped_<id>%`
and `%pixie_owned_<id>%` (`true` or `false`).

Particles cost the connection of everybody nearby a wearer, so trails only send while the wearer moves (unless a trail
says `moving: false`), and a scatter layer is one packet whatever its particle count. A rich trail costs more: a ring
sends one packet per point and a helix two, a trail may have up to 8 layers. `performance.budget-per-tick` caps how many
packets go out server-wide each tick, and `lag-guard` slows or pauses trails while the server is struggling. Trails only
reach the wearer and the players who can see them. See `config.yml` for the knobs.

## Building

```
./gradlew build
```

The jar is in `build/libs`.

## Keeping your files safe

- `config.yml` and `lang.yml` start with a `config-version` / `lang-version` number. After an update, new settings are added to your files with their comments, and nothing you changed is touched. The old file is kept next to it as `<name>.<date>.bak` (the newest 5). A setting is only removed when the changelog says so.
- A value with a mistake (a negative time, an item that does not exist, text where a number belongs) is named in the console by file and key. On a reload, the settings in use stay as they were.
- Files are written to a temporary file and moved into place, with the previous version kept as `.bak`. A file that cannot be read is restored from its `.bak`, and the unreadable one is kept as `.broken-<time>`.
- A file or database that was made by a newer version of the plugin is left alone and a warning is logged.
- `/trail doctor` shows the health of the files, versions, last backup and recent save failures (no player data). `/trail backup now` makes a checked backup right away. Both need the admin permission.

## Telemetry

On startup Pixie sends a small anonymous beacon (plugin name/version, server software/version,
online/max player counts, and a random ID with no player data) so we know which versions are in
use. Turn it off with `metrics.enabled: false` in `config.yml`. The random ID is kept as `server-id` in `data.yml` (older versions kept it in a `.server-id` file, which is moved over unchanged). The address and the interval are fixed in the plugin and are not settings.

## License

See `LICENSE`: free to run on your own servers, not for redistribution or resale.
