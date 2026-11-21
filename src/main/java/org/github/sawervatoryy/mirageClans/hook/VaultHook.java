package org.github.sawervatoryy.mirageClans.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Интеграция с Vault (основная валюта). Провайдер экономики ищется лениво — он может зарегистрироваться позже. */
public final class VaultHook {

    private volatile Economy economy;

    private Economy economy() {
        Economy e = economy;
        if (e == null && Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                e = rsp.getProvider();
                economy = e;
            }
        }
        return e;
    }

    public boolean available() {
        return economy() != null;
    }

    public double balance(OfflinePlayer p) {
        Economy e = economy();
        return e == null ? 0 : e.getBalance(p);
    }

    public boolean has(OfflinePlayer p, double amount) {
        Economy e = economy();
        return e != null && e.has(p, amount);
    }

    /** Снимает деньги у игрока. @return true при успехе */
    public boolean withdraw(OfflinePlayer p, double amount) {
        Economy e = economy();
        if (e == null) {
            return false;
        }
        EconomyResponse r = e.withdrawPlayer(p, amount);
        return r.transactionSuccess();
    }

    /** Выдаёт деньги игроку. @return true при успехе */
    public boolean deposit(OfflinePlayer p, double amount) {
        Economy e = economy();
        if (e == null) {
            return false;
        }
        EconomyResponse r = e.depositPlayer(p, amount);
        return r.transactionSuccess();
    }
}
