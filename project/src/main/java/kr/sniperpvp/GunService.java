package kr.sniperpvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import kr.sniperpvp.arena.ArenaService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

final class GunService {
    private static final Material RIFLE_MATERIAL = Material.PAPER;
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final long EVENT_DEDUPLICATION_NANOS = 20_000_000L;
    private static final long FEEDBACK_THROTTLE_NANOS = 250_000_000L;
    private static final long RELOAD_INPUT_FIRE_BLOCK_TICKS = 2L;
    private static final NamespacedKey SCOPE_OVERLAY = NamespacedKey.fromString("sniperpvp:misc/scope");

    private final SniperPvpPlugin plugin;
    private final Supplier<PluginSettings> settings;
    private final ArenaService arena;
    private final BooleanSupplier gameRunning;
    private final Set<UUID> applyingShotDamage = new HashSet<>();
    private final Map<UUID, Long> lastTriggerNanos = new HashMap<>();
    private final Map<UUID, Long> lastFeedbackNanos = new HashMap<>();
    private final Map<UUID, Long> fireBlockedUntilTick = new HashMap<>();
    private final Map<UUID, ShotHit> latestHits = new HashMap<>();
    private final Map<UUID, GunRuntime> runtimes = new HashMap<>();

    GunService(
        SniperPvpPlugin plugin,
        Supplier<PluginSettings> settings,
        ArenaService arena,
        BooleanSupplier gameRunning
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.arena = arena;
        this.gameRunning = gameRunning;
    }

    ItemStack createRifle() {
        PluginSettings.RifleSettings rifle = settings.get().rifle();
        return createRifle(new PluginSettings.RifleModelSettings(
            "gameplay",
            "저격총",
            rifle.itemModel(),
            rifle.customModelData()
        ));
    }

    ItemStack createRifle(PluginSettings.RifleModelSettings model) {
        PluginSettings.RifleSettings rifle = settings.get().rifle();
        ItemStack item = new ItemStack(RIFLE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(rifleName(model.displayName(), rifle.magazineSize(), rifle.magazineSize()));
        meta.lore(List.of(
            Component.text("우클릭: 줌 전환", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("좌클릭: 즉시 발사", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Q: 재장전", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("엄폐물은 관통하지 않습니다", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));
        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        modelData.setFloats(List.of((float) model.customModelData()));
        meta.setCustomModelDataComponent(modelData);
        NamespacedKey itemModel = NamespacedKey.fromString(model.itemModel());
        if (itemModel == null) {
            throw new IllegalStateException("Invalid rifle item model: " + model.itemModel());
        }
        meta.setItemModel(itemModel);
        meta.getPersistentDataContainer().set(plugin.rifleKey(), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(
            plugin.rifleModelKey(),
            PersistentDataType.STRING,
            model.id()
        );
        meta.getPersistentDataContainer().set(
            plugin.rifleDisplayNameKey(),
            PersistentDataType.STRING,
            model.displayName()
        );
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    boolean isRifle(ItemStack item) {
        if (item == null || item.getType() != RIFLE_MATERIAL || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(
            plugin.rifleKey(),
            PersistentDataType.BYTE
        );
        return marker != null && marker == (byte) 1;
    }

    void toggleZoom(Player player, ItemStack item) {
        if (!isRifle(item) || !canOperateRifle(player)) {
            return;
        }
        GunRuntime runtime = runtimeFor(player);
        if (runtime.scoped) {
            endZoom(player, true);
            return;
        }
        long now = System.nanoTime();
        if (runtime.reloading || now < runtime.readyAtNanos) {
            showUnavailable(player, runtime);
            playThrottledFeedback(player, now);
            return;
        }

        runtime.previousHelmet = cloneOrNull(player.getInventory().getHelmet());
        runtime.previousSlowness = player.getPotionEffect(PotionEffectType.SLOWNESS);
        player.getInventory().setHelmet(createScopeOverlayHelmet());
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            PotionEffect.INFINITE_DURATION,
            settings.get().rifle().zoomSlownessAmplifier(),
            false,
            false,
            false
        ));
        runtime.scoped = true;
        playPrivate(player, settings.get().sounds().zoomIn(), settings.get().sounds().zoomVolume(), 1.0);
    }

    void endZoom(Player player) {
        endZoom(player, true);
    }

    boolean tryReload(Player player) {
        if (!canOperateRifle(player) || !isRifle(player.getInventory().getItemInMainHand())) {
            return false;
        }
        return beginManualReload(player);
    }

    boolean tryReloadFromDrop(Player player, ItemStack droppedItem) {
        if (!canOperateRifle(player) || !isRifle(droppedItem)) {
            return false;
        }
        fireBlockedUntilTick.put(
            player.getUniqueId(),
            (long) Bukkit.getCurrentTick() + RELOAD_INPUT_FIRE_BLOCK_TICKS
        );
        // A cancelled drop is restored to the inventory after PlayerDropItemEvent returns.
        // Start the reload one tick later, but block the accompanying swing immediately.
        plugin.getServer().getScheduler().runTask(plugin, () -> tryReload(player));
        return true;
    }

    private boolean beginManualReload(Player player) {
        GunRuntime runtime = runtimeFor(player);
        if (runtime.reloading || runtime.magazine.isFull()) {
            return false;
        }
        endZoom(player, true);
        cancelTask(runtime.boltTask);
        runtime.boltTask = null;
        beginReload(player, runtime, System.nanoTime(), settings.get().rifle());
        return true;
    }

    boolean tryFire(Player shooter) {
        if (!canOperateRifle(shooter) || !isRifle(shooter.getInventory().getItemInMainHand())) {
            return false;
        }

        UUID playerId = shooter.getUniqueId();
        Long blockedUntilTick = fireBlockedUntilTick.get(playerId);
        if (blockedUntilTick != null) {
            if (Bukkit.getCurrentTick() <= blockedUntilTick) {
                return false;
            }
            fireBlockedUntilTick.remove(playerId, blockedUntilTick);
        }
        long now = System.nanoTime();
        long previousTrigger = lastTriggerNanos.getOrDefault(playerId, 0L);
        if (now - previousTrigger < EVENT_DEDUPLICATION_NANOS) {
            return false;
        }
        lastTriggerNanos.put(playerId, now);

        GunRuntime runtime = runtimeFor(shooter);
        PluginSettings.RifleSettings rifle = settings.get().rifle();
        if (rifle.requireZoom() && !runtime.scoped) {
            shooter.sendActionBar(Component.text("우클릭으로 조준한 뒤 발사하세요.", NamedTextColor.YELLOW));
            playThrottledFeedback(shooter, now);
            return false;
        }
        if (runtime.reloading || now < runtime.readyAtNanos) {
            showUnavailable(shooter, runtime);
            playThrottledFeedback(shooter, now);
            return false;
        }
        if (!runtime.magazine.consume()) {
            beginReload(shooter, runtime, now, rifle);
            playThrottledFeedback(shooter, now);
            return false;
        }

        boolean scopedShot = runtime.scoped;
        playWorld(
            shooter,
            settings.get().sounds().fire(),
            settings.get().sounds().fireVolume(),
            settings.get().sounds().firePitch()
        );
        fireHitscan(shooter, rifle, scopedShot);

        if (runtime.magazine.isEmpty()) {
            endZoom(shooter, true);
            beginReload(shooter, runtime, now, rifle);
        } else {
            scheduleBolt(shooter, runtime, rifle.boltSoundDelayTicks());
            runtime.readyAtNanos = now + rifle.cooldownTicks() * NANOS_PER_TICK;
            shooter.setCooldown(RIFLE_MATERIAL, rifle.cooldownTicks());
            updateRifleName(shooter, runtime);
        }
        return true;
    }

    boolean isApplyingShotDamage(Player damager) {
        return applyingShotDamage.contains(damager.getUniqueId());
    }

    boolean consumeHeadshot(Player victim, Player killer) {
        ShotHit hit = latestHits.remove(victim.getUniqueId());
        return hit != null
            && killer != null
            && hit.shooterId().equals(killer.getUniqueId())
            && System.nanoTime() - hit.createdAtNanos() <= 1_000_000_000L
            && hit.headshot();
    }

    void removePlayer(Player player) {
        UUID id = player.getUniqueId();
        endZoom(player, false);
        GunRuntime runtime = runtimes.remove(id);
        cancelTasks(runtime);
        applyingShotDamage.remove(id);
        lastTriggerNanos.remove(id);
        lastFeedbackNanos.remove(id);
        fireBlockedUntilTick.remove(id);
        latestHits.remove(id);
        latestHits.entrySet().removeIf(entry -> entry.getValue().shooterId().equals(id));
    }

    void clearRuntimeState() {
        for (UUID playerId : List.copyOf(runtimes.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                endZoom(player, false);
                GunRuntime runtime = runtimes.get(playerId);
                if (runtime != null && runtime.reloading) {
                    updateRifleName(player, runtime);
                }
            }
        }
        for (GunRuntime runtime : runtimes.values()) {
            cancelTasks(runtime);
        }
        runtimes.clear();
        applyingShotDamage.clear();
        lastTriggerNanos.clear();
        lastFeedbackNanos.clear();
        fireBlockedUntilTick.clear();
        latestHits.clear();
    }

    private ItemStack createScopeOverlayHelmet() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("조준경 오버레이", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        EquippableComponent equippable = meta.getEquippable();
        equippable.setSlot(EquipmentSlot.HEAD);
        equippable.setEquipSound(null);
        equippable.setModel(null);
        equippable.setCameraOverlay(SCOPE_OVERLAY);
        equippable.setAllowedEntities(EntityType.PLAYER);
        equippable.setDispensable(false);
        equippable.setSwappable(false);
        equippable.setDamageOnHurt(false);
        equippable.setEquipOnInteract(false);
        meta.setEquippable(equippable);
        meta.getPersistentDataContainer().set(plugin.scopeHelmetKey(), PersistentDataType.BYTE, (byte) 1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isScopeOverlayHelmet(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(
            plugin.scopeHelmetKey(),
            PersistentDataType.BYTE
        );
        return marker != null && marker == (byte) 1;
    }

    private void endZoom(Player player, boolean playSound) {
        GunRuntime runtime = runtimes.get(player.getUniqueId());
        if (runtime == null) {
            return;
        }
        if (!runtime.scoped) {
            return;
        }
        runtime.scoped = false;
        if (isScopeOverlayHelmet(player.getInventory().getHelmet())) {
            player.getInventory().setHelmet(cloneOrNull(runtime.previousHelmet));
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        if (runtime.previousSlowness != null) {
            player.addPotionEffect(runtime.previousSlowness);
        }
        runtime.previousHelmet = null;
        runtime.previousSlowness = null;
        if (playSound) {
            playPrivate(player, settings.get().sounds().zoomOut(), settings.get().sounds().zoomVolume(), 1.0);
        }
    }

    private GunRuntime runtimeFor(Player player) {
        UUID playerId = player.getUniqueId();
        int capacity = settings.get().rifle().magazineSize();
        GunRuntime runtime = runtimes.get(playerId);
        if (runtime != null && runtime.magazine.capacity() == capacity) {
            return runtime;
        }
        if (runtime != null) {
            endZoom(player, false);
            cancelTasks(runtime);
        }
        GunRuntime replacement = new GunRuntime(capacity);
        runtimes.put(playerId, replacement);
        return replacement;
    }

    private void beginReload(
        Player shooter,
        GunRuntime runtime,
        long now,
        PluginSettings.RifleSettings rifle
    ) {
        if (runtime.reloading) {
            return;
        }
        runtime.reloading = true;
        runtime.readyAtNanos = now + rifle.reloadTicks() * NANOS_PER_TICK;
        runtime.reloadEndsAtTick = Bukkit.getCurrentTick() + rifle.reloadTicks();
        shooter.setCooldown(RIFLE_MATERIAL, rifle.reloadTicks());
        showReloadProgress(shooter, runtime);

        cancelTask(runtime.reloadProgressTask);
        runtime.reloadProgressTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive(shooter, runtime) || !runtime.reloading) {
                cancelTask(runtime.reloadProgressTask);
                runtime.reloadProgressTask = null;
                return;
            }
            showReloadProgress(shooter, runtime);
        }, 2L, 2L);

        long reloadSoundDelay = Math.max(1L, rifle.boltSoundDelayTicks() + 5L);
        runtime.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (isActive(shooter, runtime) && runtime.reloading) {
                playWorld(
                    shooter,
                    settings.get().sounds().reload(),
                    settings.get().sounds().mechanicalVolume(),
                    1.0
                );
            }
        }, reloadSoundDelay));

        runtime.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!isActive(shooter, runtime) || !runtime.reloading) {
                return;
            }
            runtime.magazine.refill();
            runtime.reloading = false;
            runtime.readyAtNanos = 0L;
            runtime.reloadEndsAtTick = 0L;
            cancelTask(runtime.reloadProgressTask);
            runtime.reloadProgressTask = null;
            shooter.setCooldown(RIFLE_MATERIAL, 0);
            updateRifleName(shooter, runtime);
        }, rifle.reloadTicks()));
    }

    private void scheduleBolt(Player shooter, GunRuntime runtime, long delayTicks) {
        cancelTask(runtime.boltTask);
        runtime.boltTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            runtime.boltTask = null;
            if (isActive(shooter, runtime)) {
                playWorld(
                    shooter,
                    settings.get().sounds().bolt(),
                    settings.get().sounds().mechanicalVolume(),
                    settings.get().sounds().boltPitch()
                );
            }
        }, Math.max(1L, delayTicks));
    }

    private boolean isActive(Player player, GunRuntime runtime) {
        return player.isOnline()
            && canOperateRifle(player)
            && runtimes.get(player.getUniqueId()) == runtime;
    }

    private boolean canOperateRifle(Player player) {
        return gameRunning.getAsBoolean()
            && arena.isArena(player)
            && !player.isDead()
            && player.getGameMode() == GameMode.ADVENTURE;
    }

    private void showUnavailable(Player player, GunRuntime runtime) {
        if (runtime.reloading) {
            showReloadProgress(player, runtime);
        }
    }

    private void showReloadProgress(Player player, GunRuntime runtime) {
        long remainingTicks = Math.max(0L, runtime.reloadEndsAtTick - Bukkit.getCurrentTick());
        double remainingSeconds = remainingTicks / 20.0;
        updateRifleName(player, reloadingRifleSuffix(remainingSeconds), NamedTextColor.AQUA);
    }

    private void updateRifleName(Player player, GunRuntime runtime) {
        updateRifleName(
            player,
            Component.text(
                " [" + runtime.magazine.rounds() + "/" + runtime.magazine.capacity() + "]",
                NamedTextColor.GRAY
            ).decoration(TextDecoration.ITALIC, false),
            NamedTextColor.AQUA
        );
    }

    private void updateRifleName(Player player, Component suffix, NamedTextColor nameColor) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isRifle(item)) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            String displayName = meta.getPersistentDataContainer().getOrDefault(
                plugin.rifleDisplayNameKey(),
                PersistentDataType.STRING,
                "저격총"
            );
            meta.displayName(Component.text(displayName, nameColor)
                .decoration(TextDecoration.ITALIC, false)
                .append(suffix));
            item.setItemMeta(meta);
            player.getInventory().setItem(slot, item);
        }
    }

    private static Component rifleName(String displayName, int rounds, int capacity) {
        return Component.text(displayName, NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" [" + rounds + "/" + capacity + "]", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
    }

    private static Component reloadingRifleSuffix(double remainingSeconds) {
        return Component.text(
            String.format(Locale.ROOT, " [재장전 %.1f초]", remainingSeconds),
            NamedTextColor.DARK_GRAY
        ).decoration(TextDecoration.ITALIC, false);
    }

    private void fireHitscan(Player shooter, PluginSettings.RifleSettings rifle, boolean scopedShot) {
        org.bukkit.Location visualStart = shooter.getEyeLocation();
        org.bukkit.Location hitScanStart = visualStart.clone();
        double yawRadians = Math.toRadians(hitScanStart.getYaw());
        Vector horizontalRight = new Vector(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians));
        hitScanStart.add(horizontalRight.multiply(rifle.horizontalAimOffsetBlocks()));
        Vector direction = hitScanStart.getDirection().normalize();
        if (!scopedShot && rifle.unscopedSpread() > 0.0) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            direction = AimSpread.apply(
                direction,
                rifle.unscopedSpread(),
                random.nextDouble(),
                random.nextDouble(0.0, Math.PI * 2.0)
            );
        }
        RayTraceResult result = shooter.getWorld().rayTrace(
            hitScanStart,
            direction,
            rifle.range(),
            FluidCollisionMode.NEVER,
            true,
            rifle.hitboxExpansion(),
            entity -> isValidTarget(shooter, entity)
        );

        Vector hitPosition = result == null
            ? hitScanStart.toVector().add(direction.clone().multiply(rifle.range()))
            : result.getHitPosition();
        double traceDistance = hitPosition.distance(hitScanStart.toVector());
        if (rifle.tracerEnabled()) {
            drawTracer(shooter, visualStart.toVector(), direction, traceDistance, rifle.tracerSpacing());
        }

        if (result == null || !(result.getHitEntity() instanceof Player target)) {
            return;
        }

        HitZone hitZone = hitZone(target, hitPosition, rifle);
        boolean headshot = hitZone == HitZone.HEAD;
        double damage = switch (hitZone) {
            case LEGS -> rifle.legDamage();
            case BODY -> rifle.bodyDamage();
            case HEAD -> rifle.headDamage();
        };
        ShotHit hit = new ShotHit(shooter.getUniqueId(), headshot, System.nanoTime());
        latestHits.put(target.getUniqueId(), hit);
        applyingShotDamage.add(shooter.getUniqueId());
        try {
            target.setNoDamageTicks(0);
            target.damage(damage, shooter);
        } finally {
            applyingShotDamage.remove(shooter.getUniqueId());
        }
        boolean killed = target.isDead();
        if (!killed) {
            latestHits.remove(target.getUniqueId(), hit);
        }
        if (headshot) {
            playPrivate(
                shooter,
                settings.get().sounds().hit(),
                settings.get().sounds().hitVolume(),
                1.35
            );
        }
    }

    private boolean isValidTarget(Player shooter, Entity entity) {
        return entity instanceof Player target
            && target != shooter
            && !target.isDead()
            && !target.isInvulnerable()
            && target.getGameMode() != GameMode.SPECTATOR
            && arena.isArena(target);
    }

    private HitZone hitZone(Player target, Vector hitPosition, PluginSettings.RifleSettings rifle) {
        return HitZone.atHeight(
            hitPosition.getY(),
            target.getBoundingBox().getMinY(),
            target.getBoundingBox().getHeight(),
            rifle.legHeightRatio(),
            rifle.headHeightRatio()
        );
    }

    private void drawTracer(Player shooter, Vector start, Vector direction, double distance, double spacing) {
        Particle.DustTransition dust = new Particle.DustTransition(
            Color.fromRGB(255, 255, 255),
            Color.fromRGB(69, 69, 69),
            1.0f
        );
        Vector point = start.clone().add(direction.clone().multiply(1.75));
        Vector increment = direction.clone().multiply(spacing);
        int sample = 0;
        for (double step = 1.75; step < distance; step += spacing) {
            shooter.getWorld().spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                point.getX(),
                point.getY(),
                point.getZ(),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                dust,
                true
            );
            if ((sample & 1) == 0) {
                shooter.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    point.getX(),
                    point.getY(),
                    point.getZ(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    null,
                    true
                );
                shooter.getWorld().spawnParticle(
                    Particle.MYCELIUM,
                    point.getX(),
                    point.getY(),
                    point.getZ(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.1,
                    null,
                    true
                );
            }
            point.add(increment);
            sample++;
        }
    }

    private void playThrottledFeedback(Player player, long now) {
        UUID id = player.getUniqueId();
        long previous = lastFeedbackNanos.getOrDefault(id, 0L);
        if (now - previous < FEEDBACK_THROTTLE_NANOS) {
            return;
        }
        lastFeedbackNanos.put(id, now);
        playPrivate(
            player,
            settings.get().sounds().empty(),
            settings.get().sounds().mechanicalVolume() * 0.65,
            0.9
        );
    }

    private void playPrivate(Player player, String key, double volume, double pitch) {
        if (key == null || key.isBlank() || volume <= 0.0) {
            return;
        }
        player.playSound(
            player.getLocation(),
            key,
            SoundCategory.PLAYERS,
            (float) volume,
            (float) pitch
        );
    }

    private void playWorld(Player source, String key, double volume, double pitch) {
        if (key == null || key.isBlank() || volume <= 0.0) {
            return;
        }
        source.getWorld().playSound(
            source.getLocation(),
            key,
            SoundCategory.PLAYERS,
            (float) volume,
            (float) pitch
        );
    }

    private void cancelTasks(GunRuntime runtime) {
        if (runtime == null) {
            return;
        }
        for (BukkitTask task : runtime.tasks) {
            task.cancel();
        }
        runtime.tasks.clear();
        cancelTask(runtime.reloadProgressTask);
        runtime.reloadProgressTask = null;
        cancelTask(runtime.boltTask);
        runtime.boltTask = null;
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private ItemStack cloneOrNull(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private static final class GunRuntime {
        private final RifleMagazine magazine;
        private final List<BukkitTask> tasks = new ArrayList<>();
        private long readyAtNanos;
        private long reloadEndsAtTick;
        private boolean reloading;
        private boolean scoped;
        private ItemStack previousHelmet;
        private PotionEffect previousSlowness;
        private BukkitTask reloadProgressTask;
        private BukkitTask boltTask;
        private GunRuntime(int magazineSize) {
            magazine = new RifleMagazine(magazineSize);
        }
    }

    private record ShotHit(UUID shooterId, boolean headshot, long createdAtNanos) {
    }
}
