package org.github.sawervatoryy.mirageClans.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanMember;

/** Вход/выход игроков. */
public final class PlayerListener implements Listener {

    private final MirageClans plugin;

    public PlayerListener(MirageClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.isReady()) {
            return;
        }
        Player p = e.getPlayer();
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        if (c != null) {
            ClanMember m = c.member(p.getUniqueId());
            if (m != null && !m.name().equals(p.getName())) {
                m.setName(p.getName());
                plugin.clans().markDirty(c);
            }
        } else {
            int n = plugin.invites().get(p.getUniqueId()).size();
            if (n > 0) {
                plugin.lang().send(p, "invite.pending", "count", String.valueOf(n));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.chat().untoggle(p.getUniqueId());
        plugin.prompts().cancel(p.getUniqueId());
    }
}
