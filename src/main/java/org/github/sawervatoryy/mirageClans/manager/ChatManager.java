package org.github.sawervatoryy.mirageClans.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Клановый чат: только через /cc (разовое сообщение или режим-переключатель), без префиксов в обычном чате. */
public final class ChatManager {

    private final MirageClans plugin;
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();

    public ChatManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    public boolean isToggled(UUID id) {
        return toggled.contains(id);
    }

    public void untoggle(UUID id) {
        toggled.remove(id);
    }

    /** Переключает режим кланового чата. @return новое состояние */
    public boolean toggle(UUID id) {
        if (!toggled.add(id)) {
            toggled.remove(id);
            return false;
        }
        return true;
    }

    /** Отправляет сообщение всем участникам клана, находящимся онлайн. */
    public void send(Player sender, String message) {
        Clan clan = plugin.clans().byPlayer(sender.getUniqueId());
        if (clan == null) {
            toggled.remove(sender.getUniqueId());
            plugin.lang().send(sender, "general.no-clan");
            return;
        }
        ClanRole role = clan.roleOf(sender.getUniqueId());
        String format = plugin.settings().str("chat.format");
        String roleName = role == null ? "" : plugin.clans().roleName(role);
        format = Text.apply(format, "tag", clan.tag(), "clan", clan.name(), "role", roleName,
                "player", sender.getName());
        int idx = format.indexOf("{message}");
        String before = idx < 0 ? format : format.substring(0, idx);
        String after = idx < 0 ? "" : format.substring(idx + "{message}".length());
        Component line = Component.empty().append(Text.color(before)).append(Component.text(message))
                .append(Text.color(after));
        for (UUID u : clan.memberIds()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(line);
            }
        }
        if (plugin.settings().bool("chat.log-to-console")) {
            plugin.getLogger().info("[ClanChat][" + clan.name() + "] " + sender.getName() + ": " + message);
        }
    }
}
