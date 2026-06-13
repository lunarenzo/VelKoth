package dev.velmax.velkoth.scheduler;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages automatic KoTH event scheduling.
 * Uses a single-threaded ScheduledExecutorService for efficiency.
 */
public final class SchedulerManager {

    private final VelKothPlugin plugin;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "VelKoth-Scheduler");
        t.setDaemon(true);
        return t;
    });
    private final List<ScheduleEntry> entries = new ArrayList<>();
    private final List<ScheduledFuture<?>> tasks = new CopyOnWriteArrayList<>();

    public SchedulerManager(VelKothPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load schedule entries from config and schedule them.
     */
    public void loadSchedule() {
        clearSchedule();
        for (String line : plugin.getPluginConfig().getSchedule()) {
            try {
                ScheduleEntry entry = ScheduleEntry.parse(line);
                entries.add(entry);
                scheduleEntry(entry);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Invalid schedule entry: " + line, e);
            }
        }
        plugin.getLogger().info("Loaded " + entries.size() + " scheduled events.");
    }

    private void clearSchedule() {
        tasks.forEach(task -> task.cancel(false));
        tasks.clear();
        entries.clear();
    }

    private void scheduleEntry(ScheduleEntry entry) {
        long delayMs = calculateDelayMs(entry);
        long weekMs = TimeUnit.DAYS.toMillis(7);

        ScheduledFuture<?> task = executor.scheduleAtFixedRate(() -> {
            // Must run on main thread
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                String arenaId = entry.arenaId();

                int minPlayers = plugin.getPluginConfig().getScheduleMinPlayers();
                if (minPlayers > 0 && Bukkit.getOnlinePlayers().size() < minPlayers) {
                    plugin.getLogger().info("Scheduled event for '" + arenaId + "' canceled: online players (" +
                            Bukkit.getOnlinePlayers().size() + ") < threshold (" + minPlayers + ").");
                    return;
                }

                if ("random".equalsIgnoreCase(arenaId)) {
                    startRandomArena();
                } else {
                    Arena arena = plugin.getArenaManager().getArena(arenaId);
                    if (arena != null && arena.state() == Arena.ArenaState.IDLE) {
                        plugin.getCaptureManager().startArena(arena);
                        plugin.getLogger().info("Scheduled event started: " + arenaId);
                    }
                }
            });
        }, delayMs, weekMs, TimeUnit.MILLISECONDS);
        tasks.add(task);
    }

    private void startRandomArena() {
        var arenas = plugin.getArenaManager().getArenas().stream()
                .filter(a -> a.state() == Arena.ArenaState.IDLE)
                .toList();
        if (!arenas.isEmpty()) {
            Arena random = arenas.get(ThreadLocalRandom.current().nextInt(arenas.size()));
            plugin.getCaptureManager().startArena(random);
            plugin.getLogger().info("Scheduled random event started: " + random.id());
        }
    }

    public long calculateDelayMs(ScheduleEntry entry) {
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(plugin.getPluginConfig().getScheduleTimezone(), ZoneId.SHORT_IDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid timezone in config: " + plugin.getPluginConfig().getScheduleTimezone()
                    + ", falling back to system default.");
            zoneId = ZoneId.systemDefault();
        }

        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDateTime target = now.with(TemporalAdjusters.nextOrSame(entry.dayOfWeek()))
                .withHour(entry.hour())
                .withMinute(entry.minute())
                .withSecond(0)
                .withNano(0);

        if (!target.isAfter(now)) {
            target = target.plusWeeks(1);
        }

        long targetMs = target.atZone(zoneId).toInstant().toEpochMilli();
        return targetMs - System.currentTimeMillis();
    }

    public void shutdown() {
        executor.shutdownNow();
        tasks.clear();
    }

    public void addScheduleEntry(ScheduleEntry entry) {
        List<String> current = new ArrayList<>(plugin.getPluginConfig().getSchedule());
        String line = entry.dayOfWeek().name() + ":" + String.format("%02d:%02d:", entry.hour(), entry.minute()) + entry.arenaId();
        current.add(line);
        plugin.getPluginConfig().setSchedule(current);
        plugin.getPluginConfig().save();
        loadSchedule();
    }

    public boolean removeScheduleEntry(int index) {
        List<String> current = new ArrayList<>(plugin.getPluginConfig().getSchedule());
        if (index < 0 || index >= current.size()) return false;
        current.remove(index);
        plugin.getPluginConfig().setSchedule(current);
        plugin.getPluginConfig().save();
        loadSchedule();
        return true;
    }

    public List<ScheduleEntry> getEntries() {
        return List.copyOf(entries);
    }

    public Long getNextStartTime(String arenaId) {
        ScheduleEntry entry = getNextEntry(arenaId);
        return entry == null ? null : calculateDelayMs(entry);
    }

    /**
     * @return The next scheduled entry for any arena, or null if none.
     */
    public @Nullable ScheduleEntry getNextEntry() {
        ScheduleEntry next = null;
        long minDelay = -1;
        for (ScheduleEntry entry : entries) {
            long delay = calculateDelayMs(entry);
            if (minDelay == -1 || delay < minDelay) {
                minDelay = delay;
                next = entry;
            }
        }
        return next;
    }

    /**
     * @return The next scheduled entry for a specific arena, or null if none.
     */
    public @Nullable ScheduleEntry getNextEntry(String arenaId) {
        ScheduleEntry next = null;
        long minDelay = -1;
        for (ScheduleEntry entry : entries) {
            if (entry.arenaId().equalsIgnoreCase(arenaId)) {
                long delay = calculateDelayMs(entry);
                if (minDelay == -1 || delay < minDelay) {
                    minDelay = delay;
                    next = entry;
                }
            }
        }
        return next;
    }
}
