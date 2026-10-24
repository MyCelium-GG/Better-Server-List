# Changelog

All notable changes to Better Server List are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Better Server List is versioned semantically (`MAJOR.MINOR.PATCH`).
It runs on Minecraft 26.3 (Fabric + Forge + NeoForge, Java 25) and requires
[Mycel](https://github.com/MyCelium-GG/MyCel-Lib) (`my.celium.org`).

## [1.0.0] - 2026-10-24

### Added

- Initial release.
- Replaces the multiplayer screen; *Vanilla* opens the vanilla list until
  the title screen, and a *Better list* button there returns.
- Several server lists in a sidebar: create, rename, reorder, delete.
- Collapsible, colored categories inside each list; a *Pinned* section at
  the top.
- Type-anywhere search over name, address, MOTD, note and category.
- Sort by manual order, name, players, ping or last played; *Online only*
  filter.
- Drag and drop: reorder, move into a section, or onto another list
  (Ctrl copies).
- Right-click menus for servers, sections, lists and empty space.
- Per-server notes; name tooltip with address, category, note and play
  history.
- Vanilla-style rows (icon, MOTD, players, ping); unreachable servers show
  the red cross instead of pinging forever.
- LAN worlds section.
- Copy a list as text and paste servers from the clipboard.
- Keyboard: arrows, Shift+arrows, Enter, Delete, F2, F5, Ctrl+C, Ctrl+F.
- Imports `servers.dat` on first start and keeps it in sync afterwards.
- Config (`config/betterserverlist.json`): `enabled`, `replaceScreen`,
  `showLan`, `confirmDelete`, `debug`; live reload via
  `/mycel reload betterserverlist`; config screen from the mod list on Forge
  and NeoForge.
- Client-side only. Built on Mycel 1.3.1
  (`com.github.MyCelium-GG:MyCel-Lib:1.3.1` via JitPack).
