package org.github.sawervatoryy.mirageClans.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.util.Items;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Общий виртуальный склад клана.
 * <p>
 * Защита от дюпов: у клана ровно ОДИН живой {@link Inventory}, который открывают все участники — игроки физически
 * работают с одними и теми же слотами. Заблокированные слоты (выше лимита уровня) заняты предметом-заглушкой и
 * недоступны для кликов. Сериализация ItemStack делается в главном потоке (это быстро), запись в БД — асинхронно.
 */
public final class StorageManager {

    private static final int SIZE = 54;

    private final MirageClans plugin;
    private final Map<UUID, Inventory> inventories = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final NamespacedKey lockedKey;
    private BukkitTask autosave;

    public StorageManager(MirageClans plugin) {
        this.plugin = plugin;
        this.lockedKey = new NamespacedKey(plugin, "locked_slot");
    }

    /** Запускает периодическое автосохранение. */
    public void start() {
        long period = Math.max(5, plugin.settings().integer("storage.autosave-seconds")) * 20L;
        autosave = Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirty, period, period);
    }

    public void stop() {
        if (autosave != null) {
            autosave.cancel();
        }
    }

    // ------------------------------------------------------------------ открытие

    public void open(Player p, Clan c) {
        if (!c.hasPermission(p.getUniqueId(), ClanPermission.STORAGE)) {
            plugin.lang().send(p, "general.no-permission");
            return;
        }
        Inventory inv = inventories.computeIfAbsent(c.id(), id -> create(c));
        p.openInventory(inv);
    }

    private Inventory create(Clan c) {
        StorageHolder holder = new StorageHolder(c.id());
        Component title = Text.color(Text.apply(plugin.menus().title("storage"), "clan", c.name()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);
        byte[] data = c.storageData();
        if (data.length > 0) {
            try {
                ItemStack[] items = ItemStack.deserializeItemsFromBytes(data);
                int unlocked = unlocked(c);
                for (int i = 0; i < items.length && i < unlocked; i++) {
                    ItemStack it = items[i];
                    if (it != null && !it.getType().isAir()) {
                        inv.setItem(i, it);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось прочитать склад клана " + c.name(), ex);
            }
        }
        applyLocks(c, inv);
        return inv;
    }

    private int unlocked(Clan c) {
        return Math.max(0, Math.min(SIZE, plugin.levels().storageSlots(c)));
    }

    private ItemStack lockedItem() {
        ItemStack it = Items.build(plugin.menus().button("storage", "locked"));
        it.editMeta(m -> m.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1));
        return it;
    }

    private boolean isLockedItem(ItemStack it) {
        return it != null && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(lockedKey, PersistentDataType.BYTE);
    }

    private void applyLocks(Clan c, Inventory inv) {
        int unlocked = unlocked(c);
        ItemStack locked = lockedItem();
        for (int i = 0; i < SIZE; i++) {
            if (i >= unlocked) {
                inv.setItem(i, locked.clone());
            } else if (isLockedItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
    }

    /** Обновляет заглушки после смены уровня. */
    public void refresh(Clan c) {
        Inventory inv = inventories.get(c.id());
        if (inv != null) {
            applyLocks(c, inv);
        }
    }

    // ------------------------------------------------------------------ события инвентаря

    public void handleClick(InventoryClickEvent e, StorageHolder holder) {
        Clan c = plugin.clans().byId(holder.clanId());
        Player p = (Player) e.getWhoClicked();
        if (c == null || !c.hasPermission(p.getUniqueId(), ClanPermission.STORAGE)) {
            e.setCancelled(true);
            p.closeInventory();
            return;
        }
        Inventory top = e.getView().getTopInventory();
        int unlocked = unlocked(c);
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(top) && e.getSlot() >= unlocked) {
            e.setCancelled(true);
            return;
        }
        dirty.add(c.id());
    }

    public void handleDrag(InventoryDragEvent e, StorageHolder holder) {
        Clan c = plugin.clans().byId(holder.clanId());
        if (c == null) {
            e.setCancelled(true);
            return;
        }
        int unlocked = unlocked(c);
        for (int raw : e.getRawSlots()) {
            if (raw < SIZE && raw >= unlocked) {
                e.setCancelled(true);
                return;
            }
        }
        dirty.add(c.id());
    }

    public void handleClose(InventoryCloseEvent e, StorageHolder holder) {
        Clan c = plugin.clans().byId(holder.clanId());
        if (c == null) {
            return;
        }
        dirty.add(c.id());
        // последний зритель закрыл — сохраняем сразу
        if (e.getInventory().getViewers().size() <= 1) {
            save(c);
        }
    }

    // ------------------------------------------------------------------ сохранение

    private void flushDirty() {
        for (UUID id : new ArrayList<>(dirty)) {
            Clan c = plugin.clans().byId(id);
            if (c != null) {
                save(c);
            } else {
                dirty.remove(id);
            }
        }
    }

    /** Сериализует склад (главный поток) и отправляет запись в БД (асинхронно). */
    public void save(Clan c) {
        Inventory inv = inventories.get(c.id());
        dirty.remove(c.id());
        if (inv == null) {
            return;
        }
        int unlocked = unlocked(c);
        ItemStack[] items = new ItemStack[unlocked];
        for (int i = 0; i < unlocked; i++) {
            ItemStack it = inv.getItem(i);
            items[i] = (it == null || isLockedItem(it)) ? null : it;
        }
        try {
            byte[] data = ItemStack.serializeItemsAsBytes(items);
            c.setStorageData(data);
            plugin.repository().saveStorage(c.id(), data);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сериализовать склад клана " + c.name(), ex);
        }
    }

    /** Сохраняет все открытые склады (выключение плагина). */
    public void saveAll() {
        for (UUID id : new ArrayList<>(inventories.keySet())) {
            Clan c = plugin.clans().byId(id);
            if (c != null) {
                save(c);
            }
        }
    }

    public void onDisband(Clan c) {
        Inventory inv = inventories.remove(c.id());
        dirty.remove(c.id());
        if (inv != null) {
            for (HumanEntity v : new ArrayList<>(inv.getViewers())) {
                v.closeInventory();
            }
        }
    }
}
