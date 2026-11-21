package org.github.sawervatoryy.mirageClans.gui;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.gui.menu.ConfirmMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.HistoryMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.HomesMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.InvitesMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.LevelsMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.MainMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.MembersMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.QuestsMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.RoleEditorMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.RolesMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.SettingsMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.TopMenu;
import org.github.sawervatoryy.mirageClans.gui.menu.TreasuryMenu;
import org.github.sawervatoryy.mirageClans.manager.ClanManager;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.util.Text;

/** Фабрика меню + общие плейсхолдеры клана для menus.yml. */
public final class GuiManager {

    private final MirageClans plugin;

    public GuiManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.settings().useGui();
    }

    public void openMain(Player p) {
        new MainMenu(plugin, p).open();
    }

    public void openMembers(Player p) {
        new MembersMenu(plugin, p).open();
    }

    public void openRoles(Player p) {
        new RolesMenu(plugin, p).open();
    }

    public void openRoleEditor(Player p, String roleId) {
        new RoleEditorMenu(plugin, p, roleId).open();
    }

    public void openTreasury(Player p) {
        new TreasuryMenu(plugin, p).open();
    }

    public void openHistory(Player p) {
        new HistoryMenu(plugin, p).open();
    }

    public void openHomes(Player p) {
        new HomesMenu(plugin, p).open();
    }

    public void openQuests(Player p) {
        new QuestsMenu(plugin, p).open();
    }

    public void openLevels(Player p) {
        new LevelsMenu(plugin, p).open();
    }

    public void openTop(Player p, ClanManager.TopType type) {
        new TopMenu(plugin, p, type).open();
    }

    public void openSettings(Player p) {
        new SettingsMenu(plugin, p).open();
    }

    public void openInvites(Player p) {
        new InvitesMenu(plugin, p).open();
    }

    public void confirm(Player p, String text, Runnable onConfirm, Runnable onCancel) {
        new ConfirmMenu(plugin, p, text, onConfirm, onCancel).open();
    }

    /** Общие плейсхолдеры клана для названий/описаний кнопок. */
    public String[] clanKv(Clan c, Player viewer) {
        long req = plugin.levels().xpRequired(c);
        ClanRole role = viewer == null ? null : c.roleOf(viewer.getUniqueId());
        var leader = c.member(c.leader());
        double frac = req <= 0 ? 1.0 : (double) c.xp() / req;
        return new String[]{
                "clan", c.name(),
                "tag", c.tag(),
                "level", String.valueOf(c.level()),
                "xp", String.valueOf(c.xp()),
                "xp_needed", req <= 0 ? plugin.lang().raw("levels.max-label") : String.valueOf(req),
                "xp_bar", xpBar(frac),
                "members", String.valueOf(c.memberCount()),
                "max_members", String.valueOf(plugin.levels().maxMembers(c)),
                "online", String.valueOf(plugin.clans().onlineCount(c)),
                "leader", leader == null ? "-" : leader.name(),
                "balance", Text.num(c.money()),
                "points", String.valueOf(c.points()),
                "role", role == null ? "-" : plugin.clans().roleName(role),
                "ff", plugin.lang().raw(c.friendlyFire() ? "placeholders.on" : "placeholders.off"),
                "homes", String.valueOf(c.homes().size()),
                "max_homes", String.valueOf(plugin.levels().maxHomes(c)),
                "storage_slots", String.valueOf(plugin.levels().storageSlots(c))
        };
    }

    private String xpBar(double frac) {
        double f = Math.max(0, Math.min(1, frac));
        int n = (int) Math.round(f * 20);
        return plugin.lang().raw("levels.bar-fill-color") + "|".repeat(n)
                + plugin.lang().raw("levels.bar-empty-color") + "|".repeat(20 - n);
    }
}
