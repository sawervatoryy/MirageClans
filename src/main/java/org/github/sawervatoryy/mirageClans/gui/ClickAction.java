package org.github.sawervatoryy.mirageClans.gui;

import org.bukkit.event.inventory.InventoryClickEvent;

/** Действие при клике по слоту меню. */
@FunctionalInterface
public interface ClickAction {
    void click(InventoryClickEvent event);
}
