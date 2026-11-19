# Noteblock GUI

A small Fabric mod for Minecraft Java Edition **26.3** that lets you pick a note block's note **and instrument** from a simple one-page GUI instead of clicking through 25 notes.

## Usage

**Shift + right-click** a note block with an **empty main hand**.

- Left side: all 25 notes (F#3 – F#5). Right side: the 20 selectable instruments.
- Clicking a note or instrument selects it (shown in bold yellow) and plays a preview.
- **Apply** saves the choice to the block. **Cancel** or `Esc` closes without changes.
- A normal right-click (without Shift) still tunes the block as in vanilla.

### Notes on instruments

- Mob-head instruments (zombie, skeleton, creeper, ender dragon, wither skeleton, piglin, custom head) come from a head placed above the block and can't be chosen. If a head is above the block, its instrument is kept when you Apply.
- In vanilla, a note block's instrument is derived from the block below it. A chosen instrument stays until a block directly above or below the note block is placed or broken, at which point vanilla recalculates it.

## Requirements

| | Version |
|---|---|
| Minecraft | 26.3 |
| Java | 25 or newer |
| Fabric Loader | 0.20.1 or newer |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.166.2+26.3 (or a compatible 26.3 build) |

## Installation

### Singleplayer

Put the mod jar and Fabric API in your `mods` folder.

### Multiplayer

**Best experience: install the mod on both the client and the server.** The GUI is client-side, but the change is applied by the server:

1. **Server:** put `noteblockgui-<version>.jar` and Fabric API in the server's `mods` folder, running Fabric Loader for Minecraft 26.3.
2. **Each player:** install the same mod jar and Fabric API on their client.
3. Restart the server.

**Vanilla / unmodded servers (note only):** if the server doesn't have the mod, the GUI still opens but the instrument grid is disabled and the Apply button reads "Apply (note only - server has no mod)". Apply then tunes the block by sending ordinary right-clicks, one every 4 ticks (0.2 s), since vanilla raises the note by one per click (wrapping after F#5). Going up 7 notes takes 7 clicks, up to 24 for the largest change (about 5 seconds), and you'll hear each note play. This works within the server's normal rules (reach, spawn protection, protection plugins), but a server anti-cheat may object to the rapid clicks. Instruments cannot be changed this way, because vanilla derives them from the block below.

### What the server checks (mod installed)

The server validates every change and rejects it, with a message above the player's hotbar and an entry in the server log (`Rejected note change from …`), if:

- the note or instrument value is invalid;
- the block isn't a loaded note block;
- the player is more than 8 blocks from the block;
- the player isn't allowed to modify blocks there (e.g. **spawn protection** for non-operators, or a protection plugin/mod).

## Building

```
./gradlew build
```

The jar is written to `build/libs/`. Use `./gradlew runClient` to launch a development client. `JAVA_HOME` must point to a Java 25 JDK.

## License

[MIT](LICENSE)
