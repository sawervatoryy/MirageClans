package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.util.Items;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.List;
import java.util.Locale;

/** Матрица прав одной роли: каждое право — переключатель. */
public final class RoleEditorMenu extends Menu {

    private final String roleId;

    public RoleEditorMenu(MirageClans plugin, Player player, String roleId) {
        super(plugin, player);
        this.roleId = roleId;
    }

    @Override
    protected String menuId() {
        return "role-editor";
    }

    @Override
    protected String[] titleKv() {
        Clan c = plugin.clans().byPlayer(player.getUniqueId());
        ClanRole r = c == null ? null : c.role(roleId);
        return new String[]{"role", r == null ? roleId : Text.strip(plugin.clans().roleName(r))};
    }

    @Override
    protected void build() {
        Clan c = clan();
        ClanRole role = c == null ? null : c.role(roleId);
        if (role == null) {
            plugin.gui().openRoles(player);
            return;
        }
        String[] rkv = {"role", plugin.clans().roleName(role), "weight", String.valueOf(role.weight())};
        put("info", null, rkv);
        List<Integer> slots = slots("perm-slots");
        ClanPermission[] perms = ClanPermission.values();
        for (int i = 0; i < perms.length && i < slots.size(); i++) {
            ClanPermission p = perms[i];
            boolean on = role.has(p);
            Material mat = Material.matchMaterial(plugin.menus().string("permissions." + p.name() + ".icon", "PAPER"));
            String name = plugin.menus().string("permissions." + p.name() + ".name", p.name().toLowerCase(Locale.ROOT));
            List<String> lore = plugin.lang().rawList(on ? "role-editor.perm-lore-on" : "role-editor.perm-lore-off");
            ButtonDef d = new ButtonDef(-1, mat == null ? Material.PAPER : mat, name, lore, on);
            set(slots.get(i), Items.build(d), e -> {
                Clan cur = clan();
                if (cur != null) {
                    say(plugin.clans().togglePermission(cur, player, roleId, p));
                    refresh();
                }
            });
        }
        put("weight", e -> {
            Clan cur = clan();
            if (cur != null) {
                say(plugin.clans().shiftWeight(cur, player, roleId, e.isLeftClick() ? 5 : -5));
                refresh();
            }
        }, rkv);
        put("rename", e -> plugin.prompts().ask(player, "prompt.role-rename", name -> {
            Clan cur = plugin.clans().byPlayer(player.getUniqueId());
            if (cur != null) {
                say(plugin.clans().renameRole(cur, player, roleId, name.trim()));
            }
            plugin.gui().openRoleEditor(player, roleId);
        }));
        put("delete", e -> plugin.gui().confirm(player, plugin.lang().str("confirm.delete-role", "role",
                plugin.clans().roleName(role)), () -> {
            Clan cur = clan();
            if (cur != null) {
                say(plugin.clans().deleteRole(cur, player, roleId));
            }
            plugin.gui().openRoles(player);
        }, () -> plugin.gui().openRoleEditor(player, roleId)));
        back(() -> plugin.gui().openRoles(player));
    }
}
