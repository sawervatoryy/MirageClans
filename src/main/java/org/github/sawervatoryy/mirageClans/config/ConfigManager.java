package org.github.sawervatoryy.mirageClans.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.github.sawervatoryy.mirageClans.MirageClans;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

/** Загружает и перезагружает config.yml, lang.yml, menus.yml, quests.yml (с подстановкой дефолтов из jar). */
public final class ConfigManager {

    public static final List<String> FILES = List.of("config.yml", "lang.yml", "menus.yml", "quests.yml");

    private final MirageClans plugin;

    public ConfigManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    /** Копирует отсутствующие файлы из jar в папку плагина. */
    public void ensureFiles() {
        for (String name : FILES) {
            File f = new File(plugin.getDataFolder(), name);
            if (!f.exists()) {
                plugin.saveResource(name, false);
            }
        }
    }

    /** Читает YAML-файл; недостающие ключи берутся из дефолтной копии в jar. */
    public YamlConfiguration load(String name) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
        try (InputStream in = plugin.getResource(name)) {
            if (in != null) {
                yml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Не удалось прочитать дефолты " + name, ex);
        }
        return yml;
    }
}
