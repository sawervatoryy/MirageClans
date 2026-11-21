package org.github.sawervatoryy.mirageClans.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** Точка кланового дома (неизменяемая запись). */
public record ClanHome(String name, String world, double x, double y, double z, float yaw, float pitch) {

    public static ClanHome of(String name, Location l) {
        return new ClanHome(name, l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    /** @return локация или null, если мир не загружен */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
