package dev.velmax.velkoth.display;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.capture.CaptureSession;
import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.scoreboard.Scoreboard;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player FastBoard scoreboards using an event-driven approach.
 */
public class ScoreboardManager {

    private final VelKothPlugin plugin;
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, Scoreboard> previousScoreboards = new ConcurrentHashMap<>();
    private Scoreboard emptyScoreboard;
    private final boolean isFolia;
    private boolean hasTab = false;
    private Method tabGetInstance;
    private Method tabGetScoreboardManager;
    private Method tabGetPlayer;
    private Method tabSetScoreboardVisible;

    // Cache to track if players want the scoreboard visible
    private final Map<UUID, Boolean> hudPreferences = new ConcurrentHashMap<>();

    // Cache parsed components to achieve 0% MiniMessage parser usage on hot path
    private final Map<String, Component> miniMessageCache = new java.util.LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
            return size() > 256;
        }
    };

    private synchronized Component parseMiniMessage(String text) {
        return miniMessageCache.computeIfAbsent(text, mm::deserialize);
    }

    public void reload() {
        miniMessageCache.clear();
    }

    public ScoreboardManager(VelKothPlugin plugin) {
        this.plugin = plugin;
        this.isFolia = checkFolia();
        initHooks();
    }

    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void initHooks() {
        if (Bukkit.getPluginManager().getPlugin("TAB") != null) {
            try {
                Class<?> tabAPIClass = Class.forName("me.neznamy.tab.api.TabAPI");
                tabGetInstance = tabAPIClass.getMethod("getInstance");
                tabGetScoreboardManager = tabAPIClass.getMethod("getScoreboardManager");
                tabGetPlayer = tabAPIClass.getMethod("getPlayer", UUID.class);

                Object api = tabGetInstance.invoke(null);
                Object sm = tabGetScoreboardManager.invoke(api);
                if (sm != null) {
                    tabSetScoreboardVisible = sm.getClass().getMethod("setScoreboardVisible",
                            Class.forName("me.neznamy.tab.api.TabPlayer"), boolean.class, boolean.class);
                    hasTab = true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Found TAB plugin but failed to hook into its Scoreboard API.");
            }
        }
    }

    /**
     * Initializes a scoreboard for a newly joined player.
     * Loads player preference asynchronously from database.
     * 
     * @param player the player
     */
    public void createBoard(Player player) {
        if (!plugin.getPluginConfig().getDisplay().isScoreboardEnabled())
            return;

        UUID uuid = player.getUniqueId();
        // Load preference asynchronously if not cached
        if (!hudPreferences.containsKey(uuid)) {
            plugin.getDatabaseManager().getMetadata(uuid + "_hud_enabled").thenAccept(value -> {
                boolean enabled = value == null || Boolean.parseBoolean(value);
                hudPreferences.put(uuid, enabled);
                runOnPlayerThread(player, () -> {
                    if (player.isOnline()) {
                        java.util.Collection<Arena> activeArenas = plugin.getArenaManager().getActiveArenas();
                        updatePlayerBoard(player, prebuildLines(activeArenas), activeArenas);
                    }
                });
            });
        } else {
            runOnPlayerThread(player, () -> {
                if (player.isOnline()) {
                    java.util.Collection<Arena> activeArenas = plugin.getArenaManager().getActiveArenas();
                    updatePlayerBoard(player, prebuildLines(activeArenas), activeArenas);
                }
            });
        }
    }

    /**
     * Removes and cleans up a player's scoreboard when they quit.
     * 
     * @param player the player
     */
    public void removeBoard(Player player) {
        UUID uuid = player.getUniqueId();
        hudPreferences.remove(uuid);
        FastBoard board = boards.remove(uuid);
        if (board != null) {
            board.delete();
        }
        resumeOtherScoreboards(player);
    }

    /**
     * Toggles the HUD visibility preference for the player, saves it, and updates immediately.
     */
    public void toggleHud(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = hudPreferences.getOrDefault(uuid, Boolean.TRUE);
        boolean nextState = !current;
        hudPreferences.put(uuid, nextState);

        // Async save to database metadata
        plugin.getDatabaseManager().setMetadata(uuid + "_hud_enabled", String.valueOf(nextState));

        // Display feedback
        if (nextState) {
            plugin.getDisplayManager().sendPrefixed(player, "<green>KOTH Scoreboard HUD has been enabled.</green>");
        } else {
            plugin.getDisplayManager().sendPrefixed(player, "<red>KOTH Scoreboard HUD has been disabled.</red>");
        }

        // Apply change immediately on the player thread
        runOnPlayerThread(player, () -> {
            if (player.isOnline()) {
                java.util.Collection<Arena> activeArenas = plugin.getArenaManager().getActiveArenas();
                updatePlayerBoard(player, prebuildLines(activeArenas), activeArenas);
            }
        });
    }

    /**
     * Utility helper to safely schedule player updates.
     */
    private void runOnPlayerThread(Player player, Runnable runnable) {
        if (isFolia) {
            player.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            runnable.run();
        }
    }

    /**
     * Checks if a player qualifies to see the scoreboard based on contextual world & proximity rules.
     */
    private boolean shouldShowScoreboard(Player player, java.util.Collection<Arena> activeArenas) {
        if (activeArenas.isEmpty()) {
            return plugin.getPluginConfig().getDisplay().isShowIdleScoreboard();
        }

        var displaySettings = plugin.getPluginConfig().getDisplay();
        boolean onlyInWorld = displaySettings.isScoreboardOnlyInArenaWorld();
        double proximityRadius = displaySettings.getScoreboardProximityRadius();

        if (!onlyInWorld && proximityRadius <= 0) {
            return true;
        }

        for (Arena arena : activeArenas) {
            if (arena.region() == null || arena.region().getWorld() == null) {
                continue;
            }

            // Check world matching
            if (onlyInWorld && !player.getWorld().equals(arena.region().getWorld())) {
                continue;
            }

            // Check proximity (distance squared optimization)
            if (proximityRadius > 0) {
                if (!player.getWorld().equals(arena.region().getWorld())) {
                    continue;
                }
                double distSq = player.getLocation().distanceSquared(arena.region().getCenter());
                if (distSq > proximityRadius * proximityRadius) {
                    continue;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Handles creation, updates, and deletion logic for a single player's board.
     * MUST run on the player's thread (which is guaranteed by the callers).
     */
    private void updatePlayerBoard(Player player, List<Component> finalLines, java.util.Collection<Arena> activeArenas) {
        UUID uuid = player.getUniqueId();
        Boolean preference = hudPreferences.get(uuid);
        
        // If preference is not cached yet, createBoard has been/will be called
        if (preference == null) {
            return;
        }

        // If player has manually disabled the HUD or fails contextual checks, delete/hide board
        if (!preference || !shouldShowScoreboard(player, activeArenas)) {
            FastBoard existing = boards.remove(uuid);
            if (existing != null) {
                existing.delete();
                resumeOtherScoreboards(player);
            }
            return;
        }

        // Retrieve or initialize FastBoard
        FastBoard board = boards.get(uuid);
        if (board == null || board.isDeleted()) {
            board = new FastBoard(player);
            String titleString = plugin.getMessages().getScoreboardTitle();
            board.updateTitle(mm.deserialize(titleString));
            boards.put(uuid, board);
            pauseOtherScoreboards(player);
        }

        board.updateLines(finalLines);
    }

    /**
     * Pre-builds scoreboard lines once to scale in O(1) time complexity.
     */
    private List<Component> prebuildLines(java.util.Collection<Arena> activeArenas) {
        boolean isIdle = activeArenas.isEmpty();
        List<String> rawLines = isIdle ? plugin.getMessages().getScoreboardLinesIdle() 
                                       : plugin.getMessages().getScoreboardLinesActive();
        List<Component> finalLines = new ArrayList<>(rawLines.size());

        if (isIdle) {
            for (String line : rawLines) {
                finalLines.add(parseMiniMessage(line));
            }
        } else {
            Arena arena = activeArenas.iterator().next();
            CaptureSession session = plugin.getCaptureManager().getSession(arena.id());

            String arenaName = arena.displayName();
            String capturerName = "None";
            String timeString = "0s";

            if (session != null) {
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
            }

            TemplateCache templateCache = plugin.getDisplayManager().getTemplateCache();
            for (String line : rawLines) {
                Component templateComponent = templateCache.getTemplate(line);
                Component parsedLine = templateCache.resolve(templateComponent, arenaName, null, timeString, capturerName);
                finalLines.add(parsedLine);
            }
        }
        return finalLines;
    }

    /**
     * Updates the scoreboards of all online players.
     * Safely runs each update on the player's region thread.
     */
    public void updateAll() {
        if (!plugin.getPluginConfig().getDisplay().isScoreboardEnabled())
            return;

        java.util.Collection<Arena> activeArenas = plugin.getArenaManager().getActiveArenas();
        List<Component> finalLines = prebuildLines(activeArenas);

        for (Player player : Bukkit.getOnlinePlayers()) {
            runOnPlayerThread(player, () -> {
                if (player.isOnline()) {
                    updatePlayerBoard(player, finalLines, activeArenas);
                }
            });
        }
    }

    private void pauseOtherScoreboards(Player player) {
        if (isFolia || !plugin.getPluginConfig().getDisplay().isOverrideOtherScoreboards())
            return;

        // Bukkit Dummy Scoreboard Overriding (Handles SimpleScore and most plugins)
        if (emptyScoreboard == null) {
            emptyScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        Scoreboard current = player.getScoreboard();
        if (current != Bukkit.getScoreboardManager().getMainScoreboard() && current != emptyScoreboard) {
            previousScoreboards.put(player.getUniqueId(), current);
        }
        player.setScoreboard(emptyScoreboard);

        // TAB API Overriding (FastBoards might flicker with TAB if it ignores the dummy
        // board)
        if (hasTab) {
            try {
                Object api = tabGetInstance.invoke(null);
                Object tabPlayer = tabGetPlayer.invoke(api, player.getUniqueId());
                if (tabPlayer != null) {
                    Object sm = tabGetScoreboardManager.invoke(api);
                    tabSetScoreboardVisible.invoke(sm, tabPlayer, false, false);
                }
            } catch (Exception e) {
                // Ignore silent reflection errors
            }
        }
    }

    private void resumeOtherScoreboards(Player player) {
        if (isFolia || !plugin.getPluginConfig().getDisplay().isOverrideOtherScoreboards())
            return;

        // Restore Bukkit Scoreboard
        Scoreboard previous = previousScoreboards.remove(player.getUniqueId());
        if (previous != null) {
            player.setScoreboard(previous);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        // Restore TAB API
        if (hasTab) {
            try {
                Object api = tabGetInstance.invoke(null);
                Object tabPlayer = tabGetPlayer.invoke(api, player.getUniqueId());
                if (tabPlayer != null) {
                    Object sm = tabGetScoreboardManager.invoke(api);
                    tabSetScoreboardVisible.invoke(sm, tabPlayer, true, false);
                }
            } catch (Exception e) {
                // Ignore silent reflection errors
            }
        }
    }

    /**
     * Helper to format the time string for the scoreboard
     */
    private String formatTime(CaptureSession session, Arena arena) {
        int seconds;
        int max;
        switch (arena.captureMode()) {
            case SCORE -> {
                // In SCORE mode, we don't have a specific player's score globally for the
                // board,
                // so we show the highest score or time remaining.
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
                if (mins > 0) {
                    return mins + ":" + (secs < 10 ? "0" + secs : secs);
                }
                return secs + "s";
            }
        }
        return "";
    }
}
