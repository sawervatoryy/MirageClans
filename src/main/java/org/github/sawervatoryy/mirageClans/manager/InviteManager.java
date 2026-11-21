package org.github.sawervatoryy.mirageClans.manager;

import org.github.sawervatoryy.mirageClans.MirageClans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Приглашения в кланы (с таймаутом). */
public final class InviteManager {

    /** Приглашение. */
    public record Invite(UUID clanId, UUID inviter, String inviterName, long expiresAt) {
    }

    private final MirageClans plugin;
    private final Map<UUID, List<Invite>> invites = new ConcurrentHashMap<>();

    public InviteManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    public void invite(UUID target, UUID clanId, UUID inviter, String inviterName) {
        long ttl = plugin.settings().integer("clan.invite-expire-seconds") * 1000L;
        List<Invite> list = invites.computeIfAbsent(target, k -> new CopyOnWriteArrayList<>());
        list.removeIf(i -> i.clanId().equals(clanId));
        list.add(new Invite(clanId, inviter, inviterName, System.currentTimeMillis() + Math.max(1000L, ttl)));
    }

    /** Актуальные приглашения игрока (просроченные удаляются). */
    public List<Invite> get(UUID target) {
        List<Invite> list = invites.get(target);
        if (list == null) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        list.removeIf(i -> i.expiresAt() < now || plugin.clans().byId(i.clanId()) == null);
        return new ArrayList<>(list);
    }

    public Invite find(UUID target, UUID clanId) {
        for (Invite i : get(target)) {
            if (i.clanId().equals(clanId)) {
                return i;
            }
        }
        return null;
    }

    public void remove(UUID target, UUID clanId) {
        List<Invite> list = invites.get(target);
        if (list != null) {
            list.removeIf(i -> i.clanId().equals(clanId));
        }
    }

    public void clear(UUID target) {
        invites.remove(target);
    }
}
