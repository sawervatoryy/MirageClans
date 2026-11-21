package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.model.TreasuryEntry;
import org.github.sawervatoryy.mirageClans.util.Result;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Клановая казна. Валюты: Vault (деньги) и PlayerPoints (очки).
 * Защита от дюпов: деньги всегда сначала снимаются у источника (атомарно), и только потом зачисляются получателю.
 * Все вызовы — из главного потока (API экономики), сохранение — асинхронное.
 */
public final class TreasuryManager {

    /** Валюта казны. */
    public enum Currency {
        MONEY, POINTS;

        public static Currency parse(String s) {
            if (s == null) {
                return null;
            }
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "money", "vault", "деньги", "монетки" -> MONEY;
                case "points", "point", "pp", "очки", "донат" -> POINTS;
                default -> null;
            };
        }
    }

    private final MirageClans plugin;

    public TreasuryManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    public boolean enabled(Currency cur) {
        return plugin.settings().bool(cur == Currency.MONEY ? "treasury.money-enabled" : "treasury.points-enabled");
    }

    public String currencyName(Currency cur) {
        return plugin.lang().raw(cur == Currency.MONEY ? "treasury.currency-money" : "treasury.currency-points");
    }

    public String format(Currency cur, double amount) {
        return Text.num(amount);
    }

    private double normalize(Currency cur, double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return 0;
        }
        double max = plugin.settings().decimal("treasury.max-amount");
        if (max > 0 && amount > max) {
            return 0;
        }
        return cur == Currency.POINTS ? Math.floor(amount) : Math.round(amount * 100.0) / 100.0;
    }

    /** Пополнение казны игроком. */
    public Result deposit(Player p, Clan c, Currency cur, double rawAmount) {
        if (!c.hasPermission(p.getUniqueId(), ClanPermission.TREASURY_DEPOSIT)) {
            return Result.fail("general.no-permission");
        }
        if (!enabled(cur)) {
            return Result.fail("treasury.disabled");
        }
        double amount = normalize(cur, rawAmount);
        if (amount <= 0) {
            return Result.fail("treasury.bad-amount");
        }
        String cname = currencyName(cur);
        if (cur == Currency.MONEY) {
            if (!plugin.vault().available()) {
                return Result.fail("treasury.no-economy");
            }
            if (!plugin.vault().has(p, amount) || !plugin.vault().withdraw(p, amount)) {
                return Result.fail("treasury.not-enough-player", "currency", cname);
            }
            c.depositMoney(amount);
        } else {
            if (!plugin.points().available()) {
                return Result.fail("treasury.no-economy");
            }
            int pts = (int) amount;
            if (plugin.points().balance(p.getUniqueId()) < pts || !plugin.points().take(p.getUniqueId(), pts)) {
                return Result.fail("treasury.not-enough-player", "currency", cname);
            }
            c.depositPoints(pts);
        }
        plugin.clans().save(c);
        log(c, p.getUniqueId(), p.getName(), cur, "DEPOSIT", amount);
        if (cur == Currency.MONEY) {
            plugin.quests().addProgress(c, QuestType.TREASURY_DEPOSIT, "ANY", (long) Math.floor(amount));
        }
        return Result.ok("treasury.deposited", "amount", format(cur, amount), "currency", cname);
    }

    /** Снятие из казны игроком. */
    public Result withdraw(Player p, Clan c, Currency cur, double rawAmount) {
        if (!c.hasPermission(p.getUniqueId(), ClanPermission.TREASURY_WITHDRAW)) {
            return Result.fail("general.no-permission");
        }
        if (!enabled(cur)) {
            return Result.fail("treasury.disabled");
        }
        double amount = normalize(cur, rawAmount);
        if (amount <= 0) {
            return Result.fail("treasury.bad-amount");
        }
        String cname = currencyName(cur);
        if (cur == Currency.MONEY) {
            if (!plugin.vault().available()) {
                return Result.fail("treasury.no-economy");
            }
            if (!c.withdrawMoney(amount)) {
                return Result.fail("treasury.not-enough-clan", "currency", cname);
            }
            if (!plugin.vault().deposit(p, amount)) {
                c.depositMoney(amount);
                return Result.fail("treasury.economy-error");
            }
        } else {
            if (!plugin.points().available()) {
                return Result.fail("treasury.no-economy");
            }
            int pts = (int) amount;
            if (!c.withdrawPoints(pts)) {
                return Result.fail("treasury.not-enough-clan", "currency", cname);
            }
            if (!plugin.points().give(p.getUniqueId(), pts)) {
                c.depositPoints(pts);
                return Result.fail("treasury.economy-error");
            }
        }
        plugin.clans().save(c);
        log(c, p.getUniqueId(), p.getName(), cur, "WITHDRAW", amount);
        return Result.ok("treasury.withdrawn", "amount", format(cur, amount), "currency", cname);
    }

    /** Системное начисление (награды за квесты). */
    public void reward(Clan c, String source, double money, long points) {
        if (money > 0) {
            c.depositMoney(money);
            log(c, null, source, Currency.MONEY, "QUEST", money);
        }
        if (points > 0) {
            c.depositPoints(points);
            log(c, null, source, Currency.POINTS, "QUEST", points);
        }
        if (money > 0 || points > 0) {
            plugin.clans().save(c);
        }
    }

    private void log(Clan c, java.util.UUID player, String name, Currency cur, String action, double amount) {
        plugin.repository().logTreasury(new TreasuryEntry(c.id(), player, name, cur.name(), action, amount,
                System.currentTimeMillis()));
    }

    /** История операций (загружается асинхронно). */
    public CompletableFuture<List<TreasuryEntry>> history(Clan c) {
        return plugin.repository().loadHistory(c.id(), plugin.settings().integer("treasury.history-size"));
    }
}
