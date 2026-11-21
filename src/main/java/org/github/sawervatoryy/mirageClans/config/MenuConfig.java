package org.github.sawervatoryy.mirageClans.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.github.sawervatoryy.mirageClans.MirageClans;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Доступ к menus.yml: заголовки, размеры, кнопки, фон. */
public final class MenuConfig {

    private final MirageClans plugin;
    private volatile YamlConfiguration yml = new YamlConfiguration();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public MenuConfig(MirageClans plugin) {
        this.plugin = plugin;
    }

    public void load(YamlConfiguration y) {
        this.yml = y;
        warned.clear();
    }

    public String title(String menu) {
        return yml.getString("menus." + menu + ".title", "&f" + menu);
    }

    public int rows(String menu) {
        return Math.max(1, Math.min(6, yml.getInt("menus." + menu + ".rows", 3)));
    }

    /** Список слотов (для динамических элементов: участники, дома, квесты). */
    public List<Integer> slots(String menu, String key) {
        return yml.getIntegerList("menus." + menu + "." + key);
    }

    /** Кнопка меню; если нет в menus.<menu>.items — ищется в common. */
    public ButtonDef button(String menu, String key) {
        ConfigurationSection s = yml.getConfigurationSection("menus." + menu + ".items." + key);
        if (s == null) {
            s = yml.getConfigurationSection("common." + key);
        }
        if (s == null) {
            if (warned.add(menu + "." + key)) {
                plugin.getLogger().warning("menus.yml: нет кнопки '" + key + "' в меню '" + menu + "'");
            }
            return new ButtonDef(-1, Material.BARRIER, "&cmissing: " + menu + "." + key, List.of(), false);
        }
        return parse(s);
    }

    /** Предмет-фон меню или null, если фон отключён. */
    public ButtonDef filler(String menu) {
        ConfigurationSection s = yml.getConfigurationSection("menus." + menu + ".filler");
        if (s == null) {
            s = yml.getConfigurationSection("filler");
        }
        if (s == null) {
            return null;
        }
        ButtonDef d = parse(s);
        return d.material() == Material.AIR ? null : d;
    }

    private ButtonDef parse(ConfigurationSection s) {
        Material m = Material.matchMaterial(s.getString("material", "STONE").toUpperCase(Locale.ROOT));
        if (m == null) {
            m = Material.STONE;
        }
        return new ButtonDef(s.getInt("slot", -1), m, s.getString("name", " "), s.getStringList("lore"),
                s.getBoolean("glow", false));
    }

    /** Строка из menus.yml по пути (для подписей, не относящихся к кнопкам). */
    public String string(String path, String def) {
        return yml.getString(path, def);
    }
}
