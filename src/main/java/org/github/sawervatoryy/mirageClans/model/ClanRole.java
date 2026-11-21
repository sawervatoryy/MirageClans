package org.github.sawervatoryy.mirageClans.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Роль внутри клана. Лидер — встроенная роль со всеми правами и неизменяемая. */
public final class ClanRole {

    public static final String LEADER_ID = "LEADER";
    public static final int LEADER_WEIGHT = 1000;

    private final String id;
    private volatile String displayName;
    private volatile int weight;
    private final Set<ClanPermission> permissions = ConcurrentHashMap.newKeySet();

    public ClanRole(String id, String displayName, int weight) {
        this.id = id;
        this.displayName = displayName;
        this.weight = weight;
    }

    public static ClanRole leader() {
        return new ClanRole(LEADER_ID, "Leader", LEADER_WEIGHT);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int weight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isLeader() {
        return LEADER_ID.equals(id);
    }

    public boolean has(ClanPermission p) {
        return isLeader() || permissions.contains(p);
    }

    public Set<ClanPermission> permissions() {
        return permissions;
    }

    /** Переключает право. @return новое состояние */
    public boolean toggle(ClanPermission p) {
        if (!permissions.add(p)) {
            permissions.remove(p);
            return false;
        }
        return true;
    }
}
