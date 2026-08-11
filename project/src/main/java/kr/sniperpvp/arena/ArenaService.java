package kr.sniperpvp.arena;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import kr.sniperpvp.PluginSettings;
import kr.sniperpvp.SniperPvpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;

public final class ArenaService implements Listener {
    private final SniperPvpPlugin plugin;
    private final Supplier<PluginSettings> settings;
    private final ArenaBuilder builder = new ArenaBuilder();
    private final WeightedSpawnSelector spawnSelector = new WeightedSpawnSelector();
    private final long[] lastSpawnUseNanos = new long[ArenaBlueprint.spawns().size()];
    private final Set<String> preparedWorlds = ConcurrentHashMap.newKeySet();

    public ArenaService(SniperPvpPlugin plugin, Supplier<PluginSettings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        prepare(event.getWorld());
    }

    public void prepareLoadedWorlds() {
        Bukkit.getWorlds().forEach(this::prepare);
    }

    public void prepare(World world) {
        if (!world.getName().equals(settings.get().arena().worldName())) {
            return;
        }
        configureWorld(world);
        if (!preparedWorlds.add(world.getUID().toString())) {
            return;
        }
        if (!settings.get().arena().autoBuild()) {
            plugin.getLogger().info("Arena auto-build is disabled for " + world.getName());
            return;
        }

        Integer version = world.getPersistentDataContainer().get(
            plugin.arenaVersionKey(),
            PersistentDataType.INTEGER
        );
        if (version != null && version == ArenaConstants.BUILD_VERSION) {
            plugin.getLogger().info("Arena layout version " + version + " is already present");
            return;
        }

        plugin.getLogger().info("Building the 300x300 gray-concrete arena...");
        ArenaBuilder.BuildResult result = builder.build(world);
        world.getPersistentDataContainer().set(
            plugin.arenaVersionKey(),
            PersistentDataType.INTEGER,
            ArenaConstants.BUILD_VERSION
        );
        plugin.getLogger().info("Arena built: " + result.regions() + " regions, "
            + result.plannedBlocks() + " planned block writes, " + result.elapsedMillis() + " ms");
    }

    public World world() {
        return Bukkit.getWorld(settings.get().arena().worldName());
    }

    public boolean isArena(World world) {
        return world != null && world.getName().equals(settings.get().arena().worldName());
    }

    public boolean isArena(Player player) {
        return isArena(player.getWorld());
    }

    public boolean contains(Location location) {
        return location.getWorld() != null
            && isArena(location.getWorld())
            && ArenaConstants.contains(location.getBlockX(), location.getBlockZ());
    }

    public Location nextSpawn() {
        return nextSpawn(null);
    }

    public Location nextSpawn(Player excludedPlayer) {
        World arena = world();
        if (arena == null) {
            return null;
        }
        List<ArenaSpawn> spawns = ArenaBlueprint.spawns();
        List<WeightedSpawnSelector.PlayerPoint> occupied = arena.getPlayers().stream()
            .filter(player -> player != excludedPlayer)
            .filter(player -> !player.isDead())
            .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
            .map(player -> new WeightedSpawnSelector.PlayerPoint(
                player.getLocation().getX(),
                player.getLocation().getZ()
            ))
            .toList();
        long now = System.nanoTime();
        int index = spawnSelector.select(
            spawns,
            occupied,
            lastSpawnUseNanos,
            now,
            ThreadLocalRandom.current()
        );
        lastSpawnUseNanos[index] = now;
        ArenaSpawn spawn = spawns.get(index);
        int y = arena.getHighestBlockYAt(spawn.x(), spawn.z(), HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
        double towardCenterX = -spawn.x();
        double towardCenterZ = -spawn.z();
        float yaw = (float) Math.toDegrees(Math.atan2(-towardCenterX, towardCenterZ));
        return new Location(arena, spawn.x() + 0.5, y, spawn.z() + 0.5, yaw, 0.0f);
    }

    private void configureWorld(World world) {
        world.setPVP(true);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setSpawnFlags(false, false);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setSpawnLocation(0, ArenaConstants.FLOOR_Y + 5, 0, 0.0f);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(300.0);
        border.setWarningDistance(3);
        border.setDamageAmount(0.0);
    }
}
