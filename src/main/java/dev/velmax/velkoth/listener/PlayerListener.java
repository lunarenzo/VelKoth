package dev.velmax.velkoth.listener;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.capture.CaptureSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit events for KoTH state management.
 */
public final class PlayerListener implements Listener {

    private final VelKothPlugin plugin;

    public PlayerListener(VelKothPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Clean up wand selections
        plugin.getWandManager().clearSelection(event.getPlayer());

        // If the player was capturing, the tick loop's grace period will handle it
        for (Arena arena : plugin.getArenaManager().getActiveArenas()) {
            CaptureSession session = plugin.getCaptureManager().getSession(arena.id());
            if (session != null && event.getPlayer().getUniqueId().equals(session.capturingPlayer())) {
                session.setLastCapturer(event.getPlayer().getUniqueId());
            }
        }

        // Remove scoreboard
        plugin.getDisplayManager().getScoreboardManager().removeBoard(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // BossBars are server-wide; new players don't auto-see them
        // We need no special handling as the BossBar tick will include them

        // Create player scoreboard
        plugin.getDisplayManager().getScoreboardManager().createBoard(event.getPlayer());

        // Check for dynamic player count triggers
        plugin.getDynamicTriggerManager().checkTrigger();
    }
}
