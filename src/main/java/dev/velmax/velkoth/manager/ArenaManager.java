package dev.velmax.velkoth.manager;

import dev.velmax.velkoth.VelKothPlugin;
import dev.velmax.velkoth.arena.Arena;
import dev.velmax.velkoth.arena.region.CuboidRegion;
import dev.velmax.velkoth.arena.region.CylinderRegion;
import dev.velmax.velkoth.arena.region.Region;
import dev.velmax.velkoth.config.ArenaConfig;
import dev.velmax.velkoth.reward.CommandReward;
import dev.velmax.velkoth.reward.EconomyReward;
import dev.velmax.velkoth.reward.ItemReward;
import dev.velmax.velkoth.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all KoTH arenas: CRUD, persistence, and lookups.
 */
public final class ArenaManager {

    private final VelKothPlugin plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();

    public ArenaManager(VelKothPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all arenas from the arena config.
     */
    public void loadArenas() {
        arenas.clear();
        ArenaConfig config = plugin.getArenaConfig();
        for (var entry : config.getArenas().entrySet()) {
            String id = entry.getKey();
            ArenaConfig.ArenaEntry ae = entry.getValue();
            Arena arena = deserializeArena(id, ae);
            if (arena != null) {
                arenas.put(id.toLowerCase(), arena);
            }
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }

    /**
     * Save all arenas back to config.
     */
    public void saveArenas() {
        ArenaConfig config = plugin.getArenaConfig();
        Map<String, ArenaConfig.ArenaEntry> entries = new LinkedHashMap<>();
        for (var arena : arenas.values()) {
            entries.put(arena.id(), serializeArena(arena));
        }
        config.setArenas(entries);
        config.save();
    }

    private @Nullable Arena deserializeArena(String id, ArenaConfig.ArenaEntry ae) {
        World world = Bukkit.getWorld(ae.getWorld());
        if (world == null) {
            plugin.getLogger().warning("World '" + ae.getWorld() + "' not found for arena '" + id + "'");
            return null;
        }

        Region region;
        if ("CYLINDER".equalsIgnoreCase(ae.getRegionType())) {
            region = new CylinderRegion(world, ae.getCenterX(), ae.getCenterZ(),
                    ae.getRadius(), ae.getMinY(), ae.getMaxY());
        } else {
            List<Double> p1 = ae.getPos1();
            List<Double> p2 = ae.getPos2();
            region = new CuboidRegion(world, p1.get(0), p1.get(1), p1.get(2),
                    p2.get(0), p2.get(1), p2.get(2));
        }

        Arena.CaptureMode mode = Arena.CaptureMode.valueOf(ae.getCaptureMode().toUpperCase());
        Arena arena = new Arena(id, ae.getDisplayName(), region, ae.getCaptureTime(),
                mode, ae.getGracePeriod(), ae.getMaxScore());

        // Parse rewards
        List<Reward> rewards = new ArrayList<>();
        for (String raw : ae.getRewards()) {
            Reward reward = parseReward(raw);
            if (reward != null) {
                rewards.add(reward);
            }
        }
        arena.setRewards(rewards);

        // Parse custom hologram location
        if (ae.getHologramLocation() != null && ae.getHologramLocation().size() == 3) {
            List<Double> loc = ae.getHologramLocation();
            arena.setHologramLocation(new org.bukkit.Location(world, loc.get(0), loc.get(1), loc.get(2)));
        }

        return arena;
    }

    /**
     * Parse a reward string from config or command.
     */
    public @Nullable Reward parseReward(String raw) {
        if (raw.startsWith("ECONOMY:")) {
            try {
                return new EconomyReward(Double.parseDouble(raw.substring(8)));
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else if (raw.startsWith("ITEM:")) {
            String[] parts = raw.split(":");
            if (parts.length >= 2) {
                Material mat = Material.matchMaterial(parts[1]);
                int amount = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                if (mat != null) {
                    return new ItemReward(new ItemStack(mat, amount));
                }
            }
            return null;
        } else if (raw.startsWith("COMMAND:")) {
            return new CommandReward(raw.substring(8));
        } else {
            return new CommandReward(raw);
        }
    }

    private ArenaConfig.ArenaEntry serializeArena(Arena arena) {
        ArenaConfig.ArenaEntry ae = new ArenaConfig.ArenaEntry();
        ae.setDisplayName(arena.displayName());
        ae.setCaptureTime(arena.captureTime());
        ae.setCaptureMode(arena.captureMode().name());
        ae.setMaxScore(arena.maxScore());
        ae.setGracePeriod(arena.gracePeriod());

        Region region = arena.region();
        ae.setWorld(region.getWorld().getName());
        ae.setRegionType(region.getType());

        switch (region) {
            case CuboidRegion cuboid -> {
                ae.setPos1(List.of(cuboid.minX(), cuboid.minY(), cuboid.minZ()));
                ae.setPos2(List.of(cuboid.maxX(), cuboid.maxY(), cuboid.maxZ()));
            }
            case CylinderRegion cylinder -> {
                ae.setRegionType("CYLINDER");
                ae.setCenterX(cylinder.centerX());
                ae.setCenterZ(cylinder.centerZ());
                ae.setRadius(cylinder.radius());
                ae.setMinY(cylinder.minY());
                ae.setMaxY(cylinder.maxY());
            }
        }

        // Serialize rewards as command strings
        List<String> rewardStrings = new ArrayList<>();
        for (Reward r : arena.rewards()) {
            switch (r) {
                case CommandReward cr -> rewardStrings.add("COMMAND:" + cr.command());
                case EconomyReward er -> rewardStrings.add("ECONOMY:" + er.amount());
                case ItemReward ir ->
                    rewardStrings.add("ITEM:" + ir.item().getType().name() + ":" + ir.item().getAmount());
            }
        }
        ae.setRewards(rewardStrings);

        // Serialize custom hologram location
        if (arena.hologramLocation() != null) {
            org.bukkit.Location loc = arena.hologramLocation();
            ae.setHologramLocation(List.of(loc.getX(), loc.getY(), loc.getZ()));
        } else {
            ae.setHologramLocation(null);
        }

        return ae;
    }

    public @Nullable Arena getArena(@NotNull String id) {
        return arenas.get(id.toLowerCase());
    }

    public @NotNull Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public @NotNull Collection<Arena> getActiveArenas() {
        return arenas.values().stream()
                .filter(a -> a.state() == Arena.ArenaState.ACTIVE)
                .toList();
    }

    /**
     * Checks if there is any active arena without allocating memory.
     * @return true if at least one arena is ACTIVE, false otherwise
     */
    public boolean hasActiveArena() {
        for (Arena arena : arenas.values()) {
            if (arena.state() == Arena.ArenaState.ACTIVE) {
                return true;
            }
        }
        return false;
    }

    public boolean arenaExists(String id) {
        return arenas.containsKey(id.toLowerCase());
    }

    public void addArena(Arena arena) {
        arenas.put(arena.id().toLowerCase(), arena);
        saveArenas();
    }

    public boolean removeArena(String id) {
        Arena removed = arenas.remove(id.toLowerCase());
        if (removed != null) {
            saveArenas();
            return true;
        }
        return false;
    }

    public @NotNull Set<String> getArenaIds() {
        return Collections.unmodifiableSet(arenas.keySet());
    }
}
