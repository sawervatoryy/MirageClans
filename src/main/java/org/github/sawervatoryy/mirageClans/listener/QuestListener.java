package org.github.sawervatoryy.mirageClans.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.manager.QuestType;
import org.github.sawervatoryy.mirageClans.model.Clan;

/** Источники прогресса клановых квестов: убийства, добыча блоков. */
public final class QuestListener implements Listener {

    private final MirageClans plugin;

    public QuestListener(MirageClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || !plugin.isReady()) {
            return;
        }
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        if (c != null) {
            plugin.quests().addProgress(c, QuestType.BREAK_BLOCK, e.getBlock().getType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player || !plugin.isReady()) {
            return;
        }
        Player killer = e.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        Clan c = plugin.clans().byPlayer(killer.getUniqueId());
        if (c != null) {
            plugin.quests().addProgress(c, QuestType.KILL_MOB, e.getEntityType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || !plugin.isReady()) {
            return;
        }
        Clan c = plugin.clans().byPlayer(killer.getUniqueId());
        if (c == null || c.isMember(victim.getUniqueId())) {
            return;
        }
        if (plugin.quests().allowPlayerKill(killer.getUniqueId(), victim.getUniqueId())) {
            plugin.quests().addProgress(c, QuestType.KILL_PLAYER, "PLAYER", 1);
        }
    }
}
