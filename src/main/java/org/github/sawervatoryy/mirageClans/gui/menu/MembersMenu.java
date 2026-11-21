package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanMember;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.util.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Список участников: ЛКМ — повысить, ПКМ — понизить, Shift+ЛКМ — исключить, Shift+ПКМ — передать лидерство. */
public final class MembersMenu extends Menu {

    private static final int PER_PAGE = 45;
    private int page;

    public MembersMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "members";
    }

    @Override
    protected void build() {
        Clan c = clan();
        if (c == null) {
            return;
        }
        List<ClanMember> list = new ArrayList<>(c.members());
        list.sort(Comparator.comparingInt((ClanMember m) -> -c.weightOf(m.uuid())).thenComparing(ClanMember::name));
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        ButtonDef def = btn("member");
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) {
                break;
            }
            ClanMember m = list.get(idx);
            ClanRole role = c.roleOf(m.uuid());
            boolean online = Bukkit.getPlayer(m.uuid()) != null;
            set(i, Items.head(def, Bukkit.getOfflinePlayer(m.uuid()), "player", m.name(),
                    "role", role == null ? "-" : plugin.clans().roleName(role),
                    "status", plugin.lang().raw(online ? "members.online" : "members.offline")),
                    e -> onMemberClick(e.getClick(), m.uuid(), m.name()));
        }
        put("prev", e -> {
            if (page > 0) {
                page--;
                refresh();
            }
        });
        put("next", e -> {
            if (page < pages - 1) {
                page++;
                refresh();
            }
        });
        put("invite", e -> {
            Clan cur = clan();
            if (cur != null && cur.hasPermission(player.getUniqueId(), ClanPermission.INVITE)) {
                plugin.prompts().ask(player, "prompt.invite-name", name -> {
                    Clan cc = plugin.clans().byPlayer(player.getUniqueId());
                    if (cc != null) {
                        say(plugin.clans().invite(cc, player, name.trim()));
                    }
                    plugin.gui().openMembers(player);
                });
            } else {
                plugin.lang().send(player, "general.no-permission");
            }
        });
        back(() -> plugin.gui().openMain(player));
    }

    private void onMemberClick(ClickType click, UUID target, String name) {
        Clan c = clan();
        if (c == null) {
            return;
        }
        switch (click) {
            case LEFT -> {
                say(plugin.clans().shiftRole(c, player, target, 1));
                refresh();
            }
            case RIGHT -> {
                say(plugin.clans().shiftRole(c, player, target, -1));
                refresh();
            }
            case SHIFT_LEFT -> plugin.gui().confirm(player, plugin.lang().str("confirm.kick", "player", name), () -> {
                Clan cur = clan();
                if (cur != null) {
                    say(plugin.clans().kick(cur, player, target));
                }
                plugin.gui().openMembers(player);
            }, () -> plugin.gui().openMembers(player));
            case SHIFT_RIGHT -> plugin.gui().confirm(player, plugin.lang().str("confirm.transfer", "player", name), () -> {
                Clan cur = clan();
                if (cur != null) {
                    say(plugin.clans().transfer(cur, player, target));
                }
                plugin.gui().openMembers(player);
            }, () -> plugin.gui().openMembers(player));
            default -> {
            }
        }
    }
}
