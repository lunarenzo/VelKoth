package dev.velmax.velkoth.manager;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.api.event.KothStopEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages dynamically triggering KoTH events based on server player count.
 * Leverages atomic flags for safe concurrent accesses across region threads.
 */
public final class DynamicTriggerManager implements Listener {

    private final VelKothPlugin plugin;
    private final AtomicLong lastTriggerTime = new AtomicLong(0L);
    private final AtomicBoolean pendingTrigger = new AtomicBoolean(false);
    private ScheduledTask periodicTask;

    public DynamicTriggerManager(VelKothPlugin plugin) {
        this.plugin = plugin;
        loadLastTriggerTime();
    }

    private void loadLastTriggerTime() {
        String val = plugin.getDatabaseManager().getMetadataSync("dynamic_trigger_last_time");
        if (val != null) {
            try {
                lastTriggerTime.set(Long.parseLong(val));
            } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * Starts the periodic check task (running every 30 seconds).
     */
    public void start() {
        if (periodicTask != null) {
            return;
        }
        // Run every 30 seconds (600 game ticks)
        periodicTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> checkTrigger(), 600L, 600L);
    }

    /**
     * Cancels the periodic check task.
     */
    public void stop() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    /**
     * Listens to the KothStopEvent to mark the start of the dynamic trigger cooldown.
     * This guarantees the cooldown counts down from when the KOTH finishes, not when it starts.
     */
    @EventHandler
    public void onKothStop(KothStopEvent event) {
        long now = System.currentTimeMillis();
        lastTriggerTime.set(now);
        plugin.getDatabaseManager().setMetadata("dynamic_trigger_last_time", String.valueOf(now));
    }

    /**
     * Checks all dynamic trigger rules and starts a KOTH event if conditions are met.
     * Safely handles concurrent join events from regional threads by avoiding redundant task queueing.
     */
    public void checkTrigger() {
        var config = plugin.getPluginConfig().getDynamicTriggers();
        if (!config.isEnabled()) {
            return;
        }

        // Do not trigger if a KOTH event is already active
        if (plugin.getArenaManager().hasActiveArena()) {
            return;
        }

        // Check if there is already a pending scheduler task to avoid task spamming
        if (pendingTrigger.get()) {
            return;
        }

        // Check cooldown constraint
        long now = System.currentTimeMillis();
        long cooldownMs = TimeUnit.MINUTES.toMillis(config.getCooldownMinutes());
        if (now - lastTriggerTime.get() < cooldownMs) {
            return;
        }

        // Check player count threshold
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (onlinePlayers < config.getMinPlayers()) {
            return;
        }

        // Atomically ensure only one task is scheduled to start the event
        if (!pendingTrigger.compareAndSet(false, true)) {
            return;
        }

        // Delegate to the global region thread to remain fully Folia-compliant
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            try {
                // Re-verify under global thread context
                if (plugin.getArenaManager().hasActiveArena()) {
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
                    plugin.getCaptureManager().startArena(selectedArena);
                    plugin.getLogger().info("Dynamic trigger fired: Started KOTH '" + selectedArena.id() +
                            "' because online players reached " + onlinePlayers + " (threshold: " + config.getMinPlayers() + ").");
                }
            } finally {
                // Release the pending trigger flag
                pendingTrigger.set(false);
            }
        });
    }

    public long getLastTriggerTime() {
        return lastTriggerTime.get();
    }

    public void setLastTriggerTime(long lastTriggerTime) {
        this.lastTriggerTime.set(lastTriggerTime);
    }
}
