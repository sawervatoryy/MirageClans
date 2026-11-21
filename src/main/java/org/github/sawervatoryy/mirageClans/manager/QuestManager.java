package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.QuestProgress;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клановые квесты (quests.yml): общий прогресс всех участников, ежедневное/еженедельное обновление,
 * награды — клановый опыт и средства в казну.
 */
public final class QuestManager {

    private final MirageClans plugin;
    private volatile Map<String, QuestDef> defs = new LinkedHashMap<>();
    private volatile Map<QuestType, List<QuestDef>> byType = new EnumMap<>(QuestType.class);
    private volatile ZoneId zone = ZoneId.systemDefault();
    private volatile int resetHour;
    private volatile DayOfWeek weeklyDay = DayOfWeek.MONDAY;
    private volatile long killCooldownMs = 600_000L;
    private final Map<String, Long> lastKills = new ConcurrentHashMap<>();
    private BukkitTask playtimeTask;

    public QuestManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ загрузка

    public void load(YamlConfiguration y) {
        try {
            zone = ZoneId.of(y.getString("reset.timezone", "UTC"));
        } catch (Exception ex) {
            zone = ZoneId.systemDefault();
            plugin.getLogger().warning("quests.yml: неверный часовой пояс, используется системный");
        }
        resetHour = Math.max(0, Math.min(23, y.getInt("reset.hour", 0)));
        try {
            weeklyDay = DayOfWeek.valueOf(y.getString("reset.weekly-day", "MONDAY").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            weeklyDay = DayOfWeek.MONDAY;
        }
        killCooldownMs = Math.max(0, y.getLong("settings.kill-player-same-victim-cooldown-seconds", 600)) * 1000L;

        Map<String, QuestDef> map = new LinkedHashMap<>();
        Map<QuestType, List<QuestDef>> types = new EnumMap<>(QuestType.class);
        ConfigurationSection qs = y.getConfigurationSection("quests");
        if (qs != null) {
            for (String id : qs.getKeys(false)) {
                ConfigurationSection s = qs.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    QuestType type = QuestType.valueOf(s.getString("type", "").toUpperCase(Locale.ROOT));
                    QuestPeriod period = QuestPeriod.valueOf(s.getString("period", "DAILY").toUpperCase(Locale.ROOT));
                    Set<String> targets = new HashSet<>();
                    for (String t : s.getStringList("targets")) {
                        targets.add(t.toUpperCase(Locale.ROOT));
                    }
                    if (s.isString("target")) {
                        targets.add(s.getString("target", "ANY").toUpperCase(Locale.ROOT));
                    }
                    if (targets.isEmpty()) {
                        targets.add("ANY");
                    }
                    Material icon = Material.matchMaterial(s.getString("icon", "PAPER"));
                    QuestDef d = new QuestDef(id, period, type, targets, Math.max(1, s.getLong("amount", 1)),
                            s.getInt("min-level", 1), s.getString("name", id), s.getStringList("description"),
                            icon == null ? Material.PAPER : icon, s.getLong("rewards.xp", 0),
                            s.getDouble("rewards.money", 0), s.getLong("rewards.points", 0));
                    map.put(id, d);
                    types.computeIfAbsent(type, k -> new ArrayList<>()).add(d);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("quests.yml: квест '" + id + "' пропущен (неверный type/period)");
                }
            }
        }
        this.defs = map;
        this.byType = types;
    }

    public void start() {
        playtimeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Clan c = plugin.clans().byPlayer(p.getUniqueId());
                if (c != null) {
                    addProgress(c, QuestType.PLAYTIME, "ANY", 1);
                }
            }
        }, 1200L, 1200L);
    }

    public void stop() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
        }
    }

    // ------------------------------------------------------------------ периоды

    private ZonedDateTime shifted() {
        return ZonedDateTime.now(zone).minusHours(resetHour);
    }

    /** Ключ текущего периода: при смене ключа прогресс сбрасывается. */
    public String periodKey(QuestPeriod p) {
        ZonedDateTime now = shifted();
        return switch (p) {
            case DAILY -> now.toLocalDate().toString();
            case WEEKLY -> now.toLocalDate().with(TemporalAdjusters.previousOrSame(weeklyDay)).toString();
        };
    }

    /** Секунд до ближайшего обновления. */
    public long secondsUntilReset(QuestPeriod p) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.toLocalDate().atTime(resetHour, 0).atZone(zone);
        if (p == QuestPeriod.WEEKLY) {
            next = next.with(TemporalAdjusters.nextOrSame(weeklyDay));
            if (!next.isAfter(now)) {
                next = next.plusWeeks(1);
            }
        } else if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Math.max(0, Duration.between(now, next).getSeconds());
    }

    // ------------------------------------------------------------------ доступ

    public List<QuestDef> definitions(QuestPeriod period) {
        List<QuestDef> list = new ArrayList<>();
        for (QuestDef d : defs.values()) {
            if (d.period() == period) {
                list.add(d);
            }
        }
        return list;
    }

    public List<QuestDef> all() {
        return new ArrayList<>(defs.values());
    }

    /** Прогресс квеста клана с автоматическим сбросом при смене периода. */
    public QuestProgress progress(Clan c, QuestDef d) {
        String key = periodKey(d.period());
        QuestProgress p = c.quests().computeIfAbsent(d.id(), k -> new QuestProgress(key));
        if (!key.equals(p.periodKey())) {
            synchronized (p) {
                if (!key.equals(p.periodKey())) {
                    p.set(0);
                    p.setCompleted(false);
                    p.setPeriodKey(key);
                    plugin.clans().markDirty(c);
                }
            }
        }
        return p;
    }

    /** Количество выполненных в текущем периоде квестов. */
    public int completedCount(Clan c) {
        int n = 0;
        for (QuestDef d : defs.values()) {
            if (progress(c, d).completed()) {
                n++;
            }
        }
        return n;
    }

    /** Защита от фарма: один и тот же убийца/жертва не чаще, чем раз в N секунд. */
    public boolean allowPlayerKill(UUID killer, UUID victim) {
        if (killCooldownMs <= 0) {
            return true;
        }
        String key = killer + ":" + victim;
        long now = System.currentTimeMillis();
        Long last = lastKills.get(key);
        if (last != null && now - last < killCooldownMs) {
            return false;
        }
        lastKills.put(key, now);
        if (lastKills.size() > 5000) {
            lastKills.values().removeIf(t -> now - t > killCooldownMs);
        }
        return true;
    }

    // ------------------------------------------------------------------ прогресс

    /** Добавляет общий прогресс клана всем подходящим квестам. */
    public void addProgress(Clan c, QuestType type, String target, long amount) {
        List<QuestDef> list = byType.get(type);
        if (list == null || amount <= 0) {
            return;
        }
        int level = c.level();
        for (QuestDef d : list) {
            if (level < d.minLevel() || !d.matches(target)) {
                continue;
            }
            QuestProgress p = progress(c, d);
            boolean justCompleted = false;
            synchronized (p) {
                if (p.completed()) {
                    continue;
                }
                long v = p.add(amount);
                if (v >= d.amount()) {
                    p.set(d.amount());
                    p.setCompleted(true);
                    justCompleted = true;
                }
            }
            plugin.clans().markDirty(c);
            if (justCompleted) {
                complete(c, d);
            }
        }
    }

    private void complete(Clan c, QuestDef d) {
        plugin.treasury().reward(c, Text.strip(d.name()), d.rewardMoney(), d.rewardPoints());
        plugin.levels().addXp(c, d.rewardXp());
        plugin.clans().save(c);
        plugin.clans().broadcast(c, "quests.completed", "quest", d.name(), "xp", String.valueOf(d.rewardXp()),
                "money", Text.num(d.rewardMoney()), "points", String.valueOf(d.rewardPoints()));
    }
}
