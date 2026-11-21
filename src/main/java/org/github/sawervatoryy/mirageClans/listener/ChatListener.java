package org.github.sawervatoryy.mirageClans.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.github.sawervatoryy.mirageClans.MirageClans;

import java.util.function.Consumer;

/** Перехват чата: ввод значений для GUI и режим кланового чата (/cc без аргументов). */
public final class ChatListener implements Listener {

    private final MirageClans plugin;

    public ChatListener(MirageClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        String msg = PlainTextComponentSerializer.plainText().serialize(e.message());
        Consumer<String> handler = plugin.prompts().take(p.getUniqueId());
        if (handler != null) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("отмена")) {
                    plugin.lang().send(p, "prompt.cancelled");
                } else {
                    handler.accept(msg);
                }
            });
            return;
        }
        if (plugin.chat().isToggled(p.getUniqueId())) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.chat().send(p, msg));
        }
    }
}
