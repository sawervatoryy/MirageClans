package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.manager.TreasuryManager.Currency;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.util.Items;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.List;

/** Казна: баланс, быстрые кнопки (ЛКМ — внести, ПКМ — снять), произвольная сумма, история. */
public final class TreasuryMenu extends Menu {

    public TreasuryMenu(MirageClans plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String menuId() {
        return "treasury";
    }

    @Override
    protected void build() {
        Clan c = clan();
        if (c == null) {
            return;
        }
        put("money-info", null, "balance", Text.num(c.money()));
        put("points-info", null, "balance", String.valueOf(c.points()));
        fill(c, Currency.MONEY, "money-slots", "money-amount", "money-custom");
        fill(c, Currency.POINTS, "points-slots", "points-amount", "points-custom");
        put("history", e -> plugin.gui().openHistory(player));
        back(() -> plugin.gui().openMain(player));
    }

    private void fill(Clan c, Currency cur, String slotsKey, String amountKey, String customKey) {
        List<Integer> slots = slots(slotsKey);
        List<Double> amounts = plugin.settings().cfg().getDoubleList(
                cur == Currency.MONEY ? "treasury.quick-amounts.money" : "treasury.quick-amounts.points");
        ButtonDef def = btn(amountKey);
        for (int i = 0; i < slots.size() && i < amounts.size(); i++) {
            double amount = amounts.get(i);
            set(slots.get(i), Items.build(def, "amount", Text.num(amount)), e -> {
                Clan cur2 = clan();
                if (cur2 == null) {
                    return;
                }
                say(e.isLeftClick() ? plugin.treasury().deposit(player, cur2, cur, amount)
                        : plugin.treasury().withdraw(player, cur2, cur, amount));
                refresh();
            });
        }
        put(customKey, e -> {
            boolean deposit = e.isLeftClick();
            plugin.prompts().ask(player, deposit ? "prompt.amount-deposit" : "prompt.amount-withdraw", text -> {
                Clan cur2 = plugin.clans().byPlayer(player.getUniqueId());
                double v;
                try {
                    v = Double.parseDouble(text.trim().replace(',', '.'));
                } catch (NumberFormatException ex) {
                    plugin.lang().send(player, "treasury.bad-amount");
                    plugin.gui().openTreasury(player);
                    return;
                }
                if (cur2 != null) {
                    say(deposit ? plugin.treasury().deposit(player, cur2, cur, v)
                            : plugin.treasury().withdraw(player, cur2, cur, v));
                }
                plugin.gui().openTreasury(player);
            });
        });
    }
}
