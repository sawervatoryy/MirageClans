package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.manager.TreasuryManager.Currency;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.TreasuryEntry;
import org.github.sawervatoryy.mirageClans.util.Items;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** История операций казны. Данные грузятся асинхронно из БД. */
public final class HistoryMenu extends Menu {

    private volatile List<TreasuryEntry> entries;

    public HistoryMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "history";
    }

    @Override
    public void open() {
        super.open();
        Clan c = plugin.clans().byPlayer(player.getUniqueId());
        if (c == null) {
            return;
        }
        plugin.treasury().history(c).whenComplete((list, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            entries = err == null ? list : List.of();
            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inventory)) {
                refresh();
            }
        }));
    }

    @Override
    protected void build() {
        back(() -> plugin.gui().openTreasury(player));
        if (entries == null) {
            putStatic("loading");
            return;
        }
        if (entries.isEmpty()) {
            putStatic("empty");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm");
        ButtonDef def = btn("entry");
        for (int i = 0; i < entries.size() && i < 45; i++) {
            TreasuryEntry t = entries.get(i);
            Currency cur = Currency.valueOf(t.currency());
            set(i, Items.build(def, "player", t.playerName(),
                    "action", plugin.lang().raw("history.actions." + t.action()),
                    "amount", Text.num(t.amount()), "currency", plugin.treasury().currencyName(cur),
                    "time", fmt.format(new Date(t.timestamp()))), null);
        }
    }
}
