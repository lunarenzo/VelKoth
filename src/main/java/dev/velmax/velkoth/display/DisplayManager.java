package dev.velmax.velkoth.display;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.capture.CaptureSession;
import dev.velmax.velkoth.config.MessagesConfig;
import dev.velmax.velkoth.config.PluginConfig;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all visual feedback for KoTH events:
 * BossBar, ActionBar, Titles, Sounds, and Particles.
 */
public final class DisplayManager {

    private final VelKothPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, BossBar> bossBars = new ConcurrentHashMap<>();

    private final ScoreboardManager scoreboardManager;
    private final HologramManager hologramManager;

    // Cache parsed components to achieve 0% MiniMessage parser usage on hot path
    private final Map<String, Component> miniMessageCache = new java.util.LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
            return size() > 256;
        }
    };

    private record BossBarState(Component name, float progress) {}
    private final Map<String, BossBarState> lastBossBarStates = new ConcurrentHashMap<>();
    private Component cachedPrefix;

    public DisplayManager(VelKothPlugin plugin) {
        this.plugin = plugin;
        this.scoreboardManager = new ScoreboardManager(plugin);
        this.hologramManager = new HologramManager(plugin);
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    private PluginConfig.DisplaySettings display() {
        return plugin.getPluginConfig().getDisplay();
    }

    private MessagesConfig messages() {
        return plugin.getMessages();
    }

    // ──────────────────────────── BossBar ────────────────────────────

    /**
     * Create a BossBar for an arena event and show it to all online players.
     */
    public void createBossBar(Arena arena) {
        if (!display().isBossBarEnabled())
            return;

        BossBar bar = BossBar.bossBar(
                parsePlaceholders(messages().getBossBarIdle(), arena, null, null),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS);
        bossBars.put(arena.id(), bar);
        Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(bar));
    }

    /**
     * Update the BossBar with current capture state.
     */
    public void updateBossBar(Arena arena, CaptureSession session) {
        if (!display().isBossBarEnabled())
            return;
        BossBar bar = bossBars.get(arena.id());
        if (bar == null)
            return;

        Component name;
        if (session.isContested()) {
            name = parsePlaceholders(messages().getBossBarContested(), arena, session, null);
            bar.color(BossBar.Color.YELLOW);
        } else if (session.capturingPlayer() != null) {
            Player capturer = Bukkit.getPlayer(session.capturingPlayer());
            name = parsePlaceholders(messages().getBossBarCapturing(), arena, session, capturer);
            bar.color(BossBar.Color.GREEN);
        } else {
            name = parsePlaceholders(messages().getBossBarIdle(), arena, session, null);
            bar.color(BossBar.Color.YELLOW);
        }

        // Progress based on capture time
        float progress = Math.clamp((float) session.elapsedSeconds() / arena.captureTime(), 0f, 1f);

        // Check cache to avoid sending redundant packets
        BossBarState currentState = new BossBarState(name, progress);
        BossBarState previousState = lastBossBarStates.get(arena.id());
        if (previousState != null && previousState.equals(currentState)) {
            return;
        }
        lastBossBarStates.put(arena.id(), currentState);

        bar.name(name);
        bar.progress(progress);
    }

    /**
     * Remove the BossBar for an arena.
     */
    public void removeBossBar(Arena arena) {
        lastBossBarStates.remove(arena.id());
        BossBar bar = bossBars.remove(arena.id());
        if (bar != null) {
            Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bar));
        }
    }

    // ──────────────────────────── ActionBar ────────────────────────────

    public void sendActionBar(Player player, Arena arena, CaptureSession session) {
        if (!display().isActionBarEnabled())
            return;
        Component msg = parsePlaceholders(messages().getActionBarCapturing(), arena, session, player);
        player.sendActionBar(msg);
    }

    // ──────────────────────────── Titles ────────────────────────────

    /**
     * Show event start title to all online players.
     */
    public void showStartTitle(Arena arena) {
        if (!display().isTitlesEnabled())
            return;
        Title title = Title.title(
                parsePlaceholders(messages().getTitleStart(), arena, null, null),
                parsePlaceholders(messages().getSubtitleStart(), arena, null, null),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500)));
        Bukkit.getOnlinePlayers().forEach(p -> p.showTitle(title));
    }

    /**
     * Show win title to the winner.
     */
    public void showWinTitle(Player winner, Arena arena) {
        if (!display().isTitlesEnabled())
            return;
        Title title = Title.title(
                parsePlaceholders(messages().getTitleWin(), arena, null, winner),
                parsePlaceholders(messages().getSubtitleWin(), arena, null, winner),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500)));
        winner.showTitle(title);
    }

    /**
     * Show contested title to players on the hill.
     */
    public void showContestedTitle(Player player, Arena arena) {
        if (!display().isTitlesEnabled())
            return;
        Title title = Title.title(
                parsePlaceholders(messages().getTitleContested(), arena, null, null),
                parsePlaceholders(messages().getSubtitleContested(), arena, null, null),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(300)));
        player.showTitle(title);
    }

    // ──────────────────────────── Sounds ────────────────────────────

    public void playCaptureStartSound(Player player) {
        if (!display().isSoundsEnabled())
            return;
        player.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,
                Sound.Source.MASTER, 1f, 1.5f));
    }

    public void playContestedSound(Player player) {
        if (!display().isSoundsEnabled())
            return;
        player.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS,
                Sound.Source.MASTER, 1f, 0.8f));
    }

    public void playWinSound() {
        if (!display().isSoundsEnabled())
            return;
        Sound sound = Sound.sound(org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                Sound.Source.MASTER, 1f, 1f);
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(sound));
    }

    public void playTickSound(Player player) {
        if (!display().isSoundsEnabled())
            return;
        player.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT,
                Sound.Source.MASTER, 0.3f, 1.2f));
    }

    // ──────────────────────────── Particles ────────────────────────────

    /**
     * Spawn particles at the hill center (called periodically).
     */
    public void spawnHillParticles(Arena arena) {
        if (!display().isParticlesEnabled())
            return;
        var center = arena.region().getCenter();
        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0),
                5, 1.5, 1.5, 1.5, 0.02);
    }

    // ──────────────────────────── Chat Messages ────────────────────────────

    /**
     * Broadcast a prefixed chat message to all players.
     */
    public void broadcast(String messageTemplate, Arena arena, @Nullable Player player) {
        Component prefix = getPrefix();
        Component msg = parsePlaceholders(messageTemplate, arena, null, player);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(prefix.append(msg)));
    }

    /**
     * Send a prefixed message to a single player.
     */
    public void sendMessage(Player target, String messageTemplate, Arena arena) {
        Component prefix = getPrefix();
        Component msg = parsePlaceholders(messageTemplate, arena, null, target);
        target.sendMessage(prefix.append(msg));
    }

    /**
     * Send a raw prefixed message (no arena context).
     */
    public void sendPrefixed(Player target, String rawMessage) {
        Component prefix = getPrefix();
        Component msg = parseMiniMessage(rawMessage);
        target.sendMessage(prefix.append(msg));
    }

    // ──────────────────────────── Cleanup ────────────────────────────

    public void cleanup() {
        bossBars.values().forEach(bar -> Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bar)));
        bossBars.clear();
        hologramManager.removeAll();
    }

    // ──────────────────────────── Helpers ────────────────────────────

    public synchronized Component parseMiniMessage(String text) {
        return miniMessageCache.computeIfAbsent(text, miniMessage::deserialize);
    }

    public Component getPrefix() {
        if (cachedPrefix == null) {
            cachedPrefix = parseMiniMessage(messages().getPrefix());
        }
        return cachedPrefix;
    }

    public void reload() {
        miniMessageCache.clear();
        cachedPrefix = null;
        lastBossBarStates.clear();
        scoreboardManager.reload();
        hologramManager.reload();
    }

    private Component parsePlaceholders(String template, Arena arena,
            @Nullable CaptureSession session,
            @Nullable Player player) {
        String arenaName = arena != null ? arena.displayName() : "Unknown";
        String playerName = player != null ? player.getName() : "None";
        String time = "0";
        if (session != null && arena != null) {
            int remaining = arena.captureTime() - session.elapsedSeconds();
            time = formatTime(Math.max(0, remaining));
        }

        String escapedArena = miniMessage.escapeTags(arenaName);
        String escapedPlayer = miniMessage.escapeTags(playerName);
        String escapedTime = miniMessage.escapeTags(time);

        String resolved = template
                .replace("<arena>", escapedArena)
                .replace("<player>", escapedPlayer)
                .replace("<time>", escapedTime);

        return parseMiniMessage(resolved);
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : seconds);
    }
}
