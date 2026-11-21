package org.github.sawervatoryy.mirageClans.config;

import org.bukkit.Material;

import java.util.List;

/**
 * Описание кнопки меню из menus.yml.
 *
 * @param slot     слот (-1, если слот задаётся кодом)
 * @param material материал
 * @param name     название (с &-кодами и {плейсхолдерами})
 * @param lore     описание
 * @param glow     свечение
 */
public record ButtonDef(int slot, Material material, String name, List<String> lore, boolean glow) {
}
