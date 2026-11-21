package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.util.Items;

import java.util.List;

/** Список ролей клана; клик открывает редактор прав. */
public final class RolesMenu extends Menu {

    public RolesMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "roles";
    }

    @Override
    protected void build() {
        Clan c = clan();
        if (c == null) {
            return;
        }
        List<Integer> slots = slots("role-slots");
        List<ClanRole> roles = c.rolesSorted();
        ButtonDef def = btn("role");
        for (int i = 0; i < roles.size() && i < slots.size(); i++) {
            ClanRole r = roles.get(i);
            long count = c.members().stream().filter(m -> m.roleId().equals(r.id())).count();
            set(slots.get(i), Items.build(def, "role", plugin.clans().roleName(r), "weight", String.valueOf(r.weight()),
                    "members", String.valueOf(count),
                    "perms", r.isLeader() ? plugin.lang().raw("roles.all-perms") : String.valueOf(r.permissions().size())),
                    e -> {
                        if (r.isLeader()) {
                            plugin.lang().send(player, "role.leader-fixed");
                        } else {
                            plugin.gui().openRoleEditor(player, r.id());
                        }
                    });
        }
        put("create", e -> plugin.prompts().ask(player, "prompt.role-name", name -> {
            Clan cur = plugin.clans().byPlayer(player.getUniqueId());
            if (cur != null) {
                say(plugin.clans().createRole(cur, player, name.trim()));
            }
            plugin.gui().openRoles(player);
        }));
        back(() -> plugin.gui().openMain(player));
    }
}
