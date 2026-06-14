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
     * 
     * @param player the player
     */
    public void createBoard(Player player) {
        if (!plugin.getPluginConfig().getDisplay().isScoreboardEnabled())
            return;

        boolean isIdle = plugin.getArenaManager().getActiveArenas().isEmpty();
        if (isIdle && !plugin.getPluginConfig().getDisplay().isShowIdleScoreboard()) {
            // Don't create the board yet, wait for an event
            return;
        }

        FastBoard board = new FastBoard(player);
        String titleString = plugin.getMessages().getScoreboardTitle();
        board.updateTitle(mm.deserialize(titleString));
        boards.put(player.getUniqueId(), board);

        pauseOtherScoreboards(player);
        updateBoard(player);
    }

    /**
     * Removes and cleans up a player's scoreboard when they quit.
     * 
     * @param player the player
     */
    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
        resumeOtherScoreboards(player);
    }

    /**
     * Re-renders the scoreboard for a specific player.
     * 
     * @param player the player to update
     */
    private void updateBoard(Player player) {
        java.util.Collection<Arena> activeArenas = plugin.getArenaManager().getActiveArenas();
        boolean isIdle = activeArenas.isEmpty();

        if (isIdle && !plugin.getPluginConfig().getDisplay().isShowIdleScoreboard()) {
            FastBoard existing = boards.remove(player.getUniqueId());
            if (existing != null) {
                existing.delete();
                resumeOtherScoreboards(player);
            }
            return; // Don't show inactive board
        }

        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) {
            createBoard(player);
            board = boards.get(player.getUniqueId());
            if (board == null)
                return;
        }

        List<String> rawLines = isIdle ? plugin.getMessages().getScoreboardLinesIdle() 
                                       : plugin.getMessages().getScoreboardLinesActive();
        List<Component> finalLines = new ArrayList<>(rawLines.size());

        if (isIdle) {
            for (String line : rawLines) {
                finalLines.add(mm.deserialize(line));
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

            for (String line : rawLines) {
                Component parsed = mm.deserialize(line,
                        Placeholder.unparsed("arena", arenaName),
                        Placeholder.unparsed("capturer", capturerName),
                        Placeholder.unparsed("time", timeString));
                finalLines.add(parsed);
            }
        }

        board.updateLines(finalLines);
    }

    /**
     * Updates the scoreboards of all online players.
     * Pre-builds lines once to achieve efficient O(1) scaling relative to player count.
     */
    public void updateAll() {
        if (!plugin.getPluginConfig().getDisplay().isScoreboardEnabled())
            return;

        java.util.Collection<Arena> activeArenas = plugin.getArenaManager().getActiveArenas();
        boolean isIdle = activeArenas.isEmpty();

        if (isIdle && !plugin.getPluginConfig().getDisplay().isShowIdleScoreboard()) {
            // Remove boards for all players if idle and idle scoreboard is disabled
            for (Player player : Bukkit.getOnlinePlayers()) {
                FastBoard existing = boards.remove(player.getUniqueId());
                if (existing != null) {
                    existing.delete();
                    resumeOtherScoreboards(player);
                }
            }
            return;
        }

        List<String> rawLines = isIdle ? plugin.getMessages().getScoreboardLinesIdle() 
                                       : plugin.getMessages().getScoreboardLinesActive();
        List<Component> finalLines = new ArrayList<>(rawLines.size());

        if (isIdle) {
            for (String line : rawLines) {
                finalLines.add(mm.deserialize(line));
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

            for (String line : rawLines) {
                Component parsed = mm.deserialize(line,
                        Placeholder.unparsed("arena", arenaName),
                        Placeholder.unparsed("capturer", capturerName),
                        Placeholder.unparsed("time", timeString));
                finalLines.add(parsed);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = boards.get(player.getUniqueId());
            if (board == null || board.isDeleted()) {
                createBoard(player);
                board = boards.get(player.getUniqueId());
                if (board == null)
                    continue;
            }
            board.updateLines(finalLines);
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
                if (mins > 0)
                    return String.format("%02d:%02d", mins, secs);
                return secs + "s";
            }
        }
        return "";
    }
}
