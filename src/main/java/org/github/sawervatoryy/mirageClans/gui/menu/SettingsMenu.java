package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.List;

/** Настройки клана: дружеский огонь, режим и цвет подсветки. */
public final class SettingsMenu extends Menu {

    public SettingsMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "settings";
    }

    @Override
    protected void build() {
        Clan c = clan();
        if (c == null) {
            return;
        }
        put("friendly-fire", e -> {
            Clan cur = clan();
            if (cur != null) {
                say(plugin.clans().setFriendlyFire(cur, player, !cur.friendlyFire()));
                refresh();
            }
        }, "state", plugin.lang().raw(c.friendlyFire() ? "placeholders.on" : "placeholders.off"));
        put("glow-mode", e -> {
            Clan cur = clan();
            if (cur != null) {
                say(plugin.clans().setGlowMode(cur, player, cur.glowMode().next()));
                refresh();
            }
        }, "mode", plugin.lang().raw("glow-modes." + c.glowMode().name()));
        put("glow-color", e -> {
            Clan cur = clan();
            if (cur == null) {
                return;
            }
            List<String> colors = plugin.settings().cfg().getStringList("glow.colors");
            if (colors.isEmpty()) {
                return;
            }
            int idx = colors.indexOf(cur.glowColor());
            String next = colors.get((idx + 1) % colors.size());
            say(plugin.clans().setGlowColor(cur, player, next));
            refresh();
        }, "color", Text.colorCode(c.glowColor()) + c.glowColor());
        back(() -> plugin.gui().openMain(player));
    }
}
