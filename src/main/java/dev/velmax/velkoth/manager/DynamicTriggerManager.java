package dev.velmax.velkoth.manager;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import org.bukkit.Bukkit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Manages dynamically triggering KoTH events based on server player count.
 */
public final class DynamicTriggerManager {

    private final VelKothPlugin plugin;
    private long lastTriggerTime = 0L;

    public DynamicTriggerManager(VelKothPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks all dynamic trigger rules and starts a KOTH event if conditions are met.
     */
    public void checkTrigger() {
        var config = plugin.getPluginConfig().getDynamicTriggers();
        if (!config.isEnabled()) {
            return;
        }

        // Do not trigger if a KOTH event is already active
        if (!plugin.getArenaManager().getActiveArenas().isEmpty()) {
            return;
        }

        // Check cooldown constraint
        long now = System.currentTimeMillis();
        long cooldownMs = TimeUnit.MINUTES.toMillis(config.getCooldownMinutes());
        if (now - lastTriggerTime < cooldownMs) {
            return;
        }

        // Check player count threshold
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (onlinePlayers < config.getMinPlayers()) {
            return;
        }

        // Delegate to the global region thread to remain fully Folia-compliant
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            // Re-verify under global thread context
            if (!plugin.getArenaManager().getActiveArenas().isEmpty()) {
                return;
            }
            if (Bukkit.getOnlinePlayers().size() < config.getMinPlayers()) {
                return;
            }

            // Resolve target arena
            List<String> configuredArenas = config.getArenas();
            Arena selectedArena = null;

            if (configuredArenas.contains("random") || configuredArenas.isEmpty()) {
                // Select a random arena from currently idle ones
                var idleArenas = plugin.getArenaManager().getArenas().stream()
                        .filter(a -> a.state() == Arena.ArenaState.IDLE)
                        .toList();
                if (!idleArenas.isEmpty()) {
                    selectedArena = idleArenas.get(ThreadLocalRandom.current().nextInt(idleArenas.size()));
                }
            } else {
                // Pick the first configured arena that is idle
                for (String id : configuredArenas) {
                    Arena arena = plugin.getArenaManager().getArena(id);
                    if (arena != null && arena.state() == Arena.ArenaState.IDLE) {
                        selectedArena = arena;
                        break;
                    }
                }
            }

            if (selectedArena != null) {
                lastTriggerTime = now;
                plugin.getCaptureManager().startArena(selectedArena);
                plugin.getLogger().info("Dynamic trigger fired: Started KOTH '" + selectedArena.id() +
                        "' because online players reached " + onlinePlayers + " (threshold: " + config.getMinPlayers() + ").");
            }
        });
    }

    public long getLastTriggerTime() {
        return lastTriggerTime;
    }

    public void setLastTriggerTime(long lastTriggerTime) {
        this.lastTriggerTime = lastTriggerTime;
    }
}
