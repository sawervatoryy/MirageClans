package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.hook.ProtocolGlow;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.GlowMode;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Подсветка соклановцев.
 * GLOW — пакетное свечение (только для соклановцев, нужен ProtocolLib);
 * NAME_COLOR — цветные ники через команду scoreboard (видна всем) и relational-плейсхолдер
 * %rel_mirageclans_color% (персонально для зрителя, для TAB).
 */
public final class GlowManager {

    private final MirageClans plugin;
    private ProtocolGlow protocolGlow;
    private BukkitTask task;
    private final Map<UUID, Team> teams = new ConcurrentHashMap<>();
    private boolean warned;

    public GlowManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    public boolean packetGlowAvailable() {
        return protocolGlow != null;
    }

    public void start() {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            try {
                protocolGlow = new ProtocolGlow();
            } catch (Throwable t) {
                plugin.getLogger().warning("ProtocolLib найден, но не удалось инициализировать: " + t.getMessage());
            }
        }
        long period = Math.max(2, plugin.settings().integer("glow.update-ticks"));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Team t : teams.values()) {
            try {
                t.unregister();
            } catch (IllegalStateException ignored) {
                // уже удалена
            }
        }
        teams.clear();
    }

    private void tick() {
        boolean teamsEnabled = plugin.settings().bool("glow.name-color-team");
        for (Clan c : plugin.clans().all()) {
            List<Player> online = onlineMembers(c);
            GlowMode mode = c.glowMode();
            if (mode == GlowMode.GLOW && online.size() > 1) {
                if (protocolGlow == null) {
                    if (!warned) {
                        warned = true;
                        plugin.getLogger().warning("Режим GLOW требует ProtocolLib — подсветка отключена.");
                    }
                } else {
                    sendGlow(online, true);
                }
            }
            if (mode == GlowMode.NAME_COLOR && teamsEnabled) {
                syncTeam(c, online);
            } else {
                removeTeam(c.id());
            }
        }
    }

    private List<Player> onlineMembers(Clan c) {
        List<Player> list = new ArrayList<>();
        for (UUID u : c.memberIds()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    private void sendGlow(List<Player> players, boolean glow) {
        if (protocolGlow == null) {
            return;
        }
        for (Player viewer : players) {
            for (Player target : players) {
                if (viewer != target && viewer.getWorld().equals(target.getWorld())) {
                    protocolGlow.send(viewer, target, glow);
                }
            }
        }
    }

    private void syncTeam(Clan c, List<Player> online) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = teams.get(c.id());
        if (team == null) {
            String name = "mc_" + c.id().toString().replace("-", "").substring(0, 12);
            team = sb.getTeam(name);
            if (team == null) {
                team = sb.registerNewTeam(name);
            }
            teams.put(c.id(), team);
        }
        team.color(Text.namedColor(c.glowColor()));
        for (Player p : online) {
            if (!team.hasEntry(p.getName())) {
                team.addEntry(p.getName());
            }
        }
        for (String entry : new ArrayList<>(team.getEntries())) {
            Player p = Bukkit.getPlayerExact(entry);
            if (p == null || !c.isMember(p.getUniqueId())) {
                team.removeEntry(entry);
            }
        }
    }

    private void removeTeam(UUID clanId) {
        Team t = teams.remove(clanId);
        if (t != null) {
            try {
                t.unregister();
            } catch (IllegalStateException ignored) {
                // уже удалена
            }
        }
    }

    /** Применяет изменение режима/цвета немедленно. */
    public void refresh(Clan c) {
        List<Player> online = onlineMembers(c);
        if (c.glowMode() != GlowMode.GLOW) {
            sendGlow(online, false);
        }
        if (c.glowMode() != GlowMode.NAME_COLOR) {
            removeTeam(c.id());
        }
    }

    public void onClanRemoved(Clan c) {
        sendGlow(onlineMembers(c), false);
        removeTeam(c.id());
    }

    /** Игрок покинул клан: снимаем с него свечение для бывших соклановцев. */
    public void onMemberRemoved(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null || protocolGlow == null) {
            return;
        }
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other != p) {
                protocolGlow.send(other, p, false);
                protocolGlow.send(p, other, false);
            }
        }
        for (Team t : teams.values()) {
            t.removeEntry(p.getName());
        }
    }
}
