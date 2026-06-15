package dev.velmax.velkoth;

import dev.velmax.velkoth.capture.CaptureManager;
import dev.velmax.velkoth.command.KothCommand;
import dev.velmax.velkoth.config.ArenaConfig;
import dev.velmax.velkoth.config.MessagesConfig;
import dev.velmax.velkoth.config.PluginConfig;
import dev.velmax.velkoth.display.DisplayManager;
import dev.velmax.velkoth.hook.KothPlaceholderExpansion;
import dev.velmax.velkoth.hook.VaultHook;
import dev.velmax.velkoth.listener.PlayerListener;
import dev.velmax.velkoth.listener.WandListener;
import dev.velmax.velkoth.manager.ArenaManager;
import dev.velmax.velkoth.manager.RewardManager;
import dev.velmax.velkoth.manager.StatsManager;
import dev.velmax.velkoth.manager.WandManager;
import dev.velmax.velkoth.manager.DynamicTriggerManager;
import dev.velmax.velkoth.team.TeamManager;
import dev.velmax.velkoth.scheduler.SchedulerManager;
import dev.velmax.velkoth.storage.DatabaseManager;
import dev.velmax.velkoth.display.TemplateCache;
import net.kyori.adventure.text.Component;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * VelKoth — A modern, high-performance King of the Hill plugin.
 *
 * @author VelMax Studios
 */
public final class VelKothPlugin extends JavaPlugin {

    private static VelKothPlugin instance;

    // Configs
    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private ArenaConfig arenaConfig;

    // Managers
    private DatabaseManager databaseManager;
    private ArenaManager arenaManager;
    private CaptureManager captureManager;
    private DisplayManager displayManager;
    private RewardManager rewardManager;
    private StatsManager statsManager;
    private WandManager wandManager;
    private SchedulerManager schedulerManager;
    private TeamManager teamManager;
    private DynamicTriggerManager dynamicTriggerManager;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        // 1. Configs
        loadConfigs();

        // 2. Database
        databaseManager = new DatabaseManager(this, pluginConfig.getDatabase());

        // 3. Managers
        arenaManager = new ArenaManager(this);
        captureManager = new CaptureManager(this);
        displayManager = new DisplayManager(this);
        rewardManager = new RewardManager(this);
        statsManager = new StatsManager(databaseManager);
        wandManager = new WandManager();
        schedulerManager = new SchedulerManager(this);
        teamManager = new TeamManager(this);
        dynamicTriggerManager = new DynamicTriggerManager(this);
        teamManager.loadHook();

        // 4. Load arenas from config
        arenaManager.loadArenas();

        // 5. Load schedule
        schedulerManager.loadSchedule();
        dynamicTriggerManager.start();

        // 5.1 Test Template Cache resolving
        try {
            TemplateCache testCache = new TemplateCache();
            Component resolved = testCache.resolve("<gradient:#ff6b6b:#ffa500><bold><arena> KoTH</bold></gradient>", "TEST_ARENA_NAME", null, null, null);
            getLogger().info("[TemplateCache Test] Resolved: " + net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(resolved));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "TemplateCache startup test failed", e);
        }

        // 6. Register commands
        new KothCommand(this);

        // 7. Register listeners
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new WandListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(dynamicTriggerManager, this);

        // 8. Hooks
        VaultHook.setup(getLogger());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KothPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        // 9. Initialize bStats
        int pluginId = 29992;
        new Metrics(this, pluginId);

        long elapsed = System.currentTimeMillis() - start;
        getLogger()
                .info("VelKoth enabled in " + elapsed + "ms — " + arenaManager.getArenas().size() + " arenas loaded.");
    }

    @Override
    public void onDisable() {
        // Graceful shutdown in reverse order
        if (captureManager != null)
            captureManager.shutdown();
        if (displayManager != null)
            displayManager.cleanup();
        if (schedulerManager != null)
            schedulerManager.shutdown();
        if (dynamicTriggerManager != null)
            dynamicTriggerManager.stop();
        if (arenaManager != null)
            arenaManager.saveArenas();
        if (databaseManager != null)
            databaseManager.shutdown();

        instance = null;
        getLogger().info("VelKoth disabled.");
    }

    // ── Config Loading ──

    private void loadConfigs() {
        pluginConfig = ConfigManager.create(PluginConfig.class, cfg -> {
            cfg.withConfigurer(new YamlSnakeYamlConfigurer());
            cfg.withBindFile(new File(getDataFolder(), "config.yml"));
            cfg.withRemoveOrphans(false);
            cfg.saveDefaults();
            cfg.load(true);
        });

        messagesConfig = ConfigManager.create(MessagesConfig.class, cfg -> {
            cfg.withConfigurer(new YamlSnakeYamlConfigurer());
            cfg.withBindFile(new File(getDataFolder(), "messages.yml"));
            cfg.withRemoveOrphans(false);
            cfg.saveDefaults();
            cfg.load(true);
        });

        arenaConfig = ConfigManager.create(ArenaConfig.class, cfg -> {
            cfg.withConfigurer(new YamlSnakeYamlConfigurer());
            cfg.withBindFile(new File(getDataFolder(), "arenas.yml"));
            cfg.withRemoveOrphans(false);
            cfg.saveDefaults();
            cfg.load(true);
        });
    }

    public void reloadAllConfigs() {
        if (captureManager != null) {
            captureManager.shutdown();
        }
        pluginConfig.load(true);
        messagesConfig.load(true);
        arenaConfig.load(true);
        arenaManager.loadArenas();
        schedulerManager.loadSchedule();
        if (displayManager != null) {
            displayManager.reload();
        }
        getLogger().info("All configurations reloaded.");
    }


    // ── Accessors ──

    public static VelKothPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public MessagesConfig getMessages() {
        return messagesConfig;
    }

    public ArenaConfig getArenaConfig() {
        return arenaConfig;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public CaptureManager getCaptureManager() {
        return captureManager;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public WandManager getWandManager() {
        return wandManager;
    }

    public SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public DynamicTriggerManager getDynamicTriggerManager() {
        return dynamicTriggerManager;
    }
}
