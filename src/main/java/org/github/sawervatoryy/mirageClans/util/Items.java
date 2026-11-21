package org.github.sawervatoryy.mirageClans.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.github.sawervatoryy.mirageClans.config.ButtonDef;

import java.util.ArrayList;
import java.util.List;

/** Фабрика предметов для меню. */
public final class Items {

    private Items() {
    }

    /** Строит предмет из описания кнопки; в name/lore заменяются {плейсхолдеры}, "\n" разбивает строку лора. */
    public static ItemStack build(ButtonDef d, String... kv) {
        ItemStack item = new ItemStack(d.material());
        applyMeta(item, Text.apply(d.name(), kv), expand(d.lore(), kv), d.glow());
        return item;
    }

    /** Голова игрока на основе описания кнопки. */
    public static ItemStack head(ButtonDef d, OfflinePlayer owner, String... kv) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        applyMeta(item, Text.apply(d.name(), kv), expand(d.lore(), kv), d.glow());
        item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(owner));
        return item;
    }

    private static List<String> expand(List<String> lore, String... kv) {
        List<String> out = new ArrayList<>();
        for (String line : lore) {
            String replaced = Text.apply(line, kv);
            for (String part : replaced.split("\n", -1)) {
                out.add(part);
            }
        }
        return out;
    }

    private static void applyMeta(ItemStack item, String name, List<String> lore, boolean glow) {
        item.editMeta(meta -> {
            meta.displayName(Text.color(name));
            List<Component> l = Text.colorAll(lore);
            meta.lore(l.isEmpty() ? null : l);
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
            }
            meta.addItemFlags(ItemFlag.values());
        });
    }
}
