# Tasks for Hologram Positioning and Commands

Implement centering holograms on blocks, adding commands to modify and change hologram positions, and verifying optimal performance using Display entities (TextDisplay).

## Todo List
- [x] Add `hologramLocation` field (nullable Location) in `Arena.java` and its getter/setter
- [x] Add `hologramLocation` field (List<Double>) in `ArenaConfig.ArenaEntry` and its getter/setter
- [x] Update serialization/deserialization logic in `ArenaManager.java` to support custom hologram location
- [x] Modify `HologramManager.java` to:
  - Spawn hologram at custom location if set, otherwise fallback to region center + Y offset
  - Always center X/Z coordinates of hologram location to block center (X.5, Z.5)
- [x] Add commands in `KothCommand.java` under `/koth set hologram`:
  - `/koth set hologram <arena>` (sets to current player position)
  - `/koth set hologram <arena> <x> <y> <z>` (sets to custom coordinates)
  - `/koth set hologram <arena> reset` (clears the custom location)
- [x] Add support to update active holograms immediately when their position changes

## Review Section
- Custom `hologramLocation` has been added to `Arena` and `ArenaConfig.ArenaEntry`.
- Deserialization and serialization logic have been fully integrated in `ArenaManager`.
- Spawning logic in `HologramManager` now checks for a custom location first and falls back to the region center + Y offset if not set.
- Both custom and default hologram positions are automatically centered to the block's horizontal middle (`X.5`, `Z.5`).
- TextDisplay (Display Entity) is used for high-performance hologram rendering.
- New commands registered: `/koth set hologram <arena>`, `/koth set hologram <arena> <x> <y> <z>`, and `/koth set hologram <arena> reset`.
- If an arena is active, any modification to its hologram position immediately updates the active hologram in the game.
- Verified build and compilation successfully with Java 21 toolchain.
