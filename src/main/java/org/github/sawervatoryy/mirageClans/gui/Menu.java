package org.github.sawervatoryy.mirageClans.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.util.Items;
import org.github.sawervatoryy.mirageClans.util.Result;
import org.github.sawervatoryy.mirageClans.util.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Базовое меню: слоты → действия, фон, опциональная анимация открытия «волной» от центра. */
public abstract class Menu implements InventoryHolder {

    protected final MirageClans plugin;
    protected final Player player;
    protected Inventory inventory;
    private final Map<Integer, ClickAction> actions = new HashMap<>();
    private Map<Integer, ItemStack> pending;
    private BukkitTask animTask;

    protected Menu(MirageClans plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /** Ключ меню в menus.yml. */
    protected abstract String menuId();

    /** Заполняет меню предметами (set / put). */
    protected abstract void build();

    /** Плейсхолдеры заголовка. */
    protected String[] titleKv() {
        return new String[0];
    }

    protected boolean animated() {
        return false;
    }

    protected Component title() {
        return Text.color(Text.apply(plugin.menus().title(menuId()), titleKv()));
    }

    // ------------------------------------------------------------------ открытие

    public void open() {
        inventory = Bukkit.createInventory(this, plugin.menus().rows(menuId()) * 9, title());
        boolean anim = animated() && plugin.settings().bool("animation.enabled");
        pending = anim ? new LinkedHashMap<>() : null;
        actions.clear();
        build();
        fillRest();
        player.openInventory(inventory);
        if (anim) {
            runAnimation();
        }
    }

    /** Перестраивает содержимое уже открытого меню. */
    public void refresh() {
        if (animTask != null) {
            animTask.cancel();
            animTask = null;
        }
        pending = null;
        inventory.clear();
        actions.clear();
        build();
        fillRest();
    }

    private void fillRest() {
        ButtonDef f = plugin.menus().filler(menuId());
        if (f == null) {
            return;
        }
        ItemStack filler = Items.build(f);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (pending != null) {
                pending.putIfAbsent(i, filler.clone());
            } else if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler.clone());
            }
        }
    }

    private void runAnimation() {
        int rows = inventory.getSize() / 9;
        int centerRow = (rows - 1) / 2;
        Map<Integer, List<Integer>> rings = new TreeMap<>();
        for (Integer slot : pending.keySet()) {
            int dist = Math.abs(slot / 9 - centerRow) + Math.abs(slot % 9 - 4);
            rings.computeIfAbsent(dist, k -> new ArrayList<>()).add(slot);
        }
        List<List<Integer>> order = new ArrayList<>(rings.values());
        Map<Integer, ItemStack> data = pending;
        pending = null;
        long period = Math.max(1, plugin.settings().integer("animation.ring-delay-ticks"));
        String sound = plugin.settings().str("animation.sound");
        animTask = new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inventory)
                        || i >= order.size()) {
                    cancel();
                    return;
                }
                for (int slot : order.get(i)) {
                    inventory.setItem(slot, data.get(slot));
                }
                if (!sound.isBlank()) {
                    player.playSound(player.getLocation(), sound, 0.4f, 0.8f + i * 0.08f);
                }
                i++;
            }
        }.runTaskTimer(plugin, 0L, period);
    }

    // ------------------------------------------------------------------ построение

    protected void set(int slot, ItemStack item, ClickAction action) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        if (pending != null) {
            pending.put(slot, item);
        } else {
            inventory.setItem(slot, item);
        }
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected ButtonDef btn(String key) {
        return plugin.menus().button(menuId(), key);
    }

    /** Кнопка из menus.yml со слотом из конфига. */
    protected void put(String key, ClickAction action, String... kv) {
        ButtonDef d = btn(key);
        set(d.slot(), Items.build(d, kv), action);
    }

    protected void putStatic(String key, String... kv) {
        put(key, null, kv);
    }

    protected void back(Runnable r) {
        put("back", e -> r.run());
    }

    protected List<Integer> slots(String key) {
        return plugin.menus().slots(menuId(), key);
    }

    /** Текущий клан игрока (null — закрывает меню). */
    protected Clan clan() {
        Clan c = plugin.clans().byPlayer(player.getUniqueId());
        if (c == null) {
            player.closeInventory();
        }
        return c;
    }

    protected void say(Result r) {
        plugin.lang().send(player, r);
    }

    // ------------------------------------------------------------------ события

    public void handleClick(InventoryClickEvent e) {
        ClickAction a = actions.get(e.getSlot());
        ItemStack cur = e.getCurrentItem();
        if (a == null || cur == null || cur.getType().isAir()) {
            return;
        }
        String snd = plugin.settings().str("click-sound");
        if (!snd.isBlank()) {
            player.playSound(player.getLocation(), snd, 0.5f, 1f);
        }
        a.click(e);
    }

    public void onClose() {
        if (animTask != null) {
            animTask.cancel();
            animTask = null;
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
