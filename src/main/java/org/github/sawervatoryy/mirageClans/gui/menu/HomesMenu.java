package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanHome;
import org.github.sawervatoryy.mirageClans.util.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Клановые дома: ЛКМ — телепорт, Shift+ПКМ — удалить. */
public final class HomesMenu extends Menu {

    public HomesMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "homes";
    }

    @Override
    protected void build() {
        Clan c = clan();
        if (c == null) {
            return;
        }
        List<ClanHome> homes = new ArrayList<>(c.homes().values());
        homes.sort(Comparator.comparing(ClanHome::name));
        List<Integer> slots = slots("home-slots");
        ButtonDef def = btn("home");
        for (int i = 0; i < homes.size() && i < slots.size(); i++) {
            ClanHome h = homes.get(i);
            set(slots.get(i), Items.build(def, "home", h.name(), "world", h.world(),
                    "x", String.valueOf((int) h.x()), "y", String.valueOf((int) h.y()), "z", String.valueOf((int) h.z())),
                    e -> {
                        Clan cur = clan();
                        if (cur == null) {
                            return;
                        }
                        if (e.getClick() == ClickType.SHIFT_RIGHT) {
                            say(plugin.homes().delete(player, cur, h.name()));
                            refresh();
                        } else if (e.isLeftClick()) {
                            player.closeInventory();
                            say(plugin.homes().teleport(player, cur, h.name()));
                        }
                    });
        }
        put("set", e -> plugin.prompts().ask(player, "prompt.home-name", name -> {
            Clan cur = plugin.clans().byPlayer(player.getUniqueId());
            if (cur != null) {
                say(plugin.homes().set(player, cur, name.trim()));
            }
            plugin.gui().openHomes(player);
        }), "count", String.valueOf(c.homes().size()), "max", String.valueOf(plugin.levels().maxHomes(c)));
        back(() -> plugin.gui().openMain(player));
    }
}
