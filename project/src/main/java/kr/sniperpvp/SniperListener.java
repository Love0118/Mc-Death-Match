package kr.sniperpvp;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import kr.sniperpvp.arena.ArenaService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class SniperListener implements Listener {
    private final SniperPvpPlugin plugin;
    private final ArenaService arena;
    private final GameManager game;
    private final GunService gun;

    SniperListener(
        SniperPvpPlugin plugin,
        ArenaService arena,
        GameManager game,
        GunService gun
    ) {
        this.plugin = plugin;
        this.arena = arena;
        this.game = game;
        this.gun = gun;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            boolean teleport = plugin.settings().arena().teleportOnJoin();
            game.handleJoin(event.getPlayer(), teleport);
        });
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (game.isRunning() && arena.isArena(event.getPlayer())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> game.handleArenaEntry(event.getPlayer()));
        } else if (arena.isArena(event.getFrom())) {
            game.handleArenaExit(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!arena.isArena(event.getPlayer())) {
            return;
        }
        game.prepareRespawn(event.getPlayer());
        if (arena.world() != null) {
            event.setRespawnLocation(arena.world().getSpawnLocation());
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> game.finishRespawn(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        game.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        // A resource-pack request emits ACCEPTED/DOWNLOADED before it is fully applied, and
        // Paper may emit additional lifecycle statuses later. Once this required pack has
        // loaded, keep that state for the player's connection instead of treating every
        // non-success status as an unload.
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            game.setResourcePackLoaded(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onArmSwing(PlayerArmSwingEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && gun.tryFire(event.getPlayer())) {
            game.markCombat(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !gun.isRifle(event.getItem())) {
            return;
        }
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            event.getPlayer().clearActiveItem();
            gun.toggleZoom(event.getPlayer(), event.getItem());
            return;
        }
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (gun.tryFire(event.getPlayer())) {
                game.markCombat(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onHeldSlotChanged(PlayerItemHeldEvent event) {
        ItemStack nextItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (!gun.isRifle(nextItem)) {
            gun.endZoom(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !arena.isArena(victim) || !game.isRunning()) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity
            && byEntity.getDamager() instanceof Player damager) {
            if (gun.isApplyingShotDamage(damager)) {
                game.markCombat(victim);
                game.markCombat(damager);
                return;
            }
            event.setCancelled(true);
            if (gun.isRifle(damager.getInventory().getItemInMainHand())) {
                if (gun.tryFire(damager)) {
                    game.markCombat(damager);
                }
            }
            return;
        }

        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            game.rescueFromVoid(victim);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        if (!arena.isArena(victim) || !game.isRunning()) {
            return;
        }
        Player killer = victim.getKiller();
        boolean headshot = gun.consumeHeadshot(victim, killer);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);
        game.handleDeath(victim, killer, headshot);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (arena.isArena(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (arena.isArena(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        if (arena.isArena(event.getPlayer()) && game.isRunning()) {
            boolean rifle = gun.isRifle(event.getItemDrop().getItemStack());
            event.setCancelled(true);
            if (rifle) {
                gun.tryReloadFromDrop(event.getPlayer(), event.getItemDrop().getItemStack());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (arena.isArena(event.getPlayer()) && game.isRunning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && arena.isArena(player) && game.isRunning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && arena.isArena(player)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setExhaustion(0.0f);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player && arena.isArena(player) && game.isRunning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (arena.isArena(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (arena.isArena(event.getLocation().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent event) {
        if (arena.isArena(event.getWorld()) && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }
}
