package dev.velmax.velkoth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Main plugin configuration loaded from config.yml.
 */
@Header("VelKoth Configuration")
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class PluginConfig extends OkaeriConfig {

    @Comment({
            "Database configuration",
            "SQLITE: Local file storage (standard for single servers)",
            "MYSQL: Recommended for database synchronization across multiple bungee/velocity servers."
    })
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Default capture settings for new arenas")
    private CaptureDefaults captureDefaults = new CaptureDefaults();

    @Comment("Display settings")
    private DisplaySettings display = new DisplaySettings();

    @Comment({
            "Timezone for scheduled events.",
            "Use a region-based ID (e.g., 'Europe/Paris' or 'America/New_York') for automatic Daylight Saving Time (DST) adjustment.",
            "Use a fixed offset (e.g., 'GMT+2' or '+02:00') if you want the schedule to stay the same all year round.",
            "Examples: UTC, EST, America/New_York, Europe/London, Asia/Kolkata, GMT+2"
    })
    private String scheduleTimezone = "UTC";

    @Comment({ "Scheduled events", "Format: day:HH:mm:arenaId or a cron expression" })
    private List<String> schedule = List.of(
            "SATURDAY:14:00:spawn_koth",
            "SUNDAY:18:00:random");

    @Comment("Minimum player count threshold for scheduled events to start. Set to 0 to disable.")
    private int scheduleMinPlayers = 0;

    @Comment("Dynamic player-count event settings")
    private DynamicTriggerConfig dynamicTriggers = new DynamicTriggerConfig();

    public DatabaseConfig getDatabase() {
        return database;
    }

    public CaptureDefaults getCaptureDefaults() {
        return captureDefaults;
    }

    public DisplaySettings getDisplay() {
        return display;
    }

    public List<String> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<String> schedule) {
        this.schedule = schedule;
    }

    public String getScheduleTimezone() {
        return scheduleTimezone;
    }

    public int getScheduleMinPlayers() {
        return scheduleMinPlayers;
    }

    public DynamicTriggerConfig getDynamicTriggers() {
        return dynamicTriggers;
    }

    public static class DatabaseConfig extends OkaeriConfig {
        @Comment("Database type: SQLITE or MYSQL")
        private String type = "SQLITE";

        @Comment("MySQL connection details (ignored for SQLite)")
        private String host = "localhost";
        private int port = 3306;
        private String database = "velkoth";
        private String username = "root";
        private String password = "";

        @Comment("Connection pool size")
        private int poolSize = 5;

        public String getType() {
            return type;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getDatabase() {
            return database;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public int getPoolSize() {
            return poolSize;
        }
    }

    public static class CaptureDefaults extends OkaeriConfig {
        @Comment("Default capture time in seconds")
        private int captureTime = 120;

        @Comment("Grace period in seconds before capture resets when player leaves")
        private int gracePeriod = 5;

        @Comment({
                "Default capture mode for the hill:",
                "CAPTURE: Traditional mode where players must stay on the hill until it turns their color.",
                "SCORE: Points-based mode where teams earn score over time while holding the hill."
        })
        private String captureMode = "CAPTURE";

        @Comment("Default max score for SCORE mode")
        private int maxScore = 300;

        @Comment("Whether the hill becomes contested when multiple players are on it")
        private boolean contestEnabled = true;

        @Comment("Score mode: base points awarded per second of holding the hill")
        private int scoreBasePoints = 1;

        @Comment("Score mode: enable holding streak multiplier")
        private boolean scoreStreakEnabled = true;

        @Comment("Score mode: consecutive seconds needed to increase multiplier level")
        private int scoreStreakInterval = 10;

        @Comment("Score mode: maximum multiplier level allowed")
        private int scoreMaxMultiplier = 5;

        @Comment("Score mode: bonus points for killing an enemy inside the hill")
        private int scoreKillBonus = 5;

        @Comment("Score mode: team coop multiplier bonus per additional teammate on the hill")
        private double scoreTeamCoopBonus = 0.2;

        @Comment("Score mode: maximum team coop multiplier")
        private double scoreMaxCoopMultiplier = 2.0;

        public int getCaptureTime() {
            return captureTime;
        }

        public int getGracePeriod() {
            return gracePeriod;
        }

        public String getCaptureMode() {
            return captureMode;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public boolean isContestEnabled() {
            return contestEnabled;
        }

        public int getScoreBasePoints() {
            return scoreBasePoints;
        }

        public boolean isScoreStreakEnabled() {
            return scoreStreakEnabled;
        }

        public int getScoreStreakInterval() {
            return scoreStreakInterval;
        }

        public int getScoreMaxMultiplier() {
            return scoreMaxMultiplier;
        }

        public int getScoreKillBonus() {
            return scoreKillBonus;
        }

        public double getScoreTeamCoopBonus() {
            return scoreTeamCoopBonus;
        }

        public double getScoreMaxCoopMultiplier() {
            return scoreMaxCoopMultiplier;
        }
    }

    public static class DisplaySettings extends OkaeriConfig {
        @Comment("Enable BossBar showing capture progress")
        private boolean bossBarEnabled = true;

        @Comment("Enable ActionBar timer")
        private boolean actionBarEnabled = true;

        @Comment("Enable title notifications")
        private boolean titlesEnabled = true;

        @Comment("Enable capture sounds")
        private boolean soundsEnabled = true;

        @Comment("Enable particle effects on the hill")
        private boolean particlesEnabled = true;

        @Comment("Enable per-player FastBoard scoreboards showing active capture status")
        private boolean scoreboardEnabled = true;

        @Comment({
                "Should VelKoth show an 'Idle' scoreboard when there are no active events?",
                "Set this to false if you are using another scoreboard plugin (TAB, SimpleScore) and only want VelKoth's scoreboard during events."
        })
        private boolean showIdleScoreboard = false;

        @Comment({
                "If true, VelKoth will attempt to pause other scoreboard plugins (like TAB or SimpleScore) while an event is active.",
                "This ensures there are no flickering issues when VelKoth takes over the player's scoreboard."
        })
        private boolean overrideOtherScoreboards = true;

        @Comment({
                "Should VelKoth only show the scoreboard to players in the same world as the active KOTH event?",
                "This keeps the HUD player-friendly by not showing it to players in other worlds."
        })
        private boolean scoreboardOnlyInArenaWorld = true;

        @Comment({
                "Proximity radius in blocks around the KOTH hill center for showing the scoreboard.",
                "If set to a positive value (e.g. 50.0), players must be within this distance from the hill to see the HUD.",
                "Set to -1.0 to disable proximity radius checks."
        })
        private double scoreboardProximityRadius = -1.0;

        @Comment("Enable floating TextDisplay holograms above active hills")
        private boolean hologramEnabled = true;

        @Comment("Y-axis offset for the hologram above the hill center")
        private double hologramYOffset = 3.0;

        public boolean isBossBarEnabled() {
            return bossBarEnabled;
        }

        public boolean isActionBarEnabled() {
            return actionBarEnabled;
        }

        public boolean isTitlesEnabled() {
            return titlesEnabled;
        }

        public boolean isSoundsEnabled() {
            return soundsEnabled;
        }

        public boolean isParticlesEnabled() {
            return particlesEnabled;
        }

        public boolean isScoreboardEnabled() {
            return scoreboardEnabled;
        }

        public boolean isShowIdleScoreboard() {
            return showIdleScoreboard;
        }

        public boolean isOverrideOtherScoreboards() {
            return overrideOtherScoreboards;
        }

        public boolean isScoreboardOnlyInArenaWorld() {
            return scoreboardOnlyInArenaWorld;
        }

        public double getScoreboardProximityRadius() {
            return scoreboardProximityRadius;
        }

        public boolean isHologramEnabled() {
            return hologramEnabled;
        }

        public double getHologramYOffset() {
            return hologramYOffset;
        }
    }

    public static class DynamicTriggerConfig extends OkaeriConfig {
        @Comment("Enable dynamic player-count event triggers")
        private boolean enabled = false;

        @Comment("Minimum player count to trigger a KOTH event")
        private int minPlayers = 15;

        @Comment("Minimum cooldown in minutes between dynamically triggered events")
        private int cooldownMinutes = 60;

        @Comment("List of arena IDs to randomly select from, or ['random'] to select from all idle arenas")
        private List<String> arenas = List.of("random");

        public boolean isEnabled() {
            return enabled;
        }

        public int getMinPlayers() {
            return minPlayers;
        }

        public int getCooldownMinutes() {
            return cooldownMinutes;
        }

        public List<String> getArenas() {
            return arenas;
        }
    }
}
