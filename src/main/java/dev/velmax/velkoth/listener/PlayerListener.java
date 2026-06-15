package dev.velmax.velkoth.listener;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.capture.CaptureSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import java.util.UUID;

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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        // Check active SCORE mode arenas
        for (Arena arena : plugin.getArenaManager().getActiveArenas()) {
            if (arena.captureMode() != Arena.CaptureMode.SCORE) {
                continue;
            }

            CaptureSession session = plugin.getCaptureManager().getSession(arena.id());
            if (session == null || session.capturingPlayer() == null) {
                continue;
            }

            // Check if victim died inside the capture zone
            if (!arena.region().contains(victim.getLocation())) {
                continue;
            }

            // Check if the killer (or their team) is currently holding this hill
            UUID capturingPlayerUuid = session.capturingPlayer();
            if (capturingPlayerUuid == null) {
                continue;
            }

            Player capturer = org.bukkit.Bukkit.getPlayer(capturingPlayerUuid);
            if (capturer == null) {
                continue;
            }

            boolean isDefender = killer.getUniqueId().equals(capturer.getUniqueId()) ||
                    plugin.getTeamManager().isSameTeam(killer, capturer);

            if (isDefender) {
                int killBonus = plugin.getPluginConfig().getCaptureDefaults().getScoreKillBonus();
                if (killBonus <= 0) {
                    continue;
                }

                // Award points
                String identifier = plugin.getCaptureManager().getScoreIdentifier(killer);
                int newScore = session.addScore(identifier, killBonus);

                // Notify killer (and defender team) via message
                String template = plugin.getMessages().getScoreKillBonus();
                net.kyori.adventure.text.Component message = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        template,
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("points", String.valueOf(killBonus)),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("victim", victim.getName())
                );
                
                killer.sendMessage(plugin.getDisplayManager().getPrefix().append(message));
                plugin.getDisplayManager().playTickSound(killer);

                // Check win condition
                if (newScore >= arena.maxScore()) {
                    plugin.getCaptureManager().handleWin(arena, session, killer);
                }
            }
        }
    }
}
