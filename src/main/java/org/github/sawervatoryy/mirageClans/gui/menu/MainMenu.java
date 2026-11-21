package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.manager.ClanManager;
import org.github.sawervatoryy.mirageClans.model.Clan;

/** Главное анимированное меню /clan. */
public final class MainMenu extends Menu {

    public MainMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "main";
    }

    @Override
    protected boolean animated() {
        return true;
    }

    @Override
    protected String[] titleKv() {
        Clan c = plugin.clans().byPlayer(player.getUniqueId());
        return new String[]{"clan", c == null ? plugin.lang().raw("general.no-clan-label") : c.name()};
    }

    @Override
    protected void build() {
        Clan c = plugin.clans().byPlayer(player.getUniqueId());
        if (c == null) {
            buildNoClan();
            return;
        }
        String[] kv = plugin.gui().clanKv(c, player);
        put("info", null, kv);
        put("members", e -> plugin.gui().openMembers(player), kv);
        put("treasury", e -> plugin.gui().openTreasury(player), kv);
        put("storage", e -> plugin.storage().open(player, c), kv);
        put("homes", e -> plugin.gui().openHomes(player), kv);
        put("quests", e -> plugin.gui().openQuests(player), kv);
        put("levels", e -> plugin.gui().openLevels(player), kv);
        put("top", e -> plugin.gui().openTop(player, ClanManager.TopType.LEVEL), kv);
        put("roles", e -> plugin.gui().openRoles(player), kv);
        put("settings", e -> plugin.gui().openSettings(player), kv);
        if (c.isLeader(player.getUniqueId())) {
            put("disband", e -> plugin.gui().confirm(player, plugin.lang().raw("confirm.disband"), () -> {
                Clan cur = plugin.clans().byPlayer(player.getUniqueId());
                if (cur != null && cur.isLeader(player.getUniqueId())) {
                    plugin.clans().disband(cur);
                    plugin.lang().send(player, "disband.success", "clan", cur.name());
                }
                player.closeInventory();
            }, () -> plugin.gui().openMain(player)), kv);
        } else {
            put("leave", e -> plugin.gui().confirm(player, plugin.lang().raw("confirm.leave"), () -> {
                say(plugin.clans().leave(player));
                player.closeInventory();
            }, () -> plugin.gui().openMain(player)), kv);
        }
        put("close", e -> player.closeInventory());
    }

    private void buildNoClan() {
        put("create", e -> plugin.prompts().ask(player, "prompt.clan-name", name ->
                plugin.prompts().ask(player, "prompt.clan-tag", tag -> {
                    var r = plugin.clans().create(player, name.trim(), tag.trim());
                    say(r);
                    if (r.isOk()) {
                        plugin.gui().openMain(player);
                    }
                })));
        put("invites", e -> plugin.gui().openInvites(player),
                "count", String.valueOf(plugin.invites().get(player.getUniqueId()).size()));
        put("nc-top", e -> plugin.gui().openTop(player, ClanManager.TopType.LEVEL));
        put("close", e -> player.closeInventory());
    }
}
