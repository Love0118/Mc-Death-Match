package kr.sniperpvp.arena;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import kr.sniperpvp.PluginSettings;
import kr.sniperpvp.SniperPvpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;

public final class ArenaService implements Listener {
    private final SniperPvpPlugin plugin;
    private final Supplier<PluginSettings> settings;
    private final ArenaBuilder builder = new ArenaBuilder();
    private final WeightedSpawnSelector spawnSelector = new WeightedSpawnSelector();
    private final long[] lastSpawnUseNanos = new long[ArenaBlueprint.spawns().size()];
    private final Set<String> preparedWorlds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> jumpPadCooldowns = new ConcurrentHashMap<>();

    public ArenaService(SniperPvpPlugin plugin, Supplier<PluginSettings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        prepare(event.getWorld());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        jumpPadCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (!isArena(player)
            || player.isDead()
            || player.getGameMode() == GameMode.SPECTATOR
            || sameBlock(from, to)) {
            return;
        }

        Block floorBlock = to.getWorld().getBlockAt(
            to.getBlockX(),
            (int) Math.floor(to.getY() - 0.15),
            to.getBlockZ()
        );
        if (floorBlock.getType() != Material.SLIME_BLOCK) {
            return;
        }
        JumpPad pad = ArenaBlueprint.jumpPadAt(
            floorBlock.getX(),
            floorBlock.getY(),
            floorBlock.getZ()
        );
        if (pad == null) {
            return;
        }

        int currentTick = Bukkit.getCurrentTick();
        if (currentTick < jumpPadCooldowns.getOrDefault(player.getUniqueId(), 0)) {
            return;
        }
        jumpPadCooldowns.put(
            player.getUniqueId(),
            currentTick + pad.horizontalGuidanceDelayTicks() + 20
        );
        player.setFallDistance(0.0f);
        player.setVelocity(pad.verticalLaunchVector());
        player.getWorld().playSound(
            to,
            "minecraft:block.slime_block.fall",
            SoundCategory.PLAYERS,
            0.9f,
            0.85f
        );
        Bukkit.getScheduler().runTaskLater(
            plugin,
            () -> guideJump(player, pad),
            pad.horizontalGuidanceDelayTicks()
        );
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

        boolean replacingExistingLayout = version != null;
        plugin.getLogger().info("Building arena layout version " + ArenaConstants.BUILD_VERSION
            + (replacingExistingLayout ? " over version " + version : " in a new world") + "...");
        ArenaBuilder.BuildResult result = builder.build(world, replacingExistingLayout);
        world.getPersistentDataContainer().set(
            plugin.arenaVersionKey(),
            PersistentDataType.INTEGER,
            ArenaConstants.BUILD_VERSION
        );
        plugin.getLogger().info("Arena built: " + result.regions() + " concrete regions, "
            + result.jumpPads() + " jump pads (" + result.jumpPadBlocks() + " slime blocks), "
            + result.plannedBlocks() + " planned concrete writes, " + result.clearedBlocks()
            + " old blocks cleared, " + result.elapsedMillis() + " ms");
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
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setSpawnLocation(0, ArenaConstants.FLOOR_Y + 5, 0, 0.0f);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.PVP, true);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, false);
        world.setGameRule(GameRules.FALL_DAMAGE, false);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        world.setGameRule(GameRules.RESPAWN_RADIUS, 0);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(300.0);
        border.setWarningDistance(3);
        border.setDamageAmount(0.0);
    }

    private static boolean sameBlock(Location first, Location second) {
        return first.getBlockX() == second.getBlockX()
            && first.getBlockY() == second.getBlockY()
            && first.getBlockZ() == second.getBlockZ();
    }

    private void guideJump(Player player, JumpPad pad) {
        if (!player.isOnline()
            || player.isDead()
            || player.getGameMode() == GameMode.SPECTATOR
            || !isArena(player)
            || player.getLocation().getY() <= pad.surfaceY() + 1.0) {
            return;
        }
        player.setVelocity(pad.guidedLaunchVector(
            player.getLocation().getX(),
            player.getLocation().getZ(),
            player.getVelocity().getY()
        ));
    }
}
