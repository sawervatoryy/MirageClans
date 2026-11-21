package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.manager.InviteManager.Invite;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.util.Items;

import java.util.List;

/** Входящие приглашения: ЛКМ — принять, ПКМ — отклонить. */
public final class InvitesMenu extends Menu {

    public InvitesMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "invites";
    }

    @Override
    protected void build() {
        List<Invite> list = plugin.invites().get(player.getUniqueId());
        List<Integer> slots = slots("invite-slots");
        ButtonDef def = btn("invite");
        for (int i = 0; i < list.size() && i < slots.size(); i++) {
            Invite inv = list.get(i);
            Clan c = plugin.clans().byId(inv.clanId());
            if (c == null) {
                continue;
            }
            long left = Math.max(0, (inv.expiresAt() - System.currentTimeMillis()) / 1000);
            set(slots.get(i), Items.build(def, "clan", c.name(), "tag", c.tag(), "inviter", inv.inviterName(),
                    "expires", plugin.lang().duration(left), "members", String.valueOf(c.memberCount())), e -> {
                if (e.isLeftClick()) {
                    var r = plugin.clans().accept(player, c.id());
                    say(r);
                    if (r.isOk()) {
                        plugin.gui().openMain(player);
                        return;
                    }
                } else {
                    say(plugin.clans().deny(player, c.id()));
                }
                refresh();
            });
        }
        back(() -> plugin.gui().openMain(player));
    }
}
