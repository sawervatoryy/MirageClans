package org.github.sawervatoryy.mirageClans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Утилиты текста: &-коды и &#RRGGBB через LegacyComponentSerializer (Adventure).
 */
public final class Text {

    private static final LegacyComponentSerializer AMP =
            LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final LegacyComponentSerializer SECTION =
            LegacyComponentSerializer.builder().character('\u00a7').hexColors()
                    .useUnusualXRepeatedCharacterHexFormat().build();

    private Text() {
    }

    /** Преобразует строку с &-кодами в Component (курсив выключен — удобно для предметов). */
    public static Component color(String s) {
        return AMP.deserialize(s == null ? "" : s).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> colorAll(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String l : lines) {
            out.add(color(l));
        }
        return out;
    }

    /** Строка в «секционном» формате (§), пригодная для PlaceholderAPI. */
    public static String legacy(String amp) {
        return SECTION.serialize(color(amp));
    }

    public static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    /** Убирает все цвета из строки с &-кодами. */
    public static String strip(String amp) {
        return plain(AMP.deserialize(amp == null ? "" : amp));
    }

    /** Заменяет {ключ} значениями из пар kv. */
    public static String apply(String s, String... kv) {
        if (s == null) {
            return "";
        }
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return s;
    }

    public static String num(double v) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(' ');
        sym.setDecimalSeparator('.');
        return new DecimalFormat("#,##0.##", sym).format(v);
    }

    /** Полоска прогресса из символов fill/empty. */
    public static String bar(double fraction, int length, String fill, String empty) {
        double f = Math.max(0, Math.min(1, fraction));
        int filled = (int) Math.round(f * length);
        return fill.repeat(filled) + empty.repeat(length - filled);
    }

    /** Имя цвета (AQUA, GREEN…) → NamedTextColor, по умолчанию WHITE. */
    public static NamedTextColor namedColor(String name) {
        NamedTextColor c = name == null ? null : NamedTextColor.NAMES.value(name.toLowerCase(Locale.ROOT));
        return c == null ? NamedTextColor.WHITE : c;
    }

    /** Код цвета &x для имени цвета. */
    public static String colorCode(String name) {
        if (name == null) {
            return "&f";
        }
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "BLACK" -> "&0";
            case "DARK_BLUE" -> "&1";
            case "DARK_GREEN" -> "&2";
            case "DARK_AQUA" -> "&3";
            case "DARK_RED" -> "&4";
            case "DARK_PURPLE" -> "&5";
            case "GOLD" -> "&6";
            case "GRAY" -> "&7";
            case "DARK_GRAY" -> "&8";
            case "BLUE" -> "&9";
            case "GREEN" -> "&a";
            case "AQUA" -> "&b";
            case "RED" -> "&c";
            case "LIGHT_PURPLE" -> "&d";
            case "YELLOW" -> "&e";
            default -> "&f";
        };
    }
}
