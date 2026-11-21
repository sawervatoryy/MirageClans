package org.github.sawervatoryy.mirageClans.model;

import java.util.Locale;

/** Права, которые можно выдавать ролям клана (матрица прав в GUI). */
public enum ClanPermission {
    TREASURY_DEPOSIT,
    TREASURY_WITHDRAW,
    STORAGE,
    HOME_TELEPORT,
    HOME_MANAGE,
    KICK,
    INVITE,
    PVP_MANAGE,
    MANAGE_ROLES,
    UPGRADE,
    SETTINGS;

    /** @return право по имени или null */
    public static ClanPermission parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
