package kr.sniperpvp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import kr.sniperpvp.arena.ArenaService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

final class GameManager {
    private static final long TICKS_PER_SECOND = 20L;

    private final SniperPvpPlugin plugin;
    private final Supplier<PluginSettings> settings;
    private final ArenaService arena;
    private final GunService gun;
    private final KillStreakManager killStreaks;
    private final MatchScores scores = new MatchScores();
    private final HudManager hud;
    private final Map<UUID, BukkitTask> protectionTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> respawnTasks = new HashMap<>();
    private final Map<UUID, Long> respawnReadyTicks = new HashMap<>();
    private final Map<UUID, Long> nextRegenerationTicks = new HashMap<>();
    private final Map<UUID, PlayerHealthState> previousHealthStates = new HashMap<>();
    private final NamespacedKey movementSpeedKey;
    private final NamespacedKey jumpStrengthKey;

    private BukkitTask clockTask;
    private BukkitTask restartTask;
    private ScheduledTask tickRateRestoreTask;
    private Float tickRateBeforeSlowMotion;
    private boolean running;
    private boolean roundEnding;
    private long roundSerial;
    private long remainingTicks;
    private long totalTicks;
    private long hudTick;

    GameManager(
        SniperPvpPlugin plugin,
        Supplier<PluginSettings> settings,
        ArenaService arena,
        GunService gun,
        KillStreakManager killStreaks
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.arena = arena;
        this.gun = gun;
        this.killStreaks = killStreaks;
        this.hud = new HudManager(plugin, settings);
        this.movementSpeedKey = new NamespacedKey(plugin, "fixed_movement_speed");
        this.jumpStrengthKey = new NamespacedKey(plugin, "fixed_jump_strength");
    }

    boolean isRunning() {
        return running;
    }

    int start() {
        restoreTickRate();
        cancelTask(clockTask);
        cancelTask(restartTask);
        clockTask = null;
        restartTask = null;
        cancelAllProtectionTasks();
        nextRegenerationTicks.clear();

        roundSerial++;
        running = true;
        roundEnding = false;
        scores.reset();
        killStreaks.resetAll();
        gun.clearRuntimeState();
        hud.clearKillBanners();
        hud.clearGlobalKillLog();
        totalTicks = settings.get().game().matchDurationSeconds() * TICKS_PER_SECOND;
        remainingTicks = totalTicks;
        hudTick = 0L;

        int equipped = 0;
        if (arena.world() != null) {
            for (Player player : List.copyOf(arena.world().getPlayers())) {
                register(player);
                equipForRoundStart(player, true);
                showStartTitle(player);
                equipped++;
            }
        }

        refreshHud();
        clockTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        plugin.getLogger().info("Sniper deathmatch started for " + equipped + " players");
        return equipped;
    }

    int stop() {
        restoreTickRate();
        running = false;
        roundEnding = false;
        roundSerial++;
        cancelTask(clockTask);
        cancelTask(restartTask);
        clockTask = null;
        restartTask = null;
        cancelAllProtectionTasks();
        nextRegenerationTicks.clear();
        killStreaks.resetAll();
        scores.reset();
        hud.hideAll();

        int cleaned = 0;
        if (arena.world() != null) {
            for (Player player : List.copyOf(arena.world().getPlayers())) {
                removeCombatState(player);
                cleaned++;
            }
        }
        plugin.getServer().broadcast(Component.text(
            "저격 데스매치가 중지되었습니다.",
            NamedTextColor.YELLOW
        ));
        return cleaned;
    }

    void shutdown() {
        restoreTickRate();
        running = false;
        roundEnding = false;
        roundSerial++;
        cancelTask(clockTask);
        cancelTask(restartTask);
        clockTask = null;
        restartTask = null;
        cancelAllProtectionTasks();
        cancelAllRespawnTasks();
        nextRegenerationTicks.clear();
        hud.hideAll();
        if (arena.world() != null) {
            for (Player player : List.copyOf(arena.world().getPlayers())) {
                removeCombatState(player);
            }
        }
    }

    void handleJoin(Player player, boolean teleport) {
        if (running) {
            register(player);
            equipSpawnProtected(player, teleport);
            refreshHud();
            return;
        }
        if (roundEnding && arena.isArena(player)) {
            player.setInvulnerable(true);
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    void handleArenaEntry(Player player) {
        if (!running || !arena.isArena(player)) {
            return;
        }
        register(player);
        equipSpawnProtected(player, false);
        refreshHud();
    }

    void handleArenaExit(Player player) {
        cancelProtection(player.getUniqueId());
        nextRegenerationTicks.remove(player.getUniqueId());
        hud.removePlayer(player);
        removeCombatState(player);
        refreshHud();
    }

    void handleQuit(Player player) {
        cancelProtection(player.getUniqueId());
        hud.removePlayer(player);
        removeCombatState(player);
        killStreaks.remove(player);
    }

    void handleDeath(Player victim, Player killer, boolean headshot) {
        if (!running || !arena.isArena(victim)) {
            return;
        }

        cancelProtection(victim.getUniqueId());
        nextRegenerationTicks.remove(victim.getUniqueId());
        gun.removePlayer(victim);
        KillStreakCounter.Streak streak = killStreaks.onDeath(victim, killer);
        scheduleRespawn(victim);

        if (killer != null && killer != victim && arena.isArena(killer)) {
            register(killer);
            int totalKills = scores.recordKill(killer.getUniqueId(), killer.getName());
            hud.showGlobalKillLog(killer.getName(), victim.getName());
            hud.showKillBanner(
                killer,
                victim.getName(),
                totalKills,
                settings.get().game().killLimit(),
                streak == null ? 1 : streak.count(),
                headshot
            );
            refreshHud();
            if (totalKills >= settings.get().game().killLimit()) {
                finishMatch(
                    Set.of(killer.getUniqueId()),
                    resultHud(killer.getName(), totalKills, "킬 제한 달성")
                );
            }
        } else {
            refreshHud();
        }
    }

    void prepareRespawn(Player player) {
        BukkitTask task = respawnTasks.remove(player.getUniqueId());
        cancelTask(task);
    }

    void finishRespawn(Player player) {
        UUID playerId = player.getUniqueId();
        long readyAt = respawnReadyTicks.getOrDefault(playerId, (long) Bukkit.getCurrentTick());
        long remainingDelay = Math.max(0L, readyAt - Bukkit.getCurrentTick());
        if (running) {
            register(player);
            if (remainingDelay > 0L) {
                player.setGameMode(GameMode.SPECTATOR);
                player.setInvulnerable(true);
                player.removePotionEffect(PotionEffectType.GLOWING);
                BukkitTask delayedEquip = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    respawnTasks.remove(playerId);
                    respawnReadyTicks.remove(playerId);
                    if (player.isOnline() && running) {
                        equipSpawnProtected(player, true);
                        refreshHud();
                    }
                }, remainingDelay);
                respawnTasks.put(playerId, delayedEquip);
                return;
            }
            respawnReadyTicks.remove(playerId);
            equipSpawnProtected(player, true);
            refreshHud();
        } else if (roundEnding) {
            respawnReadyTicks.remove(playerId);
            player.setGameMode(GameMode.ADVENTURE);
            player.setInvulnerable(true);
        } else {
            respawnReadyTicks.remove(playerId);
            removeCombatState(player);
        }
    }

    void equipForRoundStart(Player player, boolean teleport) {
        equip(player, teleport, false);
    }

    void equipSpawnProtected(Player player, boolean teleport) {
        equip(player, teleport, true);
    }

    void rescueFromVoid(Player player) {
        equipSpawnProtected(player, true);
    }

    void setResourcePackLoaded(Player player, boolean loaded) {
        hud.setResourcePackLoaded(player, loaded);
        if (arena.isArena(player) && (running || roundEnding)) {
            refreshHud();
        }
    }

    void refreshAfterSettingsReload() {
        if (running) {
            refreshHud();
        }
    }

    String remainingTime() {
        return HudManager.formatTime(remainingTicks);
    }

    int kills(Player player) {
        return scores.kills(player.getUniqueId());
    }

    void markCombat(Player player) {
        if (!running || player == null || player.isDead() || !arena.isArena(player)) {
            return;
        }
        long delay = settings.get().game().regenerationCombatDelaySeconds() * TICKS_PER_SECOND;
        nextRegenerationTicks.put(player.getUniqueId(), (long) Bukkit.getCurrentTick() + delay);
    }

    private void equip(Player player, boolean teleport, boolean protectedSpawn) {
        if (!running || player.isDead()) {
            return;
        }
        if (teleport) {
            Location destination = arena.nextSpawn(player);
            if (destination != null) {
                player.teleport(destination);
            }
        }
        if (!arena.isArena(player)) {
            return;
        }

        cancelProtection(player.getUniqueId());
        gun.removePlayer(player);
        if (settings.get().game().clearInventoryOnEquip()) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }
        player.getInventory().setItem(0, gun.createRifle());
        player.getInventory().setHeldItemSlot(0);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFallDistance(0.0f);
        player.setAbsorptionAmount(0.0);
        player.clearActiveItem();
        applyCombatHealth(player);
        nextRegenerationTicks.remove(player.getUniqueId());

        applyCombatMovement(player);
        if (protectedSpawn) {
            beginSpawnProtection(player);
        } else {
            player.setInvulnerable(false);
            player.setNoDamageTicks(0);
            applyBuff(player, PotionEffectType.GLOWING, 0);
        }
        player.updateInventory();
    }

    private void beginSpawnProtection(Player player) {
        player.removePotionEffect(PotionEffectType.GLOWING);
        player.setInvulnerable(true);
        int protectionTicks = settings.get().game().spawnProtectionTicks();
        player.setNoDamageTicks(protectionTicks);
        long expectedRound = roundSerial;
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            protectionTasks.remove(player.getUniqueId());
            if (!player.isOnline() || !running || roundSerial != expectedRound || !arena.isArena(player)) {
                return;
            }
            player.setInvulnerable(false);
            player.setNoDamageTicks(0);
            applyBuff(player, PotionEffectType.GLOWING, 0);
            player.sendActionBar(Component.text("무적 종료", NamedTextColor.GRAY));
        }, protectionTicks);
        protectionTasks.put(player.getUniqueId(), task);
    }

    private void scheduleRespawn(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask previous = respawnTasks.remove(playerId);
        cancelTask(previous);
        long delay = settings.get().game().respawnDelaySeconds() * TICKS_PER_SECOND;
        respawnReadyTicks.put(playerId, (long) Bukkit.getCurrentTick() + delay);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            respawnTasks.remove(playerId);
            if (player.isOnline() && player.isDead()) {
                player.spigot().respawn();
            }
        }, delay);
        respawnTasks.put(playerId, task);
    }

    private void tick() {
        if (!running) {
            return;
        }
        if (remainingTicks > 0L) {
            remainingTicks--;
        }
        tickRegeneration();
        hudTick++;
        if (hudTick >= settings.get().hud().updateIntervalTicks()) {
            hudTick = 0L;
            refreshHud();
        }
        if (remainingTicks <= 0L) {
            finishByTimeLimit();
        }
    }

    private void finishByTimeLimit() {
        List<MatchScores.Entry> ranking = scores.ranking();
        if (ranking.isEmpty()) {
            finishMatch(Set.of(), resultHud("우승자 없음", 0, "시간 종료"));
            return;
        }

        int bestKills = ranking.getFirst().kills();
        List<String> winners = ranking.stream()
            .filter(score -> score.kills() == bestKills)
            .map(MatchScores.Entry::name)
            .toList();
        if (winners.size() == 1) {
            finishMatch(
                Set.of(ranking.getFirst().playerId()),
                resultHud(winners.getFirst(), bestKills, "시간 종료")
            );
        } else {
            Set<UUID> winnerIds = ranking.stream()
                .filter(score -> score.kills() == bestKills)
                .map(MatchScores.Entry::playerId)
                .collect(java.util.stream.Collectors.toSet());
            finishMatch(
                winnerIds,
                resultHud(String.join(", ", winners), bestKills, "시간 종료 · 공동 우승")
            );
        }
    }

    private void finishMatch(Set<UUID> winnerIds, Component hudResult) {
        if (!running) {
            return;
        }
        running = false;
        roundEnding = true;
        roundSerial++;
        cancelTask(clockTask);
        clockTask = null;
        cancelAllProtectionTasks();
        nextRegenerationTicks.clear();
        gun.clearRuntimeState();

        Collection<Player> players = arenaPlayers();
        for (Player player : players) {
            player.setInvulnerable(true);
            boolean winner = winnerIds.contains(player.getUniqueId());
            Component result = resultMessage(winner);
            player.showTitle(Title.title(
                result,
                Component.empty(),
                Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(4), Duration.ofMillis(600))
            ));
            playResultSound(player, winner);
        }
        hud.showResult(players, onlineRanking(players), player -> hudResult);
        beginEndingSlowMotion();

        if (settings.get().game().autoStart()) {
            long expectedRound = roundSerial;
            long delay = Math.max(1L, settings.get().game().restartDelaySeconds() * TICKS_PER_SECOND);
            restartTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                restartTask = null;
                if (roundEnding && !running && roundSerial == expectedRound) {
                    start();
                }
            }, delay);
        }
    }

    private static Component resultMessage(boolean winner) {
        return Component.text(winner ? "VICTORY" : "DEFEAT", winner ? NamedTextColor.AQUA : NamedTextColor.RED)
            .decorate(TextDecoration.BOLD);
    }

    private static Component resultHud(String winnerName, int kills, String subtitle) {
        return Component.text(winnerName, NamedTextColor.AQUA)
            .decorate(TextDecoration.BOLD)
            .append(Component.text("  " + kills + "킬", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD))
            .append(Component.text("  ·  " + subtitle, NamedTextColor.WHITE));
    }

    private void playResultSound(Player player, boolean winner) {
        PluginSettings.MatchResultSoundSettings resultSounds = settings.get().matchResultSounds();
        String sound = winner ? resultSounds.victory() : resultSounds.defeat();
        player.playSound(
            player.getLocation(),
            sound,
            SoundCategory.PLAYERS,
            (float) resultSounds.volume(),
            (float) resultSounds.pitch()
        );
    }

    private void showStartTitle(Player player) {
        player.showTitle(Title.title(
            Component.text("저격 데스매치 시작", NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
            Component.text(settings.get().game().killLimit() + "킬을 먼저 달성하세요!", NamedTextColor.WHITE),
            Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500))
        ));
    }

    private void tickRegeneration() {
        if (nextRegenerationTicks.isEmpty()) {
            return;
        }
        long currentTick = Bukkit.getCurrentTick();
        long interval = settings.get().game().regenerationIntervalSeconds() * TICKS_PER_SECOND;
        double amount = settings.get().game().regenerationAmount();
        for (Map.Entry<UUID, Long> entry : Map.copyOf(nextRegenerationTicks).entrySet()) {
            if (currentTick < entry.getValue()) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || player.isDead() || !arena.isArena(player)) {
                nextRegenerationTicks.remove(entry.getKey());
                continue;
            }
            AttributeInstance maximum = player.getAttribute(Attribute.MAX_HEALTH);
            double maximumHealth = maximum == null ? settings.get().game().maxHealth() : maximum.getValue();
            if (player.getHealth() >= maximumHealth) {
                nextRegenerationTicks.remove(entry.getKey());
                continue;
            }
            player.setHealth(Math.min(maximumHealth, player.getHealth() + amount));
            if (player.getHealth() >= maximumHealth) {
                nextRegenerationTicks.remove(entry.getKey());
            } else {
                nextRegenerationTicks.put(entry.getKey(), currentTick + interval);
            }
        }
    }

    private void applyCombatHealth(Player player) {
        AttributeInstance maximum = player.getAttribute(Attribute.MAX_HEALTH);
        if (maximum == null) {
            return;
        }
        previousHealthStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerHealthState(
            maximum.getBaseValue(),
            player.isHealthScaled(),
            player.getHealthScale()
        ));
        maximum.setBaseValue(settings.get().game().maxHealth());
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
        if (!player.isDead()) {
            player.setHealth(maximum.getValue());
        }
    }

    private void restorePreviousHealth(Player player) {
        PlayerHealthState previous = previousHealthStates.remove(player.getUniqueId());
        if (previous == null) {
            return;
        }
        AttributeInstance maximum = player.getAttribute(Attribute.MAX_HEALTH);
        if (maximum != null) {
            maximum.setBaseValue(previous.maximumHealthBase());
            if (!player.isDead() && player.getHealth() > maximum.getValue()) {
                player.setHealth(maximum.getValue());
            }
        }
        if (previous.healthScaled()) {
            player.setHealthScaled(true);
            player.setHealthScale(previous.healthScale());
        } else {
            player.setHealthScaled(false);
        }
    }

    private void beginEndingSlowMotion() {
        restoreTickRate();
        tickRateBeforeSlowMotion = plugin.getServer().getServerTickManager().getTickRate();
        plugin.getServer().getServerTickManager().setTickRate((float) settings.get().game().endSlowMotionTickRate());
        tickRateRestoreTask = plugin.getServer().getAsyncScheduler().runDelayed(
            plugin,
            ignored -> plugin.getServer().getScheduler().runTask(plugin, this::restoreTickRate),
            settings.get().game().endSlowMotionDurationSeconds(),
            TimeUnit.SECONDS
        );
    }

    private void restoreTickRate() {
        ScheduledTask pending = tickRateRestoreTask;
        tickRateRestoreTask = null;
        if (pending != null) {
            pending.cancel();
        }
        Float previous = tickRateBeforeSlowMotion;
        tickRateBeforeSlowMotion = null;
        if (previous != null) {
            plugin.getServer().getServerTickManager().setTickRate(previous);
        }
    }

    private void register(Player player) {
        scores.register(player.getUniqueId(), player.getName());
    }

    private void refreshHud() {
        Collection<Player> players = arenaPlayers();
        hud.showMatch(players, onlineRanking(players), remainingTicks, totalTicks);
    }

    private List<MatchScores.Entry> onlineRanking(Collection<Player> players) {
        Set<UUID> onlineIds = players.stream().map(Player::getUniqueId).collect(java.util.stream.Collectors.toSet());
        return scores.ranking(onlineIds);
    }

    private Collection<Player> arenaPlayers() {
        if (arena.world() == null) {
            return List.of();
        }
        return new ArrayList<>(arena.world().getPlayers());
    }

    private void applyBuff(Player player, PotionEffectType type, int amplifier) {
        player.addPotionEffect(new PotionEffect(
            type,
            PotionEffect.INFINITE_DURATION,
            amplifier,
            false,
            false,
            true
        ));
    }

    private void applyCombatMovement(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);

        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(movementSpeedKey);
            movementSpeed.addTransientModifier(new AttributeModifier(
                movementSpeedKey,
                settings.get().game().movementSpeed() - movementSpeed.getBaseValue(),
                AttributeModifier.Operation.ADD_NUMBER
            ));
        }

        AttributeInstance jumpStrength = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpStrength != null) {
            jumpStrength.removeModifier(jumpStrengthKey);
            jumpStrength.addTransientModifier(new AttributeModifier(
                jumpStrengthKey,
                settings.get().game().jumpStrength() - jumpStrength.getBaseValue(),
                AttributeModifier.Operation.ADD_NUMBER
            ));
        }
    }

    private void removeCombatMovement(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(movementSpeedKey);
        }
        AttributeInstance jumpStrength = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpStrength != null) {
            jumpStrength.removeModifier(jumpStrengthKey);
        }
    }

    private void removeCombatState(Player player) {
        nextRegenerationTicks.remove(player.getUniqueId());
        player.setInvulnerable(false);
        player.setNoDamageTicks(0);
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.GLOWING);
        removeCombatMovement(player);
        restorePreviousHealth(player);
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (gun.isRifle(item)) {
                player.getInventory().setItem(slot, null);
            }
        }
        gun.removePlayer(player);
    }

    private void cancelProtection(UUID playerId) {
        cancelTask(protectionTasks.remove(playerId));
    }

    private void cancelAllProtectionTasks() {
        protectionTasks.values().forEach(BukkitTask::cancel);
        protectionTasks.clear();
    }

    private void cancelAllRespawnTasks() {
        respawnTasks.values().forEach(BukkitTask::cancel);
        respawnTasks.clear();
        respawnReadyTicks.clear();
    }

    private static void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private record PlayerHealthState(double maximumHealthBase, boolean healthScaled, double healthScale) {
    }
}
