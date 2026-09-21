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

## Why it is light

Particles cost the connection of everybody nearby, so:

- One task for the server goes through the players who wear a trail, and a trail that is not due costs one comparison.
- A trail is only sent while its wearer moves, and a moment after. Players who stand still send nothing.
- A puff is one packet, however many particles it has.
- No more than `performance.budget-per-tick` puffs are sent in one tick, shared fairly between players.
- With `performance.lag-guard` on, trails send half as often when the server is slow and stop when it is badly lagging.
- Players who hid trails with `/trail visibility` are left out of the packets.
- A player's choice is stored on the player. There is no database and nothing is kept in memory for players without a trail.
- Nothing is downloaded and no libraries are bundled.

## Building

```
./gradlew build
```

The jar is in `build/libs`. Licensed under MIT.
