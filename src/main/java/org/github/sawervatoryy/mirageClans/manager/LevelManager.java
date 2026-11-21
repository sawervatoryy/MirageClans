package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.Settings;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.util.Result;
import org.github.sawervatoryy.mirageClans.util.Text;

/** Уровни и опыт клана: лимиты участников/склада/домов и повышение уровня. */
public final class LevelManager {

    private final MirageClans plugin;

    public LevelManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    public Settings.LevelDef def(Clan c) {
        return plugin.settings().level(c.level());
    }

    public int maxMembers(Clan c) {
        return def(c).maxMembers();
    }

    public int storageSlots(Clan c) {
        return def(c).storageSlots();
    }

    public int maxHomes(Clan c) {
        return def(c).maxHomes();
    }

    /** Опыт для перехода на следующий уровень (0 — максимум). */
    public long xpRequired(Clan c) {
        return plugin.settings().xpRequiredFor(c.level());
    }

    public boolean isMax(Clan c) {
        return c.level() >= plugin.settings().maxLevel();
    }

    /** Начисляет клановый опыт (потокобезопасно). */
    public void addXp(Clan c, long amount) {
        if (amount <= 0) {
            return;
        }
        Settings s = plugin.settings();
        int gained = c.gainXp(amount, s.autoLevel(), lvl -> s.xpRequiredFor((int) lvl));
        if (gained > 0) {
            onLevelUp(c);
        } else {
            plugin.clans().markDirty(c);
        }
    }

    private void onLevelUp(Clan c) {
        plugin.storage().refresh(c);
        plugin.clans().save(c);
        plugin.clans().broadcast(c, "levels.level-up", "level", String.valueOf(c.level()));
    }

    /** Ручное повышение уровня (режим MANUAL). */
    public Result upgrade(Clan c, Player p) {
        if (!c.hasPermission(p.getUniqueId(), ClanPermission.UPGRADE)) {
            return Result.fail("general.no-permission");
        }
        if (plugin.settings().autoLevel()) {
            return Result.fail("levels.auto-mode");
        }
        if (isMax(c)) {
            return Result.fail("levels.max");
        }
        long req = xpRequired(c);
        double cost = def(c).upgradeCost();
        if (!c.tryUpgrade(req, cost)) {
            return Result.fail("levels.not-enough", "xp", String.valueOf(req), "cost", Text.num(cost));
        }
        onLevelUp(c);
        return Result.ok();
    }
}
