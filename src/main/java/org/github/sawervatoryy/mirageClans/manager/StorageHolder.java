package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Держатель инвентаря общего склада клана. */
public final class StorageHolder implements InventoryHolder {

    private final UUID clanId;
    private Inventory inventory;

    public StorageHolder(UUID clanId) {
        this.clanId = clanId;
    }

    public UUID clanId() {
        return clanId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
