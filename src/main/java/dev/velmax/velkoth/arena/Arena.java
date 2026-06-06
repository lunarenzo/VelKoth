package dev.velmax.velkoth.arena;

import dev.velmax.velkoth.arena.region.Region;
import dev.velmax.velkoth.reward.Reward;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a King of the Hill arena with all its configuration and state.
 */
public final class Arena {

    private final String id;
    private String displayName;
    private Region region;
    private int captureTime;
    private CaptureMode captureMode;
    private ArenaState state;
    private List<Reward> rewards;
    private int graceperiod;
    private int maxScore;
    private Location hologramLocation;

    public Arena(String id, String displayName, Region region, int captureTime,
            CaptureMode captureMode, int gracePeriod, int maxScore) {
        this.id = id;
        this.displayName = displayName;
        this.region = region;
        this.captureTime = captureTime;
        this.captureMode = captureMode;
        this.state = ArenaState.IDLE;
        this.rewards = new ArrayList<>();
        this.graceperiod = gracePeriod;
        this.maxScore = maxScore;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Region region() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public int captureTime() {
        return captureTime;
    }

    public void setCaptureTime(int captureTime) {
        this.captureTime = captureTime;
    }

    public CaptureMode captureMode() {
        return captureMode;
    }

    public void setCaptureMode(CaptureMode captureMode) {
        this.captureMode = captureMode;
    }

    public ArenaState state() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public List<Reward> rewards() {
        return rewards;
    }

    public void setRewards(List<Reward> rewards) {
        this.rewards = rewards;
    }

    public int gracePeriod() {
        return graceperiod;
    }

    public void setGracePeriod(int gracePeriod) {
        this.graceperiod = gracePeriod;
    }

    public int maxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public Location hologramLocation() {
        return hologramLocation;
    }

    public void setHologramLocation(Location hologramLocation) {
        this.hologramLocation = hologramLocation;
    }

    /**
     * Mode of capture for this arena.
     */
    public enum CaptureMode {
        /** Player must hold the hill for captureTime seconds to win. */
        CAPTURE,
        /** Players earn points per second on the hill; first to maxScore wins. */
        SCORE
    }

    /**
     * Current runtime state of the arena.
     */
    public enum ArenaState {
        IDLE,
        ACTIVE,
        PAUSED
    }
}
