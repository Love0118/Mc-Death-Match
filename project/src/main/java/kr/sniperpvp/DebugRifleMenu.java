package kr.sniperpvp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

final class DebugRifleMenu implements Listener {
    private static final int INVENTORY_SIZE = 54;
    private static final int MODELS_PER_PAGE = 45;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int PAGE_INDICATOR_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private final SniperPvpPlugin plugin;
    private final GunService gun;
    private final Set<UUID> pendingSelections = new HashSet<>();

    DebugRifleMenu(SniperPvpPlugin plugin, GunService gun) {
        this.plugin = plugin;
        this.gun = gun;
    }

    void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int requestedPage) {
        List<PluginSettings.RifleModelSettings> models = plugin.settings().debugRifleModels();
        int pageCount = Math.max(1, (models.size() + MODELS_PER_PAGE - 1) / MODELS_PER_PAGE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        MenuHolder holder = new MenuHolder(page, pageCount);
        Inventory inventory = Bukkit.createInventory(
            holder,
            INVENTORY_SIZE,
            Component.text("총기 모델 선택 ", NamedTextColor.DARK_GRAY)
                .append(Component.text((page + 1) + "/" + pageCount, NamedTextColor.GRAY))
        );
        holder.attach(inventory);

        int start = page * MODELS_PER_PAGE;
        int end = Math.min(start + MODELS_PER_PAGE, models.size());
        for (int modelIndex = start; modelIndex < end; modelIndex++) {
            PluginSettings.RifleModelSettings model = models.get(modelIndex);
            int slot = modelIndex - start;
            inventory.setItem(slot, menuIcon(model));
            holder.modelsBySlot.put(slot, model);
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, navigationItem(Material.ARROW, "이전 페이지"));
        }
        inventory.setItem(
            PAGE_INDICATOR_SLOT,
            navigationItem(Material.BOOK, "모델 " + models.size() + "개 · " + (page + 1) + "/" + pageCount)
        );
        if (page + 1 < pageCount) {
            inventory.setItem(NEXT_PAGE_SLOT, navigationItem(Material.ARROW, "다음 페이지"));
        }
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (slot == PREVIOUS_PAGE_SLOT && holder.page > 0) {
            scheduleOpen(player, holder.page - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT && holder.page + 1 < holder.pageCount) {
            scheduleOpen(player, holder.page + 1);
            return;
        }
        PluginSettings.RifleModelSettings model = holder.modelsBySlot.get(slot);
        if (model == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!pendingSelections.add(playerId)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                giveSelectedModel(player, model);
            } finally {
                pendingSelections.remove(playerId);
            }
        });
    }

    @EventHandler(ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack menuIcon(PluginSettings.RifleModelSettings model) {
        ItemStack icon = gun.createRifle(model);
        ItemMeta meta = icon.getItemMeta();
        meta.lore(List.of(
            Component.text("클릭하여 이 모델 받기", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("아이템 모델: " + model.itemModel(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("경기 상태와 능력치는 변경하지 않습니다", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    private void scheduleOpen(Player player, int page) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                open(player, page);
            }
        });
    }

    private void giveSelectedModel(Player player, PluginSettings.RifleModelSettings model) {
        if (!player.isOnline()) {
            return;
        }
        int inventorySlot = firstSafeSlot(player);
        if (inventorySlot < 0) {
            player.sendMessage(Component.text("빈 인벤토리 칸이 없어 총을 지급할 수 없습니다.", NamedTextColor.RED));
            return;
        }
        player.getInventory().setItem(inventorySlot, gun.createRifle(model));
        if (inventorySlot < 9) {
            player.getInventory().setHeldItemSlot(inventorySlot);
        }
        player.closeInventory();
        player.sendMessage(Component.text(model.displayName() + " 모델을 지급했습니다.", NamedTextColor.GREEN));
    }

    private static ItemStack navigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static int firstSafeSlot(Player player) {
        int heldSlot = player.getInventory().getHeldItemSlot();
        ItemStack held = player.getInventory().getItem(heldSlot);
        if (held == null || held.getType().isAir()) {
            return heldSlot;
        }
        return player.getInventory().firstEmpty();
    }

    private static final class MenuHolder implements InventoryHolder {
        private final int page;
        private final int pageCount;
        private final Map<Integer, PluginSettings.RifleModelSettings> modelsBySlot = new HashMap<>();
        private Inventory inventory;

        private MenuHolder(int page, int pageCount) {
            this.page = page;
            this.pageCount = pageCount;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Debug rifle menu inventory has not been attached");
            }
            return inventory;
        }
    }
}
