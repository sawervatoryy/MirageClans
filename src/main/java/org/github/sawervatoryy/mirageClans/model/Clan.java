package org.github.sawervatoryy.mirageClans.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongUnaryOperator;

/**
 * Клан. Все изменяемые «денежные» значения (казна, опыт, уровень) защищены монитором {@link #lock()},
 * коллекции — потокобезопасные. Объект живёт в памяти, в БД он пишется асинхронно.
 */
public final class Clan {

    private final UUID id;
    private final long createdAt;
    private final Object lock = new Object();

    private volatile String name;
    private volatile String tag;
    private volatile UUID leader;
    private volatile boolean friendlyFire;
    private volatile GlowMode glowMode = GlowMode.OFF;
    private volatile String glowColor = "AQUA";
    private volatile byte[] storageData = new byte[0];

    // защищено lock
    private int level = 1;
    private long xp;
    private long totalXp;
    private double money;
    private long points;

    private final Map<UUID, ClanMember> members = new ConcurrentHashMap<>();
    private final Map<String, ClanRole> roles = new ConcurrentHashMap<>();
    private final Map<String, ClanHome> homes = new ConcurrentHashMap<>();
    private final Map<String, QuestProgress> quests = new ConcurrentHashMap<>();

    private final AtomicBoolean saveQueued = new AtomicBoolean();
    private volatile boolean dirty;

    public Clan(UUID id, String name, String tag, UUID leader, long createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.createdAt = createdAt;
        roles.put(ClanRole.LEADER_ID, ClanRole.leader());
    }

    /** Снимок числовых данных клана, согласованный между собой. */
    public record Core(int level, long xp, long totalXp, double money, long points) {
    }

    // ---------- Основное ----------

    public UUID id() {
        return id;
    }

    public long createdAt() {
        return createdAt;
    }

    public Object lock() {
        return lock;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String tag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID leader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public boolean friendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public GlowMode glowMode() {
        return glowMode;
    }

    public void setGlowMode(GlowMode glowMode) {
        this.glowMode = glowMode;
    }

    public String glowColor() {
        return glowColor;
    }

    public void setGlowColor(String glowColor) {
        this.glowColor = glowColor;
    }

    public byte[] storageData() {
        return storageData;
    }

    public void setStorageData(byte[] storageData) {
        this.storageData = storageData == null ? new byte[0] : storageData;
    }

    // ---------- Числа ----------

    public Core core() {
        synchronized (lock) {
            return new Core(level, xp, totalXp, money, points);
        }
    }

    public void restoreCore(int level, long xp, long totalXp, double money, long points) {
        synchronized (lock) {
            this.level = level;
            this.xp = xp;
            this.totalXp = totalXp;
            this.money = money;
            this.points = points;
        }
    }

    public int level() {
        synchronized (lock) {
            return level;
        }
    }

    public long xp() {
        synchronized (lock) {
            return xp;
        }
    }

    public long totalXp() {
        synchronized (lock) {
            return totalXp;
        }
    }

    public double money() {
        synchronized (lock) {
            return money;
        }
    }

    public long points() {
        synchronized (lock) {
            return points;
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public void depositMoney(double amount) {
        synchronized (lock) {
            money = round(money + amount);
        }
    }

    /** Атомарно снимает деньги. @return false, если средств не хватает */
    public boolean withdrawMoney(double amount) {
        synchronized (lock) {
            if (money + 1e-9 < amount) {
                return false;
            }
            money = round(money - amount);
            return true;
        }
    }

    public void depositPoints(long amount) {
        synchronized (lock) {
            points += amount;
        }
    }

    /** Атомарно снимает очки донат-валюты. @return false, если не хватает */
    public boolean withdrawPoints(long amount) {
        synchronized (lock) {
            if (points < amount) {
                return false;
            }
            points -= amount;
            return true;
        }
    }

    /**
     * Добавляет опыт. При авто-режиме повышает уровень, пока хватает опыта.
     *
     * @param requiredFor функция «уровень → опыт для перехода на следующий» (0 = максимальный уровень)
     * @return сколько уровней получено
     */
    public int gainXp(long amount, boolean autoLevel, LongUnaryOperator requiredFor) {
        synchronized (lock) {
            xp += amount;
            totalXp += amount;
            int gained = 0;
            if (autoLevel) {
                while (true) {
                    long req = requiredFor.applyAsLong(level);
                    if (req <= 0 || xp < req) {
                        break;
                    }
                    xp -= req;
                    level++;
                    gained++;
                }
            }
            return gained;
        }
    }

    /** Ручное повышение уровня: списывает опыт и деньги атомарно. */
    public boolean tryUpgrade(long xpRequired, double cost) {
        synchronized (lock) {
            if (xpRequired <= 0 || xp < xpRequired || money + 1e-9 < cost) {
                return false;
            }
            xp -= xpRequired;
            money = round(money - cost);
            level++;
            return true;
        }
    }

    // ---------- Участники и роли ----------

    public Collection<ClanMember> members() {
        return Collections.unmodifiableCollection(members.values());
    }

    public Set<UUID> memberIds() {
        return Collections.unmodifiableSet(members.keySet());
    }

    public ClanMember member(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public int memberCount() {
        return members.size();
    }

    public void addMember(ClanMember m) {
        members.put(m.uuid(), m);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return uuid.equals(leader);
    }

    public Map<String, ClanRole> roles() {
        return roles;
    }

    public ClanRole role(String id) {
        return id == null ? null : roles.get(id);
    }

    /** Роль участника (если роль удалена — самая младшая). */
    public ClanRole roleOf(UUID uuid) {
        ClanMember m = members.get(uuid);
        if (m == null) {
            return null;
        }
        ClanRole r = roles.get(m.roleId());
        return r != null ? r : lowestRole();
    }

    public ClanRole lowestRole() {
        return roles.values().stream().min(Comparator.comparingInt(ClanRole::weight)).orElse(null);
    }

    public int weightOf(UUID uuid) {
        ClanRole r = roleOf(uuid);
        return r == null ? -1 : r.weight();
    }

    public boolean hasPermission(UUID uuid, ClanPermission p) {
        ClanRole r = roleOf(uuid);
        return r != null && r.has(p);
    }

    /** Роли по убыванию веса (лидер первый). */
    public List<ClanRole> rolesSorted() {
        List<ClanRole> list = new ArrayList<>(roles.values());
        list.sort(Comparator.comparingInt(ClanRole::weight).reversed());
        return list;
    }

    // ---------- Дома ----------

    public Map<String, ClanHome> homes() {
        return homes;
    }

    // ---------- Квесты ----------

    public Map<String, QuestProgress> quests() {
        return quests;
    }

    // ---------- Сохранение ----------

    public AtomicBoolean saveQueued() {
        return saveQueued;
    }

    public boolean dirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
