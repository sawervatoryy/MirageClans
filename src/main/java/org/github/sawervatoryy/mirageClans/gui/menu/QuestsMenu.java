package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.manager.QuestDef;
import org.github.sawervatoryy.mirageClans.manager.QuestPeriod;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.QuestProgress;
import org.github.sawervatoryy.mirageClans.util.Items;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.List;

/** Клановые квесты: вкладки «ежедневные / еженедельные». */
public final class QuestsMenu extends Menu {

    private QuestPeriod period = QuestPeriod.DAILY;

    public QuestsMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "quests";
    }

    @Override
    protected void build() {
        Clan c = clan();
        if (c == null) {
            return;
        }
        String reset = plugin.lang().duration(plugin.quests().secondsUntilReset(period));
        put("tab-daily", e -> {
            period = QuestPeriod.DAILY;
            refresh();
        }, "state", plugin.lang().raw(period == QuestPeriod.DAILY ? "quests.tab-active" : "quests.tab-inactive"));
        put("tab-weekly", e -> {
            period = QuestPeriod.WEEKLY;
            refresh();
        }, "state", plugin.lang().raw(period == QuestPeriod.WEEKLY ? "quests.tab-active" : "quests.tab-inactive"));
        putStatic("reset", "reset", reset);

        List<Integer> slots = slots("quest-slots");
        List<QuestDef> defs = plugin.quests().definitions(period);
        for (int i = 0; i < defs.size() && i < slots.size(); i++) {
            QuestDef d = defs.get(i);
            set(slots.get(i), buildItem(c, d), null);
        }
        back(() -> plugin.gui().openMain(player));
    }

    private org.bukkit.inventory.ItemStack buildItem(Clan c, QuestDef d) {
        if (c.level() < d.minLevel()) {
            ButtonDef locked = new ButtonDef(-1, org.bukkit.Material.BARRIER, d.name(),
                    plugin.lang().rawList("quests.locked-lore"), false);
            return Items.build(locked, "level", String.valueOf(d.minLevel()));
        }
        QuestProgress p = plugin.quests().progress(c, d);
        long value = Math.min(p.value(), d.amount());
        double frac = (double) value / d.amount();
        String status = plugin.lang().raw(p.completed() ? "quests.status-done" : "quests.status-active");
        ButtonDef def = new ButtonDef(-1, d.icon(), d.name(), plugin.lang().rawList("quests.item-lore"), p.completed());
        return Items.build(def,
                "description", String.join("\n", d.description()),
                "bar", Text.bar(frac, 20, plugin.lang().raw("quests.bar-fill"), plugin.lang().raw("quests.bar-empty")),
                "progress", String.valueOf(value), "amount", String.valueOf(d.amount()),
                "percent", String.valueOf((int) (frac * 100)),
                "xp", String.valueOf(d.rewardXp()), "money", Text.num(d.rewardMoney()),
                "points", String.valueOf(d.rewardPoints()), "status", status,
                "reset", plugin.lang().duration(plugin.quests().secondsUntilReset(d.period())));
    }
}
