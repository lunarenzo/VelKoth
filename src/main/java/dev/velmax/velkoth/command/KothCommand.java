package dev.velmax.velkoth.command;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.arena.region.CuboidRegion;
import dev.velmax.velkoth.reward.Reward;
import dev.velmax.velkoth.config.PluginConfig;
import dev.velmax.velkoth.manager.WandManager;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.bukkit.Location;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main /koth command handler using Incendo Cloud (v2).
 */
public final class KothCommand {

    private final VelKothPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PaperCommandManager<Source> manager;

    public KothCommand(VelKothPlugin plugin) {
        this.plugin = plugin;
        this.manager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);

        registerCommands();
    }

    private void registerCommands() {
        var base = manager.commandBuilder("koth").permission("velkoth.use");

        manager.command(base
                .handler(ctx -> handleHelp(ctx.sender().source())));

        manager.command(base.literal("help")
                .handler(ctx -> handleHelp(ctx.sender().source())));

        SuggestionProvider<Source> arenaSuggestions = (ctx,
                input) -> CompletableFuture.completedFuture(plugin.getArenaManager().getArenaIds().stream()
                        .map(Suggestion::suggestion)
                        .toList());

        manager.command(base.literal("create")
                .permission("velkoth.admin")
                .senderType(PlayerSource.class)
                .required("name", StringParser.stringParser())
                .handler(ctx -> handleCreate(ctx.sender().source(), ctx.get("name"))));

        manager.command(base.literal("delete")
                .permission("velkoth.admin")
                .required("name", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleDelete(ctx.sender().source(), ctx.get("name"))));

        manager.command(base.literal("start")
                .permission("velkoth.admin")
                .required("name", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleStart(ctx.sender().source(), ctx.get("name"))));

        manager.command(base.literal("stop")
                .permission("velkoth.admin")
                .required("name", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleStop(ctx.sender().source(), ctx.get("name"))));

        manager.command(base.literal("pause")
                .permission("velkoth.admin")
                .required("name", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handlePause(ctx.sender().source(), ctx.get("name"))));

        manager.command(base.literal("resume")
                .permission("velkoth.admin")
                .required("name", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleResume(ctx.sender().source(), ctx.get("name"))));

        manager.command(base.literal("wand")
                .permission("velkoth.admin")
                .senderType(PlayerSource.class)
                .handler(ctx -> handleWand(ctx.sender().source())));

        manager.command(base.literal("list")
                .permission("velkoth.admin")
                .handler(ctx -> handleList(ctx.sender().source())));

        manager.command(base.literal("next")
                .required("name", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleNext(ctx.sender().source(), ctx.get("name"))));

        manager.command(base.literal("stats")
                .senderType(PlayerSource.class)
                .handler(ctx -> handleStats(ctx.sender().source())));

        manager.command(base.literal("reload")
                .permission("velkoth.admin")
                .handler(ctx -> handleReload(ctx.sender().source())));

        // ── Schedule ──
        var scheduleBuilder = base.literal("schedule").permission("velkoth.admin");

        manager.command(scheduleBuilder.literal("add")
                .required("day", EnumParser.enumParser(DayOfWeek.class))
                .required("time", StringParser.quotedStringParser(), (ctx, input) -> CompletableFuture.completedFuture(
                        List.of("\"00:00\"", "\"06:00\"", "\"12:00\"", "\"15:00\"", "\"18:00\"", "\"21:00\"")
                                .stream().map(Suggestion::suggestion).toList()))
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleScheduleAdd(ctx.sender().source(), ctx.get("day"), ctx.get("time"), ctx.get("arena"))));

        manager.command(scheduleBuilder.literal("list")
                .handler(ctx -> handleScheduleList(ctx.sender().source())));

        manager.command(scheduleBuilder.literal("remove")
                .required("index", IntegerParser.integerParser())
                .handler(ctx -> handleScheduleRemove(ctx.sender().source(), ctx.get("index"))));

        // ── Set ──
        var setBuilder = base.literal("set").permission("velkoth.admin");

        manager.command(setBuilder.literal("mode")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .required("mode", EnumParser.enumParser(Arena.CaptureMode.class))
                .handler(ctx -> handleSetMode(ctx.sender().source(), ctx.get("arena"), ctx.get("mode"))));

        manager.command(setBuilder.literal("time")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .required("seconds", IntegerParser.integerParser())
                .handler(ctx -> handleSetTime(ctx.sender().source(), ctx.get("arena"), ctx.get("seconds"))));

        manager.command(setBuilder.literal("score")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .required("maxScore", IntegerParser.integerParser())
                .handler(ctx -> handleSetScore(ctx.sender().source(), ctx.get("arena"), ctx.get("maxScore"))));

        manager.command(setBuilder.literal("grace")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .required("seconds", IntegerParser.integerParser())
                .handler(ctx -> handleSetGrace(ctx.sender().source(), ctx.get("arena"), ctx.get("seconds"))));

        manager.command(setBuilder.literal("hologram")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .senderType(PlayerSource.class)
                .handler(ctx -> handleSetHologramHere(ctx.sender().source(), ctx.get("arena"))));

        manager.command(setBuilder.literal("hologram")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .literal("reset")
                .handler(ctx -> handleSetHologramReset(ctx.sender().source(), ctx.get("arena"))));

        manager.command(setBuilder.literal("hologram")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .required("x", DoubleParser.doubleParser())
                .required("y", DoubleParser.doubleParser())
                .required("z", DoubleParser.doubleParser())
                .handler(ctx -> handleSetHologramCoords(ctx.sender().source(), ctx.get("arena"), ctx.get("x"), ctx.get("y"), ctx.get("z"))));

        // ── Reward ──
        var rewardBuilder = base.literal("reward").permission("velkoth.admin");

        manager.command(rewardBuilder.literal("add")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .required("reward", StringParser.greedyStringParser())
                .handler(ctx -> handleRewardAdd(ctx.sender().source(), ctx.get("arena"), ctx.get("reward"))));

        manager.command(rewardBuilder.literal("remove")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .required("index", IntegerParser.integerParser())
                .handler(ctx -> handleRewardRemove(ctx.sender().source(), ctx.get("arena"), ctx.get("index"))));

        manager.command(rewardBuilder.literal("clear")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleRewardClear(ctx.sender().source(), ctx.get("arena"))));

        manager.command(rewardBuilder.literal("list")
                .required("arena", StringParser.stringParser(), arenaSuggestions)
                .handler(ctx -> handleRewardList(ctx.sender().source(), ctx.get("arena"))));
    }

    // ── Create ──

    private void handleCreate(Player player, String nameLowercase) {
        String name = nameLowercase.toLowerCase();
        if (plugin.getArenaManager().arenaExists(name)) {
            sendPrefixed(player, plugin.getMessages().getArenaExists().replace("<arena>", name));
            return;
        }

        WandManager.Selection selection = plugin.getWandManager().getSelection(player);
        if (selection == null || !selection.isComplete()) {
            sendPrefixed(player, plugin.getMessages().getSelectionIncomplete());
            return;
        }

        var pos1 = selection.getPos1();
        var pos2 = selection.getPos2();

        CuboidRegion region = new CuboidRegion(
                pos1.getWorld(),
                pos1.getX(), pos1.getY(), pos1.getZ(),
                pos2.getX(), pos2.getY(), pos2.getZ());

        PluginConfig.CaptureDefaults defaults = plugin.getPluginConfig().getCaptureDefaults();
        Arena arena = new Arena(
                name, name, region,
                defaults.getCaptureTime(),
                Arena.CaptureMode.valueOf(defaults.getCaptureMode()),
                defaults.getGracePeriod(),
                defaults.getMaxScore());

        plugin.getArenaManager().addArena(arena);
        plugin.getWandManager().clearSelection(player);
        sendPrefixed(player, plugin.getMessages().getArenaCreated().replace("<arena>", name));
    }

    // ── Delete ──

    private void handleDelete(CommandSender sender, String nameLowercase) {
        String name = nameLowercase.toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", name));
            return;
        }

        if (arena.state() == Arena.ArenaState.ACTIVE) {
            plugin.getCaptureManager().stopArena(arena);
        }

        plugin.getArenaManager().removeArena(name);
        sendPrefixed(sender, plugin.getMessages().getArenaDeleted().replace("<arena>", name));
    }

    // ── Start ──

    private void handleStart(CommandSender sender, String nameLowercase) {
        String name = nameLowercase.toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", name));
            return;
        }
        if (arena.state() != Arena.ArenaState.IDLE) {
            sendPrefixed(sender, plugin.getMessages().getArenaAlreadyActive().replace("<arena>", name));
            return;
        }

        plugin.getCaptureManager().startArena(arena);
    }

    // ── Stop ──

    private void handleStop(CommandSender sender, String nameLowercase) {
        String name = nameLowercase.toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", name));
            return;
        }
        if (arena.state() == Arena.ArenaState.IDLE) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotActive().replace("<arena>", name));
            return;
        }

        plugin.getCaptureManager().stopArena(arena);
    }

    // ── Pause / Resume ──

    private void handlePause(CommandSender sender, String nameLowercase) {
        String name = nameLowercase.toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", name));
            return;
        }
        if (arena.state() != Arena.ArenaState.ACTIVE) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotActive().replace("<arena>", name));
            return;
        }

        plugin.getCaptureManager().pauseArena(arena);
    }

    private void handleResume(CommandSender sender, String nameLowercase) {
        String name = nameLowercase.toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", name));
            return;
        }
        if (arena.state() != Arena.ArenaState.PAUSED) {
            sendPrefixed(sender, "<red>Arena <gold>" + name + "</gold> is not paused.");
            return;
        }

        plugin.getCaptureManager().resumeArena(arena);
    }

    // ── Wand ──

    private void handleWand(Player player) {

        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.displayName(Component.text("KoTH Wand", NamedTextColor.GOLD, TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Left-click → Set Position 1", NamedTextColor.GRAY),
                Component.text("Right-click → Set Position 2", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, WandManager.WAND_TAG),
                PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);

        player.getInventory().addItem(wand);
        sendPrefixed(player, plugin.getMessages().getWandGiven());
    }

    // ── List ──

    private void handleList(CommandSender sender) {
        var arenas = plugin.getArenaManager().getArenas();
        if (arenas.isEmpty()) {
            sendPrefixed(sender, "<gray>No arenas defined.");
            return;
        }

        sendPrefixed(sender, "<gold><bold>KoTH Arenas</bold></gold> <dark_gray>(" + arenas.size() + ")</dark_gray>");
        for (Arena arena : arenas) {
            String stateColor = switch (arena.state()) {
                case ACTIVE -> "<green>";
                case PAUSED -> "<yellow>";
                case IDLE -> "<gray>";
            };
            sendPrefixed(sender, " <dark_gray>•</dark_gray> <gold>" + arena.id()
                    + "</gold> " + stateColor + "[" + arena.state().name() + "]"
                    + " <dark_gray>(" + arena.captureMode().name() + ", " + arena.captureTime() + "s)</dark_gray>");
        }
    }

    // ── Next ──

    private void handleNext(CommandSender sender, String nameLowercase) {
        String name = nameLowercase.toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", name));
            return;
        }

        Long delayMs = plugin.getSchedulerManager().getNextStartTime(name);
        var dynamicConfig = plugin.getPluginConfig().getDynamicTriggers();

        // Check if dynamic triggers apply to this arena (list is empty, contains "random", or contains this arena name)
        boolean isDynamic = dynamicConfig.isEnabled() &&
                (dynamicConfig.getArenas().isEmpty() ||
                 dynamicConfig.getArenas().stream().anyMatch(id -> id.equalsIgnoreCase("random") || id.equalsIgnoreCase(name)));

        if (isDynamic) {
            if (arena.state() == Arena.ArenaState.ACTIVE) {
                sendPrefixed(sender, "<yellow>Arena <gold>" + arena.id() + "</gold> is currently active!</yellow>");
                return;
            }

            long cooldownMs = java.util.concurrent.TimeUnit.MINUTES.toMillis(dynamicConfig.getCooldownMinutes());
            long elapsed = System.currentTimeMillis() - plugin.getDynamicTriggerManager().getLastTriggerTime();
            long remaining = cooldownMs - elapsed;

            if (remaining > 0) {
                String timeStr = formatDuration(remaining);
                sendPrefixed(sender, "<gray>Arena <gold>" + arena.id() + "</gold> is on dynamic trigger cooldown. Remaining: <yellow>" + timeStr + "</yellow>.</gray>");
            } else {
                int online = org.bukkit.Bukkit.getOnlinePlayers().size();
                int required = dynamicConfig.getMinPlayers();
                sendPrefixed(sender, "<gray>Arena <gold>" + arena.id() + "</gold> is ready to start dynamically (requires <yellow>" + required + "</yellow> online players, currently: <green>" + online + "/" + required + "</green>).</gray>");
            }

            if (delayMs != null) {
                String timeStr = formatDuration(delayMs);
                sendPrefixed(sender, plugin.getMessages().getNextScheduled()
                        .replace("<arena>", name)
                        .replace("<time>", timeStr) + " <gray>(Scheduled)</gray>");
            }
        } else {
            if (delayMs == null) {
                sendPrefixed(sender, plugin.getMessages().getNoNextScheduled().replace("<arena>", name));
            } else {
                String timeStr = formatDuration(delayMs);
                sendPrefixed(sender, plugin.getMessages().getNextScheduled()
                        .replace("<arena>", name)
                        .replace("<time>", timeStr));
            }
        }
    }

    // ── Stats ──

    private void handleStats(Player player) {

        plugin.getStatsManager().getStats(player.getUniqueId()).thenAccept(stats -> {
            sendPrefixed(player, "<gold><bold>Your KoTH Stats</bold></gold>");
            sendPrefixed(player, " <dark_gray>•</dark_gray> <gray>Total Wins:</gray> <white>" + stats.totalWins());

            plugin.getStatsManager().getDailyWins(player.getUniqueId()).thenAccept(
                    daily -> sendPrefixed(player,
                            " <dark_gray>•</dark_gray> <gray>Today:</gray> <white>" + daily));

            plugin.getStatsManager().getWeeklyWins(player.getUniqueId()).thenAccept(
                    weekly -> sendPrefixed(player,
                            " <dark_gray>•</dark_gray> <gray>This Week:</gray> <white>" + weekly));
        });
    }

    // ── Schedule ──

    private void handleScheduleAdd(CommandSender sender, DayOfWeek day, String timeStr, String arenaId) {
        try {
            String[] timeParts = timeStr.split(":");
            if (timeParts.length != 2) {
                sendPrefixed(sender, "<red>Invalid time format. Use HH:mm (e.g., 14:00)");
                return;
            }
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            if (!arenaId.equalsIgnoreCase("random") && !plugin.getArenaManager().arenaExists(arenaId)) {
                sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
                return;
            }

            dev.velmax.velkoth.scheduler.ScheduleEntry entry = new dev.velmax.velkoth.scheduler.ScheduleEntry(day, hour, minute, arenaId);
            plugin.getSchedulerManager().addScheduleEntry(entry);
            sendPrefixed(sender, "<green>Added <gold>" + arenaId + "</gold> to the schedule on <gold>" + day + "</gold> at <gold>" + timeStr + "</gold>.");
        } catch (Exception e) {
            sendPrefixed(sender, "<red>Error adding schedule entry: " + e.getMessage());
        }
    }

    private void handleScheduleList(CommandSender sender) {
        var entries = plugin.getSchedulerManager().getEntries();
        if (entries.isEmpty()) {
            sendPrefixed(sender, "<gray>No events scheduled.");
            return;
        }

        sendPrefixed(sender, "<gold><bold>KoTH Schedule</bold></gold>");
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            String timeStr = String.format("%02d:%02d", entry.hour(), entry.minute());
            sendPrefixed(sender, " <dark_gray>[" + i + "]</dark_gray> <gold>" + entry.dayOfWeek() + "</gold> at <green>" + timeStr + "</green> - <yellow>" + entry.arenaId() + "</yellow>");
        }
    }

    private void handleScheduleRemove(CommandSender sender, int index) {
        if (plugin.getSchedulerManager().removeScheduleEntry(index)) {
            sendPrefixed(sender, "<green>Removed schedule entry at index <gold>" + index + "</gold>.");
        } else {
            sendPrefixed(sender, "<red>Invalid schedule index: " + index);
        }
    }

    // ── Set ──

    private void handleSetMode(CommandSender sender, String arenaId, Arena.CaptureMode mode) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        arena.setCaptureMode(mode);
        plugin.getArenaManager().saveArenas();
        sendPrefixed(sender, plugin.getMessages().getArenaModified()
                .replace("<arena>", arena.id())
                .replace("<property>", "mode")
                .replace("<value>", mode.name()));
    }

    private void handleSetTime(CommandSender sender, String arenaId, int seconds) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        arena.setCaptureTime(seconds);
        plugin.getArenaManager().saveArenas();
        sendPrefixed(sender, plugin.getMessages().getArenaModified()
                .replace("<arena>", arena.id())
                .replace("<property>", "time")
                .replace("<value>", seconds + "s"));
    }

    private void handleSetScore(CommandSender sender, String arenaId, int maxScore) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        arena.setMaxScore(maxScore);
        plugin.getArenaManager().saveArenas();
        sendPrefixed(sender, plugin.getMessages().getArenaModified()
                .replace("<arena>", arena.id())
                .replace("<property>", "max-score")
                .replace("<value>", String.valueOf(maxScore)));
    }

    private void handleSetGrace(CommandSender sender, String arenaId, int seconds) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        arena.setGracePeriod(seconds);
        plugin.getArenaManager().saveArenas();
        sendPrefixed(sender, plugin.getMessages().getArenaModified()
                .replace("<arena>", arena.id())
                .replace("<property>", "grace-period")
                .replace("<value>", seconds + "s"));
    }

    private void handleSetHologramHere(Player player, String arenaId) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(player, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        Location loc = player.getLocation();
        arena.setHologramLocation(loc);
        plugin.getArenaManager().saveArenas();

        // Update the hologram location immediately if it is active
        plugin.getDisplayManager().getHologramManager().updateLocation(arena);

        String coordsStr = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
        sendPrefixed(player, plugin.getMessages().getArenaModified()
                .replace("<arena>", arena.id())
                .replace("<property>", "hologram-location")
                .replace("<value>", coordsStr));
    }

    private void handleSetHologramReset(CommandSender sender, String arenaId) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        arena.setHologramLocation(null);
        plugin.getArenaManager().saveArenas();

        // Update the hologram location immediately if it is active
        plugin.getDisplayManager().getHologramManager().updateLocation(arena);

        sendPrefixed(sender, plugin.getMessages().getArenaModified()
                .replace("<arena>", arena.id())
                .replace("<property>", "hologram-location")
                .replace("<value>", "default"));
    }

    private void handleSetHologramCoords(CommandSender sender, String arenaId, double x, double y, double z) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        Location loc = new Location(arena.region().getWorld(), x, y, z);
        arena.setHologramLocation(loc);
        plugin.getArenaManager().saveArenas();

        // Update the hologram location immediately if it is active
        plugin.getDisplayManager().getHologramManager().updateLocation(arena);

        String coordsStr = String.format("%.1f, %.1f, %.1f", x, y, z);
        sendPrefixed(sender, plugin.getMessages().getArenaModified()
                .replace("<arena>", arena.id())
                .replace("<property>", "hologram-location")
                .replace("<value>", coordsStr));
    }

    // ── Reward ──

    private void handleRewardAdd(CommandSender sender, String arenaId, String rewardStr) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        Reward reward = plugin.getArenaManager().parseReward(rewardStr);
        if (reward == null) {
            sendPrefixed(sender, "<red>Invalid reward format: " + rewardStr);
            return;
        }

        arena.rewards().add(reward);
        plugin.getArenaManager().saveArenas();
        sendPrefixed(sender, "<green>Added reward: <gold>" + reward.describe() + "</gold> to arena <gold>" + arenaId + "</gold>.");
    }

    private void handleRewardRemove(CommandSender sender, String arenaId, int index) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        if (index < 0 || index >= arena.rewards().size()) {
            sendPrefixed(sender, "<red>Invalid reward index: " + index);
            return;
        }

        Reward removed = arena.rewards().remove(index);
        plugin.getArenaManager().saveArenas();
        sendPrefixed(sender, "<green>Removed reward: <gold>" + removed.describe() + "</gold> from arena <gold>" + arenaId + "</gold>.");
    }

    private void handleRewardClear(CommandSender sender, String arenaId) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        arena.rewards().clear();
        plugin.getArenaManager().saveArenas();
        sendPrefixed(sender, "<green>Cleared all rewards for arena <gold>" + arenaId + "</gold>.");
    }

    private void handleRewardList(CommandSender sender, String arenaId) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            sendPrefixed(sender, plugin.getMessages().getArenaNotFound().replace("<arena>", arenaId));
            return;
        }

        if (arena.rewards().isEmpty()) {
            sendPrefixed(sender, "<gray>No rewards configured for arena <gold>" + arenaId + "</gold>.");
            return;
        }

        sendPrefixed(sender, "<gold><bold>Rewards for " + arenaId + "</bold></gold>");
        for (int i = 0; i < arena.rewards().size(); i++) {
            Reward r = arena.rewards().get(i);
            sendPrefixed(sender, " <dark_gray>[" + i + "]</dark_gray> <yellow>" + r.describe() + "</yellow>");
        }
    }

    // ── Reload ──

    private void handleReload(CommandSender sender) {
        plugin.reloadAllConfigs();
        sendPrefixed(sender, plugin.getMessages().getReloaded());
    }

    // ── Help ──

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sendPrefixed(sender, "<gold><bold>VelKoth Commands</bold></gold>");
        sendPrefixed(sender, " <gold>/koth create <name></gold> <dark_gray>-</dark_gray> <gray>Create a new arena");
        sendPrefixed(sender, " <gold>/koth delete <name></gold> <dark_gray>-</dark_gray> <gray>Delete an arena");
        sendPrefixed(sender, " <gold>/koth start <name></gold> <dark_gray>-</dark_gray> <gray>Start a KoTH event");
        sendPrefixed(sender, " <gold>/koth stop <name></gold> <dark_gray>-</dark_gray> <gray>Stop a KoTH event");
        sendPrefixed(sender, " <gold>/koth pause <name></gold> <dark_gray>-</dark_gray> <gray>Pause a KoTH event");
        sendPrefixed(sender, " <gold>/koth resume <name></gold> <dark_gray>-</dark_gray> <gray>Resume a paused event");
        sendPrefixed(sender, " <gold>/koth wand</gold> <dark_gray>-</dark_gray> <gray>Get the selection wand");
        sendPrefixed(sender, " <gold>/koth list</gold> <dark_gray>-</dark_gray> <gray>List all arenas");
        sendPrefixed(sender,
                " <gold>/koth next <name></gold> <dark_gray>-</dark_gray> <gray>Check when an arena starts next");
        sendPrefixed(sender, " <gold>/koth stats</gold> <dark_gray>-</dark_gray> <gray>View your stats");
        sendPrefixed(sender, " <gold>/koth reload</gold> <dark_gray>-</dark_gray> <gray>Reload configuration");
        sendPrefixed(sender, "");
        sendPrefixed(sender, " <gold>/koth schedule remove <index></gold>");
        sendPrefixed(sender, "");
        sendPrefixed(sender, "<gold><bold>Edit Commands</bold></gold>");
        sendPrefixed(sender, " <gold>/koth set mode <arena> <CAPTURE|SCORE></gold>");
        sendPrefixed(sender, " <gold>/koth set time <arena> <seconds></gold>");
        sendPrefixed(sender, " <gold>/koth set score <arena> <maxScore></gold>");
        sendPrefixed(sender, " <gold>/koth set grace <arena> <seconds></gold>");
        sendPrefixed(sender, " <gold>/koth set hologram <arena></gold>");
        sendPrefixed(sender, " <gold>/koth set hologram <arena> <x> <y> <z></gold>");
        sendPrefixed(sender, " <gold>/koth set hologram <arena> reset</gold>");
        sendPrefixed(sender, "");
        sendPrefixed(sender, "<gold><bold>Reward Commands</bold></gold>");
        sendPrefixed(sender, " <gold>/koth reward add <arena> <reward></gold>");
        sendPrefixed(sender, " <gold>/koth reward remove <arena> <index></gold>");
        sendPrefixed(sender, " <gold>/koth reward list <arena></gold>");
        sendPrefixed(sender, " <gold>/koth reward clear <arena></gold>");
    }

    // ── Helpers ──

    private void sendPrefixed(CommandSender sender, String message) {
        var prefix = miniMessage.deserialize(plugin.getMessages().getPrefix());
        sender.sendMessage(prefix.append(miniMessage.deserialize(message)));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = ((seconds % 86400) % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0)
            sb.append(minutes).append("m ");
        if (sb.isEmpty())
            sb.append("< 1m");
        return sb.toString().trim();
    }
}
