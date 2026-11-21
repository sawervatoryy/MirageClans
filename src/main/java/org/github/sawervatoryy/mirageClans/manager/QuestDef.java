package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Описание квеста из quests.yml. */
public record QuestDef(String id, QuestPeriod period, QuestType type, Set<String> targets, long amount,
                       int minLevel, String name, List<String> description, Material icon,
                       long rewardXp, double rewardMoney, long rewardPoints) {

    /** Подходит ли цель (тип моба / материал) под квест. */
    public boolean matches(String target) {
        return targets.contains("ANY") || (target != null && targets.contains(target.toUpperCase(Locale.ROOT)));
    }
}
