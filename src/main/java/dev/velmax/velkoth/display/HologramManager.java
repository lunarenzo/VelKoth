package dev.velmax.velkoth.display;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.capture.CaptureSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chunk-safe TextDisplay entities for active arenas.
 */
public class HologramManager {

    private final VelKothPlugin plugin;
    private final Map<String, TextDisplay> holograms = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public HologramManager(VelKothPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a new TextDisplay hologram above the center of the arena.
     * 
     * @param arena the arena
     */
    public void spawn(Arena arena) {
        if (!plugin.getPluginConfig().getDisplay().isHologramEnabled())
            return;

        Location center;
        if (arena.hologramLocation() != null) {
            center = arena.hologramLocation().clone();
        } else {
            center = arena.region().getCenter();
            // Adjust Y offset as configured (e.g. +3 blocks above center)
            center.add(0, plugin.getPluginConfig().getDisplay().getHologramYOffset(), 0);
        }

        // Always center to the block (X.5, Z.5)
        center.setX(center.getBlockX() + 0.5);
        center.setZ(center.getBlockZ() + 0.5);

        if (center.getWorld() == null)
            return;

        Bukkit.getRegionScheduler().run(plugin, center, task -> {
            // Remove existing if present to avoid duplication inside the regional thread
            TextDisplay existing = holograms.get(arena.id());
            if (existing != null && existing.isValid()) {
                existing.remove();
            }

            TextDisplay display = center.getWorld().spawn(center, TextDisplay.class, entity -> {
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setShadowed(true); // User requested true
                entity.setDefaultBackground(false);
                entity.setViewRange(32f);
                entity.setAlignment(TextDisplay.TextAlignment.CENTER);
                entity.setPersistent(false); // Do not persist in chunk files

                // Set initial state
                entity.text(Component.empty());
            });

            holograms.put(arena.id(), display);
        });
    }

    public void remove(Arena arena) {
        remove(arena.id());
    }

    public void remove(String arenaId) {
        TextDisplay display = holograms.remove(arenaId);
        if (display != null && display.isValid()) {
            if (plugin.isEnabled()) {
                display.getScheduler().run(plugin, scheduledTask -> display.remove(), null);
            } else {
                display.remove();
            }
        }
    }


    /**
     * Updates the location of the hologram dynamically if it is active.
     */
    public void updateLocation(Arena arena) {
        if (holograms.containsKey(arena.id())) {
            remove(arena.id());
            spawn(arena);
        }
    }

    /**
     * Removes all active holograms (e.g. on plugin disable).
     */
    public void removeAll() {
        for (String arenaId : new ArrayList<>(holograms.keySet())) {
            remove(arenaId);
        }
    }

    /**
     * Updates the text component of the hologram with current capture stats.
     * 
     * @param arena the arena
     */
    public void update(Arena arena) {
        if (!plugin.getPluginConfig().getDisplay().isHologramEnabled())
            return;

        TextDisplay display = holograms.get(arena.id());
        if (display == null || !display.isValid()) {
            // If invalid (e.g. chunk unloaded and entity lost somehow), respawn it
            spawn(arena);
            return;
        }
        
        final TextDisplay finalDisplay = display;
        CaptureSession session = plugin.getCaptureManager().getSession(arena.id());
        if (session == null) {
            // Should not happen if game is active, but fallback
            finalDisplay.getScheduler().run(plugin, scheduledTask -> finalDisplay.text(Component.empty()), null);
            return;
        }

        List<String> rawLines = plugin.getMessages().getHologramLines();
        Component finalComponent = Component.empty();

        String arenaName = arena.displayName();
        String capturerName = "None";
        String timeString = "0s";

        if (session.isContested()) {
            capturerName = plugin.getMessages().getCaptureContested();
        } else if (session.capturingPlayer() != null) {
            Player capPlayer = Bukkit.getPlayer(session.capturingPlayer());
            if (capPlayer != null) {
                capturerName = capPlayer.getName();
                String team = plugin.getTeamManager().getTeamName(capPlayer);
                if (team != null && !team.isEmpty()) {
                    capturerName = capturerName + " [" + team + "]";
                }
            }
        }

        timeString = formatTime(session, arena);

        // Build multi-line component
        boolean first = true;
        for (String line : rawLines) {
            if (!first) {
                finalComponent = finalComponent.append(Component.newline());
            }
            first = false;

            Component parsedLine = mm.deserialize(line,
                    Placeholder.unparsed("arena", arenaName),
                    Placeholder.unparsed("capturer", capturerName),
                    Placeholder.unparsed("time", timeString));
            finalComponent = finalComponent.append(parsedLine);
        }

        final Component finalComp = finalComponent;
        finalDisplay.getScheduler().run(plugin, scheduledTask -> finalDisplay.text(finalComp), null);
    }

    /**
     * Helper to format the time string
     */
    private String formatTime(CaptureSession session, Arena arena) {
        int seconds;
        int max;
        switch (arena.captureMode()) {
            case SCORE -> {
                seconds = session.elapsedSeconds();
                max = arena.maxScore();
                return seconds + "/" + max;
            }
            case CAPTURE -> {
                seconds = session.elapsedSeconds();
                max = arena.captureTime();
                int timeRemaining = Math.max(0, max - seconds);
                int mins = timeRemaining / 60;
                int secs = timeRemaining % 60;
                if (mins > 0)
                    return String.format("%02d:%02d", mins, secs);
                return secs + "s";
            }
        }
        return "";
    }
}
