package dev.velmax.velkoth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.util.*;

/**
 * Stores all arena definitions in arenas.yml.
 */
@Header("VelKoth Arena Definitions")
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class ArenaConfig extends OkaeriConfig {

    @Comment("Map of arena id -> arena settings")
    private Map<String, ArenaEntry> arenas = new LinkedHashMap<>();

    public Map<String, ArenaEntry> getArenas() {
        return arenas;
    }

    public void setArenas(Map<String, ArenaEntry> arenas) {
        this.arenas = arenas;
    }

    public static class ArenaEntry extends OkaeriConfig {
        private String displayName = "KoTH Arena";
        private String world = "world";

        @Comment({
                "Region type for the hill:",
                "CUBOID: A box defined by two corner points (pos1 and pos2).",
                "CYLINDER: A circular area defined by a center, radius, and height (minY to maxY)."
        })
        private String regionType = "CUBOID";

        @Comment("For CUBOID: The first corner point of the capture zone [x, y, z]")
        private List<Double> pos1 = List.of(100.0, 64.0, 100.0);
        @Comment("For CUBOID: The second (opposite) corner point of the capture zone [x, y, z]")
        private List<Double> pos2 = List.of(110.0, 70.0, 110.0);

        @Comment({
                "For CYLINDER: The settings for circular capture zones.",
                "centerX/centerZ: The midpoint of the circle.",
                "radius: How far from the center the zone extends.",
                "minY/maxY: The bottom and top height limits of the zone."
        })
        private double centerX = 0;
        private double centerZ = 0;
        private double radius = 10;
        private double minY = 60;
        private double maxY = 70;

        @Comment("Time in seconds to capture the hill")
        private int captureTime = 120;

        @Comment("Capture mode: CAPTURE or SCORE")
        private String captureMode = "CAPTURE";

        @Comment("Max score for SCORE mode")
        private int maxScore = 300;

        @Comment("Grace period in seconds")
        private int gracePeriod = 5;

        @Comment({
                "Reward commands executed when a player/team wins the KoTH.",
                "Use %player% to target the winning player.",
                "Example commands: 'give %player% diamond 3', 'eco give %player% 500'"
        })
        private List<String> rewards = List.of(
                "give %player% diamond 3",
                "eco give %player% 500");

        @Comment("Custom hologram location [x, y, z]")
        private List<Double> hologramLocation = null;

        // Getters & Setters
        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getWorld() {
            return world;
        }

        public void setWorld(String world) {
            this.world = world;
        }

        public String getRegionType() {
            return regionType;
        }

        public void setRegionType(String regionType) {
            this.regionType = regionType;
        }

        public List<Double> getPos1() {
            return pos1;
        }

        public void setPos1(List<Double> pos1) {
            this.pos1 = pos1;
        }

        public List<Double> getPos2() {
            return pos2;
        }

        public void setPos2(List<Double> pos2) {
            this.pos2 = pos2;
        }

        public double getCenterX() {
            return centerX;
        }

        public void setCenterX(double centerX) {
            this.centerX = centerX;
        }

        public double getCenterZ() {
            return centerZ;
        }

        public void setCenterZ(double centerZ) {
            this.centerZ = centerZ;
        }

        public double getRadius() {
            return radius;
        }

        public void setRadius(double radius) {
            this.radius = radius;
        }

        public double getMinY() {
            return minY;
        }

        public void setMinY(double minY) {
            this.minY = minY;
        }

        public double getMaxY() {
            return maxY;
        }

        public void setMaxY(double maxY) {
            this.maxY = maxY;
        }

        public int getCaptureTime() {
            return captureTime;
        }

        public void setCaptureTime(int captureTime) {
            this.captureTime = captureTime;
        }

        public String getCaptureMode() {
            return captureMode;
        }

        public void setCaptureMode(String captureMode) {
            this.captureMode = captureMode;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(int maxScore) {
            this.maxScore = maxScore;
        }

        public int getGracePeriod() {
            return gracePeriod;
        }

        public void setGracePeriod(int gracePeriod) {
            this.gracePeriod = gracePeriod;
        }

        public List<String> getRewards() {
            return rewards;
        }

        public void setRewards(List<String> rewards) {
            this.rewards = rewards;
        }

        public List<Double> getHologramLocation() {
            return hologramLocation;
        }

        public void setHologramLocation(List<Double> hologramLocation) {
            this.hologramLocation = hologramLocation;
        }
    }
}
