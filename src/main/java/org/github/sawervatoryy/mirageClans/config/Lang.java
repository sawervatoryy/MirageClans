package org.github.sawervatoryy.mirageClans.config;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.github.sawervatoryy.mirageClans.util.Result;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.List;

/** Доступ к сообщениям lang.yml. Ничего не хардкодим — все тексты берутся отсюда. */
public final class Lang {

    private volatile YamlConfiguration yml = new YamlConfiguration();

    public void load(YamlConfiguration y) {
        this.yml = y;
    }

    /** Сырая строка (с &-кодами). */
    public String raw(String key) {
        String s = yml.getString(key);
        return s == null ? "&cLang missing: " + key : s;
    }

    public List<String> rawList(String key) {
        return yml.getStringList(key);
    }

    public String prefix() {
        return yml.getString("prefix", "");
    }

    /** Сообщение с префиксом. */
    public Component msg(String key, String... kv) {
        return Text.color(prefix() + Text.apply(raw(key), kv));
    }

    /** Сообщение без префикса. */
    public Component plain(String key, String... kv) {
        return Text.color(Text.apply(raw(key), kv));
    }

    /** Строка без префикса с применёнными плейсхолдерами (&-коды остаются). */
    public String str(String key, String... kv) {
        return Text.apply(raw(key), kv);
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(msg(key, kv));
    }

    public void send(CommandSender to, Result r) {
        if (r.key() != null) {
            to.sendMessage(msg(r.key(), r.kv()));
        }
    }

    /** Отправляет список строк (без префикса). */
    public void sendLines(CommandSender to, String key, String... kv) {
        for (String line : rawList(key)) {
            to.sendMessage(Text.color(Text.apply(line, kv)));
        }
    }

    /** Форматирует длительность «1д 2ч 3м 4с». */
    public String duration(long seconds) {
        long s = Math.max(0, seconds);
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) {
            sb.append(Text.apply(raw("time.day"), "n", String.valueOf(d))).append(' ');
        }
        if (h > 0) {
            sb.append(Text.apply(raw("time.hour"), "n", String.valueOf(h))).append(' ');
        }
        if (m > 0) {
            sb.append(Text.apply(raw("time.minute"), "n", String.valueOf(m))).append(' ');
        }
        if (sec > 0 || sb.length() == 0) {
            sb.append(Text.apply(raw("time.second"), "n", String.valueOf(sec)));
        }
        return sb.toString().trim();
    }
}
