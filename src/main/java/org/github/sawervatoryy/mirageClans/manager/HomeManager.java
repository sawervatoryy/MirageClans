package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanHome;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.util.Result;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Клановые дома: установка, удаление, телепортация с задержкой. */
public final class HomeManager {

    private final MirageClans plugin;
    private final Set<UUID> teleporting = ConcurrentHashMap.newKeySet();

    public HomeManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    public Result set(Player p, Clan c, String name) {
        if (!c.hasPermission(p.getUniqueId(), ClanPermission.HOME_MANAGE)) {
            return Result.fail("general.no-permission");
        }
        try {
            if (!name.matches(plugin.settings().cfg().getString("homes.name-regex", "^[A-Za-z0-9_]{1,16}$"))) {
                return Result.fail("homes.bad-name");
            }
        } catch (java.util.regex.PatternSyntaxException ignored) {
            // некорректный regex в конфиге — пропускаем проверку
        }
        if (plugin.settings().cfg().getStringList("homes.blacklisted-worlds").contains(p.getWorld().getName())) {
            return Result.fail("homes.bad-world");
        }
        String key = name.toLowerCase(Locale.ROOT);
        boolean replacing = c.homes().containsKey(key);
        if (!replacing && c.homes().size() >= plugin.levels().maxHomes(c)) {
            return Result.fail("homes.limit", "max", String.valueOf(plugin.levels().maxHomes(c)));
        }
        c.homes().put(key, ClanHome.of(name, p.getLocation()));
        plugin.clans().save(c);
        return Result.ok("homes.set", "home", name);
    }

    public Result delete(Player p, Clan c, String name) {
        if (!c.hasPermission(p.getUniqueId(), ClanPermission.HOME_MANAGE)) {
            return Result.fail("general.no-permission");
        }
        ClanHome removed = c.homes().remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return Result.fail("homes.not-found", "home", name);
        }
        plugin.clans().save(c);
        return Result.ok("homes.deleted", "home", removed.name());
    }

    /** Телепортация с задержкой (отменяется при движении). */
    public Result teleport(Player p, Clan c, String name) {
        if (!c.hasPermission(p.getUniqueId(), ClanPermission.HOME_TELEPORT)) {
            return Result.fail("general.no-permission");
        }
        ClanHome home = c.homes().get(name.toLowerCase(Locale.ROOT));
        if (home == null) {
            return Result.fail("homes.not-found", "home", name);
        }
        Location target = home.toLocation();
        if (target == null) {
            return Result.fail("homes.world-missing");
        }
        if (!teleporting.add(p.getUniqueId())) {
            return Result.fail("homes.already-teleporting");
        }
        int delay = plugin.settings().integer("homes.teleport-delay-seconds");
        boolean cancelOnMove = plugin.settings().bool("homes.cancel-on-move");
        if (delay <= 0) {
            teleporting.remove(p.getUniqueId());
            p.teleportAsync(target);
            return Result.ok("homes.teleported", "home", home.name());
        }
        Location start = p.getLocation();
        UUID id = p.getUniqueId();
        String homeName = home.name();
        new BukkitRunnable() {
            int left = delay;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    teleporting.remove(id);
                    cancel();
                    return;
                }
                if (cancelOnMove && moved(start, p.getLocation())) {
                    teleporting.remove(id);
                    plugin.lang().send(p, "homes.cancelled");
                    cancel();
                    return;
                }
                if (left <= 0) {
                    teleporting.remove(id);
                    cancel();
                    p.teleportAsync(target);
                    plugin.lang().send(p, "homes.teleported", "home", homeName);
                    return;
                }
                plugin.lang().send(p, "homes.teleporting", "seconds", String.valueOf(left), "home", homeName);
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return Result.ok();
    }

    private boolean moved(Location a, Location b) {
        return a.getWorld() != b.getWorld() || a.getBlockX() != b.getBlockX() || a.getBlockY() != b.getBlockY()
                || a.getBlockZ() != b.getBlockZ();
    }
}
