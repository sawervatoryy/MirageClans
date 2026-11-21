package org.github.sawervatoryy.mirageClans.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;

/** /cc <сообщение> — сообщение клану; /cc — переключение режима кланового чата. */
public final class ClanChatCommand implements CommandExecutor {

    private final MirageClans plugin;

    public ClanChatCommand(MirageClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.lang().send(sender, "general.players-only");
            return true;
        }
        if (!plugin.isReady()) {
            plugin.lang().send(p, "general.loading");
            return true;
        }
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        if (c == null) {
            plugin.lang().send(p, "general.no-clan");
            return true;
        }
        if (args.length == 0) {
            plugin.lang().send(p, plugin.chat().toggle(p.getUniqueId()) ? "chat.enabled" : "chat.disabled");
        } else {
            plugin.chat().send(p, String.join(" ", args));
        }
        return true;
    }
}
