package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.manager.ClanManager.TopType;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.util.Items;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.List;

/** Топ кланов по уровню, очкам или балансу. */
public final class TopMenu extends Menu {

    private TopType type;

    public TopMenu(MirageClans plugin, Player player, TopType type) {
        super(plugin, player);
        this.type = type;
    }

    @Override
    protected String menuId() {
        return "top";
    }

    @Override
    protected void build() {
        for (TopType t : TopType.values()) {
            String key = "tab-" + t.name().toLowerCase();
            put(key, e -> {
                type = t;
                refresh();
            }, "state", plugin.lang().raw(t == type ? "quests.tab-active" : "quests.tab-inactive"));
        }
        List<Integer> slots = slots("top-slots");
        List<Clan> top = plugin.clans().top(type, Math.min(slots.size(), plugin.settings().integer("clan.top-size")));
        ButtonDef def = btn("entry");
        for (int i = 0; i < top.size(); i++) {
            Clan c = top.get(i);
            set(slots.get(i), Items.build(def, "rank", String.valueOf(i + 1), "clan", c.name(), "tag", c.tag(),
                    "level", String.valueOf(c.level()), "points", String.valueOf(c.totalXp()),
                    "balance", Text.num(c.money()), "members", String.valueOf(c.memberCount())), null);
        }
        back(() -> plugin.gui().openMain(player));
    }
}
