package org.github.sawervatoryy.mirageClans.model;

import java.util.UUID;

/** Участник клана. */
public final class ClanMember {

    private final UUID uuid;
    private volatile String name;
    private volatile String roleId;
    private final long joinedAt;

    public ClanMember(UUID uuid, String name, String roleId, long joinedAt) {
        this.uuid = uuid;
        this.name = name;
        this.roleId = roleId;
        this.joinedAt = joinedAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String roleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public long joinedAt() {
        return joinedAt;
    }
}
