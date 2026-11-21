package org.github.sawervatoryy.mirageClans.hook;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/** Интеграция с PlayerPoints (донат-валюта). */
public final class PointsHook {

    private PlayerPointsAPI api() {
        Plugin pl = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (pl instanceof PlayerPoints pp && pl.isEnabled()) {
            return pp.getAPI();
        }
        return null;
    }

    public boolean available() {
        return api() != null;
    }

    public int balance(UUID id) {
        PlayerPointsAPI a = api();
        return a == null ? 0 : a.look(id);
    }

    public boolean take(UUID id, int amount) {
        PlayerPointsAPI a = api();
        return a != null && a.take(id, amount);
    }

    public boolean give(UUID id, int amount) {
        PlayerPointsAPI a = api();
        return a != null && a.give(id, amount);
    }
}
