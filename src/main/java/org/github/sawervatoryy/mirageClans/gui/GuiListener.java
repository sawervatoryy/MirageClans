package org.github.sawervatoryy.mirageClans.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.manager.StorageHolder;

/** Маршрутизация событий инвентарей: меню плагина и общий склад. */
public final class GuiListener implements Listener {

    private final MirageClans plugin;

    public GuiListener(MirageClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryHolder holder = top.getHolder(false);
        if (holder instanceof StorageHolder sh) {
            plugin.storage().handleClick(e, sh);
        } else if (holder instanceof Menu menu) {
            e.setCancelled(true);
            if (e.getClickedInventory() != null && e.getClickedInventory().equals(top)) {
                menu.handleClick(e);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder holder = e.getView().getTopInventory().getHolder(false);
        if (holder instanceof StorageHolder sh) {
            plugin.storage().handleDrag(e, sh);
        } else if (holder instanceof Menu) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        InventoryHolder holder = e.getInventory().getHolder(false);
        if (holder instanceof StorageHolder sh) {
            plugin.storage().handleClose(e, sh);
        } else if (holder instanceof Menu menu) {
            menu.onClose();
        }
    }
}
