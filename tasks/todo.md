# Tasks for Reload Hologram Fixes

Fix issues where holograms stay in the world when the plugin or server is reloaded during an ongoing KoTH event.

## Todo List
- [x] Change the key type of `holograms` map in `HologramManager.java` from `Arena` to `String` (arena ID) to prevent instance mismatch issues during config reloads
- [x] Add `entity.setPersistent(false)` to hologram entities to prevent them from persisting in chunk files during crashes/reloads/chunk unloads
- [x] Shutdown active capture sessions and remove holograms dynamically in `reloadAllConfigs()` before reloading configurations

## Review Section
- Bushed version to `1.0.9` in `gradle.properties`.
- Refactored `HologramManager` to map active holograms using the `String` (arena ID) as the key instead of the `Arena` instance.
- Configured hologram `TextDisplay` entities with `.setPersistent(false)` so they are never saved into chunk region files and clean up naturally on restarts, reloads, crashes, or chunk unloads.
- Updated `reloadAllConfigs()` to gracefully shutdown all ongoing events and remove active holograms before configurations and arenas are reloaded.
- Staged all changes and committed to git repository.
