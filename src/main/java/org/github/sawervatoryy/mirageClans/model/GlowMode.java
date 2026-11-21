package org.github.sawervatoryy.mirageClans.model;

import java.util.Locale;

/** Режим подсветки соклановцев. */
public enum GlowMode {
    /** Выключено. */
    OFF,
    /** Контурное свечение (виден только соклановцам, нужен ProtocolLib). */
    GLOW,
    /** Цветные ники. */
    NAME_COLOR;

    public GlowMode next() {
        GlowMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static GlowMode parse(String s) {
        if (s == null) {
            return OFF;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return OFF;
        }
    }
}
