package org.github.sawervatoryy.mirageClans.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/** Разобранные настройки из config.yml. Редко используемые значения читаются напрямую через {@link #cfg()}. */
public final class Settings {

    /** Параметры уровня клана. */
    public record LevelDef(int level, long xpRequired, double upgradeCost, int maxMembers, int storageSlots,
                           int maxHomes) {
    }

    /** Шаблон роли для новых кланов. */
    public record RoleTemplate(String id, String name, int weight, Set<ClanPermission> permissions) {
    }

    private volatile YamlConfiguration cfg = new YamlConfiguration();
    private volatile boolean useGui = true;
    private volatile boolean autoLevel = true;
    private volatile NavigableMap<Integer, LevelDef> levels = new TreeMap<>();
    private volatile Map<String, RoleTemplate> roleTemplates = new LinkedHashMap<>();
    private volatile String defaultRoleId = "NEWBIE";

    public void load(YamlConfiguration c) {
        this.cfg = c;
        this.useGui = c.getBoolean("use-gui", true);
        this.autoLevel = !"MANUAL".equalsIgnoreCase(c.getString("leveling.mode", "AUTO"));
        this.defaultRoleId = c.getString("roles.default-role", "NEWBIE").toUpperCase(Locale.ROOT);

        TreeMap<Integer, LevelDef> lv = new TreeMap<>();
        ConfigurationSection ls = c.getConfigurationSection("leveling.levels");
        if (ls != null) {
            for (String key : ls.getKeys(false)) {
                int n;
                try {
                    n = Integer.parseInt(key);
                } catch (NumberFormatException ex) {
                    continue;
                }
                ConfigurationSection l = ls.getConfigurationSection(key);
                if (l == null) {
                    continue;
                }
                lv.put(n, new LevelDef(n, l.getLong("xp-required", 0), l.getDouble("upgrade-cost", 0),
                        l.getInt("max-members", 5), Math.min(54, Math.max(0, l.getInt("storage-slots", 9))),
                        l.getInt("max-homes", 1)));
            }
        }
        if (lv.isEmpty()) {
            lv.put(1, new LevelDef(1, 0, 0, 5, 9, 1));
        }
        this.levels = lv;

        Map<String, RoleTemplate> rt = new LinkedHashMap<>();
        ConfigurationSection rs = c.getConfigurationSection("roles.templates");
        if (rs != null) {
            for (String key : rs.getKeys(false)) {
                ConfigurationSection r = rs.getConfigurationSection(key);
                if (r == null) {
                    continue;
                }
                Set<ClanPermission> perms = EnumSet.noneOf(ClanPermission.class);
                for (String p : r.getStringList("permissions")) {
                    ClanPermission cp = ClanPermission.parse(p);
                    if (cp != null) {
                        perms.add(cp);
                    }
                }
                String id = key.toUpperCase(Locale.ROOT);
                rt.put(id, new RoleTemplate(id, r.getString("name", key), r.getInt("weight", 10), perms));
            }
        }
        this.roleTemplates = rt;
    }

    public YamlConfiguration cfg() {
        return cfg;
    }

    public boolean useGui() {
        return useGui;
    }

    public boolean autoLevel() {
        return autoLevel;
    }

    public Map<String, RoleTemplate> roleTemplates() {
        return roleTemplates;
    }

    public String defaultRoleId() {
        return defaultRoleId;
    }

    public int maxLevel() {
        return levels.lastKey();
    }

    public LevelDef level(int lvl) {
        Map.Entry<Integer, LevelDef> e = levels.floorEntry(lvl);
        return e != null ? e.getValue() : levels.firstEntry().getValue();
    }

    /** Опыт, необходимый для перехода с уровня lvl на следующий (0 — максимальный уровень). */
    public long xpRequiredFor(int lvl) {
        if (lvl >= maxLevel()) {
            return 0;
        }
        return level(lvl).xpRequired();
    }

    // Короткие геттеры
    public String str(String path) {
        return cfg.getString(path, "");
    }

    public int integer(String path) {
        return cfg.getInt(path);
    }

    public double decimal(String path) {
        return cfg.getDouble(path);
    }

    public boolean bool(String path) {
        return cfg.getBoolean(path);
    }
}
