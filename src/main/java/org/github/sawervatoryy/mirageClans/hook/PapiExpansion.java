package org.github.sawervatoryy.mirageClans.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.model.GlowMode;
import org.github.sawervatoryy.mirageClans.util.Text;
import org.jetbrains.annotations.NotNull;

/**
 * Плейсхолдеры %mirageclans_*%:
 * name, tag, role, leader, level, xp, xp_needed, xp_left, xp_percent, balance, points,
 * members_online, members_total, members_max, friendly_fire, homes, has_clan.
 * Relational: %rel_mirageclans_color%, %rel_mirageclans_same%.
 */
public final class PapiExpansion extends PlaceholderExpansion implements Relational {

    private final MirageClans plugin;

    public PapiExpansion(MirageClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mirageclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "sawervatoryy";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !plugin.isReady()) {
            return "";
        }
        String id = params.toLowerCase();
        Clan c = plugin.clans().byPlayer(player.getUniqueId());
        if (id.equals("has_clan")) {
            return String.valueOf(c != null);
        }
        if (c == null) {
            return Text.legacy(plugin.settings().cfg().getString("placeholders.no-clan." + id, ""));
        }
        switch (id) {
            case "name":
                return c.name();
            case "tag":
                return c.tag();
            case "role": {
                ClanRole r = c.roleOf(player.getUniqueId());
                return r == null ? "" : Text.legacy(plugin.clans().roleName(r));
            }
            case "leader": {
                var m = c.member(c.leader());
                return m == null ? "" : m.name();
            }
            case "level":
                return String.valueOf(c.level());
            case "xp":
                return String.valueOf(c.xp());
            case "xp_needed":
                return String.valueOf(plugin.levels().xpRequired(c));
            case "xp_left":
                return String.valueOf(Math.max(0, plugin.levels().xpRequired(c) - c.xp()));
            case "xp_percent": {
                long req = plugin.levels().xpRequired(c);
                return req <= 0 ? "100" : String.valueOf(Math.min(100, (int) (c.xp() * 100 / req)));
            }
            case "balance":
                return Text.num(c.money());
            case "points":
                return String.valueOf(c.points());
            case "members_online":
                return String.valueOf(plugin.clans().onlineCount(c));
            case "members_total":
                return String.valueOf(c.memberCount());
            case "members_max":
                return String.valueOf(plugin.levels().maxMembers(c));
            case "friendly_fire":
                return Text.legacy(plugin.lang().raw(c.friendlyFire() ? "placeholders.on" : "placeholders.off"));
            case "homes":
                return String.valueOf(c.homes().size());
            default:
                return null;
        }
    }

    @Override
    public String onPlaceholderRequest(Player one, Player two, @NotNull String identifier) {
        if (one == null || two == null || !plugin.isReady()) {
            return "";
        }
        Clan a = plugin.clans().byPlayer(one.getUniqueId());
        Clan b = plugin.clans().byPlayer(two.getUniqueId());
        boolean same = a != null && a == b;
        switch (identifier.toLowerCase()) {
            case "same":
                return String.valueOf(same);
            case "color":
                if (same && a.glowMode() == GlowMode.NAME_COLOR) {
                    return Text.colorCode(a.glowColor()).replace('&', '\u00a7');
                }
                return "";
            default:
                return null;
        }
    }
}
