package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.Settings;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.util.Result;
import org.github.sawervatoryy.mirageClans.util.Text;

/** Меню уровней и улучшений клана. */
public final class LevelsMenu extends Menu {

    public LevelsMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "levels";
    }

    @Override
    protected void build() {
        Clan c = clan();
        if (c == null) {
            return;
        }
        String[] kv = plugin.gui().clanKv(c, player);
        put("current", null, kv);
        if (plugin.levels().isMax(c)) {
            put("max", null, kv);
        } else {
            Settings.LevelDef next = plugin.settings().level(c.level() + 1);
            Settings.LevelDef cur = plugin.levels().def(c);
            String[] nkv = {"next_level", String.valueOf(next.level()),
                    "next_members", String.valueOf(next.maxMembers()), "next_storage", String.valueOf(next.storageSlots()),
                    "next_homes", String.valueOf(next.maxHomes()), "cost", Text.num(cur.upgradeCost())};
            put("next", null, concat(kv, nkv));
            if (plugin.settings().autoLevel()) {
                put("upgrade-auto", null, concat(kv, nkv));
            } else {
                put("upgrade", e -> {
                    Clan cc = clan();
                    if (cc != null) {
                        Result r = plugin.levels().upgrade(cc, player);
                        say(r);
                        refresh();
                    }
                }, concat(kv, nkv));
            }
        }
        back(() -> plugin.gui().openMain(player));
    }

    private static String[] concat(String[] a, String[] b) {
        String[] out = new String[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
