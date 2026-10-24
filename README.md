# Better Server List

[![License](https://img.shields.io/github/license/MyCelium-GG/Better-Server-List)](LICENSE)

The multiplayer screen, organized. Keep your servers in several lists, group
them into collapsible categories, pin favorites to the top, and find any
server by typing. Drag and drop to sort, right-click for everything else.
Your existing servers are imported on first start and the vanilla
`servers.dat` stays in sync, so nothing is lost if you remove the mod.
Client side.

Built on [Mycel](https://github.com/MyCelium-GG/MyCel-Lib) (`my.celium.org`), the shared
core for the MyCelium ecosystem: configuration, platform abstraction and
diagnostics are reused.

| | |
|---|---|
| Mod id | `betterserverlist` |
| Minecraft | **26.3** · Java **25** |
| Loaders | Fabric, Forge, NeoForge |
| Side | Client only |
| Docs | [wiki.mycelium-gg.org/betterserverlist](https://wiki.mycelium-gg.org/betterserverlist/) |

![The Better Server List screen](https://wiki.mycelium-gg.org/betterserverlist/img/overview.png)

## Behavior

- **Replaces the multiplayer screen.** Clicking *Multiplayer* opens Better
  Server List instead of the vanilla list. *Vanilla* in the footer opens the
  vanilla screen until you return to the title screen; a *Better list*
  button in its corner brings you back.
- **Lists** (left sidebar): as many as you like, e.g. *Favorites*,
  *Modded*, *Events*. Click to switch, double-click to rename, right-click
  to rename, reorder, copy as text, paste servers or delete.
- **Categories**: collapsible sections inside a list, each with a color.
  Create them with *+ Category* or from a server's menu. Rename, recolor,
  reorder and delete from the header's right-click menu; deleting a category
  keeps its servers (they become uncategorized).
- **Pinned**: pin a server (star on hover, or the menu) and it moves to a
  *Pinned* section at the top of its list.
- **Search**: just start typing. Matches the name, address, MOTD, note and
  category. Sections open while searching and empty ones hide.
- **Sort**: Manual (your order), Name, Players, Ping or Last played, inside
  each section. **Online only** hides servers that did not answer.
- **Drag and drop** (Manual sort): drop a server between two others, on a
  section header to move it into that section, or on a list in the sidebar
  to move it there (hold Ctrl to copy).
- **Notes and history**: give any server a note. Hovering a server's name
  shows its address, category, note, when you last played and how often.
- **Rows look like vanilla's**: server icon, MOTD, player count with the
  player list on hover, ping bars. Servers that can't be reached show the
  red cross (vanilla keeps animating forever).
- **LAN worlds** appear in their own section at the bottom.
- **Copy and paste lists as text**: one server per line
  (`Name<TAB>address`). Pasting also takes `Name address` or bare addresses,
  so a list shared in chat or Discord imports in one click.

### Keys

| Key | Action |
|---|---|
| Typing, Ctrl+F | Search |
| ↑ / ↓ | Select the previous / next server |
| Shift + ↑ / ↓ | Move the selected server within its section |
| Enter | Join (from the search box: the first match) |
| Delete | Delete the selected server |
| F2 | Edit the selected server |
| Ctrl+C | Copy the selected server's address |
| F5 | Ping every server again |
| Esc | Clear the search, close a menu, or go back |

### Where your servers live

- `config/betterserverlist-servers.json`: lists, categories, pins, notes,
  play history, sort and filter. Written atomically; a file that no longer
  parses is kept as `.broken` and the lists are rebuilt from `servers.dat`.
- `servers.dat` (vanilla): kept in step with every change. It lists each
  address once, in list order, and still stores what the game remembers per
  server (icon, resource pack choice, accepted code of conduct). Vanilla's
  hidden direct-connect history is left alone.
- On every open, servers that appear in `servers.dat` but in none of your
  lists (added with the vanilla screen, or shipped by a modpack) are added
  to the first list.

## Configuration (`config/betterserverlist.json`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch (also toggleable live via `Mycel.setEnabled`). |
| `replaceScreen` | `true` | Open Better Server List instead of the vanilla multiplayer screen. |
| `showLan` | `true` | Look for LAN worlds and list them at the bottom. |
| `confirmDelete` | `true` | Ask before deleting a server, category or list. |
| `debug` | `false` | Log loading, saving, syncing and screen swaps. |

Reload live with `/mycel reload betterserverlist`. A config screen is
available from the mod list on Forge and NeoForge.

## Building

JDK 25, then:

```sh
./gradlew build            # all loaders + tests
./gradlew :common:test     # unit tests only
./gradlew :fabric:runClient
```

The list logic (sections, search, sorting, drag and drop, import and
export, the JSON store) lives in `common/.../data` without Minecraft
classes and is covered by unit tests.

Mycel itself comes from [JitPack](https://jitpack.io/#MyCelium-GG/MyCel-Lib)
as one universal jar for all loaders
(`com.github.MyCelium-GG:MyCel-Lib:1.3.1`). No local setup needed.

## Releasing

Push a version tag (`git tag 1.0.0 && git push origin 1.0.0`). The
`release` workflow builds the three loader jars, attaches them to a GitHub
Release and uploads them to CurseForge once `curseforge_project_id` is set in
`gradle.properties` (token in the `CURSEFORGE_TOKEN` secret).

## License

MIT, see [LICENSE](LICENSE).
