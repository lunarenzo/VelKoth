package dev.velmax.velkoth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

/**
 * All player-facing messages, using MiniMessage format.
 */
@Header("VelKoth Messages — Uses MiniMessage formatting")
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class MessagesConfig extends OkaeriConfig {

    @Comment("Prefix prepended to all messages")
    private String prefix = "<gradient:#ff6b6b:#ffa500><bold>KoTH</bold></gradient> <dark_gray>»</dark_gray> ";

    // ── Arena lifecycle ──
    @Comment("Sent when a KoTH event starts")
    private String eventStart = "<green>The <gold><arena></gold> KoTH has started! Get to the hill!";

    @Comment("Sent when a KoTH event stops")
    private String eventStop = "<red>The <gold><arena></gold> KoTH has ended.";

    @Comment("Sent when a KoTH event is paused")
    private String eventPaused = "<yellow>The <gold><arena></gold> KoTH has been paused.";

    @Comment("Sent when a KoTH event is resumed")
    private String eventResumed = "<green>The <gold><arena></gold> KoTH has been resumed!";

    // ── Capture ──
    @Comment("Sent when a player starts capturing")
    private String captureStart = "<green><player> is capturing the hill!";

    @Comment("Sent when capture is contested")
    private String captureContested = "<yellow>The hill is contested!";

    @Comment("Sent when a player wins the KoTH")
    private String captureWin = "<gold><bold>☆</bold></gold> <green><player></green> has captured the <gold><arena></gold> KoTH! <gold><bold>☆</bold></gold>";

    @Comment("Score mode — points earned")
    private String scorePoint = "<gray>+1 point <dark_gray>(<aqua><score></aqua>/<aqua><max></aqua>)</dark_gray>";

    @Comment("Sent to a player when they get a kill bonus in score mode")
    private String scoreKillBonus = "<green><bold>+<points></bold> Kill Bonus! <gray>(Defeated <victim>)</gray>";

    // ── Titles ──
    @Comment("Title shown on KoTH start")
    private String titleStart = "<gold><bold>KOTH</bold></gold>";
    private String subtitleStart = "<gray>The hill is open!";

    @Comment("Title shown on win")
    private String titleWin = "<green><bold>VICTORY!</bold></green>";
    private String subtitleWin = "<gray>You captured the hill!";

    @Comment("Title shown when contested")
    private String titleContested = "<yellow><bold>CONTESTED</bold></yellow>";
    private String subtitleContested = "<gray>Multiple players on the hill!";

    // ── BossBar ──
    @Comment("BossBar text during capture")
    private String bossBarCapturing = "<gold><arena></gold> <dark_gray>|</dark_gray> <green><player></green> capturing <dark_gray>(<time>)</dark_gray>";

    @Comment("BossBar text when contested")
    private String bossBarContested = "<gold><arena></gold> <dark_gray>|</dark_gray> <yellow>Contested!</yellow>";

    @Comment("BossBar text when idle (no one on hill)")
    private String bossBarIdle = "<gold><arena></gold> <dark_gray>|</dark_gray> <gray>Waiting for capture...";

    // ── ActionBar ──
    @Comment("ActionBar text during capture")
    private String actionBarCapturing = "<green>Capturing: <white><time></white> remaining";

    // ── Scoreboard ──
    @Comment("Scoreboard title")
    private String scoreboardTitle = "<gradient:#ff6b6b:#ffa500><bold>VelMax KoTH</bold></gradient>";

    @Comment("Scoreboard lines when an arena is active")
    private java.util.List<String> scoreboardLinesActive = java.util.List.of(
            "<gray>----------------------",
            "<white>Arena: <gold><arena></gold>",
            "",
            "<white>Status: <yellow><capturer></yellow>",
            "<white>Time: <green><time></green>",
            "<gray>----------------------");

    @Comment("Scoreboard lines when no arena is active")
    private java.util.List<String> scoreboardLinesIdle = java.util.List.of(
            "<gray>----------------------",
            "<white>No active events.",
            "<gray>Waiting for next start...",
            "<gray>----------------------");

    // ── Hologram ──
    @Comment("Hologram lines displayed above the active hill")
    private java.util.List<String> hologramLines = java.util.List.of(
            "<gradient:#ff6b6b:#ffa500><bold><arena> KoTH</bold></gradient>",
            "<white>Status: <yellow><capturer></yellow>",
            "<white>Time: <green><time></green>");

    // ── Admin ──
    @Comment("Arena created")
    private String arenaCreated = "<green>Arena <gold><arena></gold> has been created.";

    @Comment("Arena deleted")
    private String arenaDeleted = "<red>Arena <gold><arena></gold> has been deleted.";

    @Comment("Arena not found")
    private String arenaNotFound = "<red>Arena <gold><arena></gold> not found.";

    @Comment("Arena already exists")
    private String arenaExists = "<red>Arena <gold><arena></gold> already exists.";

    @Comment("Arena already active")
    private String arenaAlreadyActive = "<red>Arena <gold><arena></gold> is already running.";

    @Comment("Arena not active")
    private String arenaNotActive = "<red>Arena <gold><arena></gold> is not running.";

    @Comment("Selection incomplete")
    private String selectionIncomplete = "<red>You must set both positions first! Use <gold>/koth wand</gold>.";

    @Comment("Position set")
    private String positionSet = "<green>Position <gold><pos></gold> set at <aqua><x></aqua>, <aqua><y></aqua>, <aqua><z></aqua>.";

    @Comment("Wand given")
    private String wandGiven = "<green>KoTH wand given! <gray>Left-click for pos1, right-click for pos2.";

    @Comment("Config reloaded")
    private String reloaded = "<green>Configuration reloaded.";

    @Comment("No permission")
    private String noPermission = "<red>You don't have permission to do that.";

    @Comment("Player only command")
    private String playerOnly = "<red>This command can only be run by a player.";

    @Comment("Sent when checking the next scheduled KoTH")
    private String nextScheduled = "<green>The next <gold><arena></gold> KoTH starts in <aqua><time></aqua>.";

    @Comment("Sent when checking the next scheduled KoTH and there are none")
    private String noNextScheduled = "<red>There are no scheduled events for <gold><arena></gold>.";

    @Comment("Arena property modified")
    private String arenaModified = "<green>Arena <gold><arena></gold> property <yellow><property></yellow> set to <white><value></white>.";

    // Getters
    public String getPrefix() {
        return prefix;
    }

    public String getEventStart() {
        return eventStart;
    }

    public String getEventStop() {
        return eventStop;
    }

    public String getEventPaused() {
        return eventPaused;
    }

    public String getEventResumed() {
        return eventResumed;
    }

    public String getCaptureStart() {
        return captureStart;
    }

    public String getCaptureContested() {
        return captureContested;
    }

    public String getCaptureWin() {
        return captureWin;
    }

    public String getScorePoint() {
        return scorePoint;
    }

    public String getScoreKillBonus() {
        return scoreKillBonus;
    }

    public String getTitleStart() {
        return titleStart;
    }

    public String getSubtitleStart() {
        return subtitleStart;
    }

    public String getTitleWin() {
        return titleWin;
    }

    public String getSubtitleWin() {
        return subtitleWin;
    }

    public String getTitleContested() {
        return titleContested;
    }

    public String getSubtitleContested() {
        return subtitleContested;
    }

    public String getBossBarCapturing() {
        return bossBarCapturing;
    }

    public String getBossBarContested() {
        return bossBarContested;
    }

    public String getBossBarIdle() {
        return bossBarIdle;
    }

    public String getActionBarCapturing() {
        return actionBarCapturing;
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public java.util.List<String> getScoreboardLinesActive() {
        return scoreboardLinesActive;
    }

    public java.util.List<String> getScoreboardLinesIdle() {
        return scoreboardLinesIdle;
    }

    public java.util.List<String> getHologramLines() {
        return hologramLines;
    }

    public String getArenaCreated() {
        return arenaCreated;
    }

    public String getArenaDeleted() {
        return arenaDeleted;
    }

    public String getArenaNotFound() {
        return arenaNotFound;
    }

    public String getArenaExists() {
        return arenaExists;
    }

    public String getArenaAlreadyActive() {
        return arenaAlreadyActive;
    }

    public String getArenaNotActive() {
        return arenaNotActive;
    }

    public String getSelectionIncomplete() {
        return selectionIncomplete;
    }

    public String getPositionSet() {
        return positionSet;
    }

    public String getWandGiven() {
        return wandGiven;
    }

    public String getReloaded() {
        return reloaded;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getPlayerOnly() {
        return playerOnly;
    }

    public String getNextScheduled() {
        return nextScheduled;
    }

    public String getNoNextScheduled() {
        return noNextScheduled;
    }

    public String getArenaModified() {
        return arenaModified;
    }
}
