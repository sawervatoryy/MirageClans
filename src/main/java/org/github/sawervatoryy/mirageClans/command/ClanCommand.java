package org.github.sawervatoryy.mirageClans.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.manager.ClanManager.TopType;
import org.github.sawervatoryy.mirageClans.manager.QuestDef;
import org.github.sawervatoryy.mirageClans.manager.QuestPeriod;
import org.github.sawervatoryy.mirageClans.manager.TreasuryManager.Currency;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanHome;
import org.github.sawervatoryy.mirageClans.model.ClanMember;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.model.GlowMode;
import org.github.sawervatoryy.mirageClans.model.QuestProgress;
import org.github.sawervatoryy.mirageClans.util.Result;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Команда /clan (алиасы /c, /clans). Все функции доступны и без GUI (use-gui: false). */
public final class ClanCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("help", "create", "disband", "invite", "accept", "deny",
            "leave", "kick", "promote", "demote", "setrole", "transfer", "info", "members", "top", "deposit",
            "withdraw", "balance", "history", "sethome", "home", "delhome", "homes", "storage", "quests", "level",
            "upgrade", "ff", "glow", "role", "chat", "reload");

    private final MirageClans plugin;

    public ClanCommand(MirageClans plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String key, String... kv) {
        plugin.lang().send(s, key, kv);
    }

    private Clan need(Player p) {
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        if (c == null) {
            msg(p, "general.no-clan");
        }
        return c;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mirageclans.admin")) {
                msg(sender, "general.no-permission");
                return true;
            }
            plugin.reloadAll();
            msg(sender, "general.reloaded");
            return true;
        }
        if (!plugin.isReady()) {
            msg(sender, "general.loading");
            return true;
        }
        if (!(sender instanceof Player p)) {
            msg(sender, "general.players-only");
            return true;
        }
        if (args.length == 0) {
            if (plugin.gui().enabled()) {
                plugin.gui().openMain(p);
            } else {
                plugin.lang().sendLines(p, "help");
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] a = Arrays.copyOfRange(args, 1, args.length);
        switch (sub) {
            case "create" -> {
                if (a.length < 2) {
                    msg(p, "usage.create");
                } else {
                    plugin.lang().send(p, plugin.clans().create(p, a[0], a[1]));
                }
            }
            case "disband" -> disband(p, a);
            case "invite" -> {
                Clan c = need(p);
                if (c != null) {
                    if (a.length < 1) {
                        msg(p, "usage.invite");
                    } else {
                        plugin.lang().send(p, plugin.clans().invite(c, p, a[0]));
                    }
                }
            }
            case "accept", "deny" -> invite(p, sub.equals("accept"), a);
            case "leave" -> plugin.lang().send(p, plugin.clans().leave(p));
            case "kick", "promote", "demote", "transfer" -> memberAction(p, sub, a);
            case "setrole" -> {
                Clan c = need(p);
                if (c != null) {
                    ClanMember m = a.length < 2 ? null : plugin.clans().findMember(c, a[0]);
                    if (a.length < 2) {
                        msg(p, "usage.setrole");
                    } else if (m == null) {
                        msg(p, "general.not-member");
                    } else {
                        plugin.lang().send(p, plugin.clans().assignRole(c, p, m.uuid(), a[1]));
                    }
                }
            }
            case "info" -> info(p, a);
            case "members" -> {
                if (plugin.gui().enabled()) {
                    plugin.gui().openMembers(p);
                } else {
                    members(p);
                }
            }
            case "top" -> {
                TopType t = TopType.parse(a.length > 0 ? a[0] : null);
                if (plugin.gui().enabled()) {
                    plugin.gui().openTop(p, t);
                } else {
                    top(p, t);
                }
            }
            case "deposit", "withdraw" -> treasury(p, sub.equals("deposit"), a);
            case "balance" -> {
                Clan c = need(p);
                if (c != null) {
                    msg(p, "treasury.balance", "money", Text.num(c.money()), "points", String.valueOf(c.points()));
                }
            }
            case "history" -> history(p);
            case "sethome" -> {
                Clan c = need(p);
                if (c != null) {
                    plugin.lang().send(p, plugin.homes().set(p, c, a.length > 0 ? a[0] : "home"));
                }
            }
            case "home" -> home(p, a);
            case "delhome" -> {
                Clan c = need(p);
                if (c != null) {
                    if (a.length < 1) {
                        msg(p, "usage.delhome");
                    } else {
                        plugin.lang().send(p, plugin.homes().delete(p, c, a[0]));
                    }
                }
            }
            case "homes" -> {
                if (plugin.gui().enabled()) {
                    plugin.gui().openHomes(p);
                } else {
                    homes(p);
                }
            }
            case "storage" -> {
                Clan c = need(p);
                if (c != null) {
                    plugin.storage().open(p, c);
                }
            }
            case "quests" -> {
                if (plugin.gui().enabled()) {
                    plugin.gui().openQuests(p);
                } else {
                    quests(p);
                }
            }
            case "level" -> {
                Clan c = need(p);
                if (c != null) {
                    if (plugin.gui().enabled()) {
                        plugin.gui().openLevels(p);
                    } else {
                        plugin.lang().sendLines(p, "levels.info", plugin.gui().clanKv(c, p));
                    }
                }
            }
            case "upgrade" -> {
                Clan c = need(p);
                if (c != null) {
                    plugin.lang().send(p, plugin.levels().upgrade(c, p));
                }
            }
            case "ff" -> {
                Clan c = need(p);
                if (c != null) {
                    boolean v = a.length > 0 ? a[0].equalsIgnoreCase("on") : !c.friendlyFire();
                    plugin.lang().send(p, plugin.clans().setFriendlyFire(c, p, v));
                }
            }
            case "glow" -> glow(p, a);
            case "role" -> role(p, a);
            case "chat" -> plugin.getCommand("cc").getExecutor().onCommand(p, cmd, label, new String[0]);
            default -> plugin.lang().sendLines(p, "help");
        }
        return true;
    }

    // ------------------------------------------------------------------ подкоманды

    private void disband(Player p, String[] a) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        if (!c.isLeader(p.getUniqueId())) {
            msg(p, "general.leader-only");
        } else if (a.length < 1 || !a[0].equalsIgnoreCase("confirm")) {
            msg(p, "disband.confirm");
        } else {
            plugin.clans().disband(c);
            msg(p, "disband.success", "clan", c.name());
        }
    }

    private void invite(Player p, boolean accept, String[] a) {
        List<org.github.sawervatoryy.mirageClans.manager.InviteManager.Invite> list =
                plugin.invites().get(p.getUniqueId());
        UUID target = null;
        if (a.length > 0) {
            Clan c = plugin.clans().byName(a[0]);
            target = c == null ? null : c.id();
        } else if (!list.isEmpty()) {
            target = list.get(list.size() - 1).clanId();
        }
        if (target == null) {
            msg(p, "invite.not-found");
            return;
        }
        plugin.lang().send(p, accept ? plugin.clans().accept(p, target) : plugin.clans().deny(p, target));
    }

    private void memberAction(Player p, String sub, String[] a) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        if (a.length < 1) {
            msg(p, "usage." + sub);
            return;
        }
        ClanMember m = plugin.clans().findMember(c, a[0]);
        if (m == null) {
            msg(p, "general.not-member");
            return;
        }
        Result r = switch (sub) {
            case "kick" -> plugin.clans().kick(c, p, m.uuid());
            case "promote" -> plugin.clans().shiftRole(c, p, m.uuid(), 1);
            case "demote" -> plugin.clans().shiftRole(c, p, m.uuid(), -1);
            default -> plugin.clans().transfer(c, p, m.uuid());
        };
        plugin.lang().send(p, r);
    }

    private void info(Player p, String[] a) {
        Clan c = a.length > 0 ? plugin.clans().byName(a[0]) : plugin.clans().byPlayer(p.getUniqueId());
        if (c == null) {
            msg(p, a.length > 0 ? "general.clan-not-found" : "general.no-clan");
            return;
        }
        plugin.lang().sendLines(p, "info.lines", plugin.gui().clanKv(c, p));
    }

    private void members(Player p) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        List<ClanMember> list = new ArrayList<>(c.members());
        list.sort(Comparator.comparingInt((ClanMember m) -> -c.weightOf(m.uuid())).thenComparing(ClanMember::name));
        msg(p, "members.header", "count", String.valueOf(list.size()));
        for (ClanMember m : list) {
            ClanRole r = c.roleOf(m.uuid());
            p.sendMessage(plugin.lang().plain("members.line", "player", m.name(),
                    "role", r == null ? "-" : plugin.clans().roleName(r),
                    "status", plugin.lang().raw(Bukkit.getPlayer(m.uuid()) != null ? "members.online" : "members.offline")));
        }
    }

    private void top(Player p, TopType t) {
        List<Clan> list = plugin.clans().top(t, plugin.settings().integer("clan.top-size"));
        msg(p, "top.header", "type", plugin.lang().raw("top.types." + t.name()));
        int i = 1;
        for (Clan c : list) {
            String value = switch (t) {
                case LEVEL -> String.valueOf(c.level());
                case POINTS -> String.valueOf(c.totalXp());
                case BALANCE -> Text.num(c.money());
            };
            p.sendMessage(plugin.lang().plain("top.line", "rank", String.valueOf(i++), "clan", c.name(),
                    "tag", c.tag(), "value", value));
        }
    }

    private void treasury(Player p, boolean deposit, String[] a) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        Currency cur = a.length > 0 ? Currency.parse(a[0]) : null;
        if (cur == null || a.length < 2) {
            msg(p, "usage." + (deposit ? "deposit" : "withdraw"));
            return;
        }
        double v;
        try {
            v = Double.parseDouble(a[1].replace(',', '.'));
        } catch (NumberFormatException ex) {
            msg(p, "treasury.bad-amount");
            return;
        }
        plugin.lang().send(p, deposit ? plugin.treasury().deposit(p, c, cur, v) : plugin.treasury().withdraw(p, c, cur, v));
    }

    private void history(Player p) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        plugin.treasury().history(c).whenComplete((list, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (err != null || list.isEmpty()) {
                msg(p, "history.empty");
                return;
            }
            msg(p, "history.header");
            for (var t : list) {
                p.sendMessage(plugin.lang().plain("history.line", "player", t.playerName(),
                        "action", plugin.lang().raw("history.actions." + t.action()),
                        "amount", Text.num(t.amount()),
                        "currency", plugin.treasury().currencyName(Currency.valueOf(t.currency())),
                        "time", new java.text.SimpleDateFormat("dd.MM HH:mm").format(new java.util.Date(t.timestamp()))));
            }
        }));
    }

    private void home(Player p, String[] a) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        String name;
        if (a.length > 0) {
            name = a[0];
        } else if (c.homes().size() == 1) {
            name = c.homes().values().iterator().next().name();
        } else if (c.homes().isEmpty()) {
            msg(p, "homes.none");
            return;
        } else {
            homes(p);
            return;
        }
        plugin.lang().send(p, plugin.homes().teleport(p, c, name));
    }

    private void homes(Player p) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        msg(p, "homes.header", "count", String.valueOf(c.homes().size()), "max", String.valueOf(plugin.levels().maxHomes(c)));
        for (ClanHome h : c.homes().values()) {
            p.sendMessage(plugin.lang().plain("homes.line", "home", h.name(), "world", h.world(),
                    "x", String.valueOf((int) h.x()), "y", String.valueOf((int) h.y()), "z", String.valueOf((int) h.z())));
        }
    }

    private void quests(Player p) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        for (QuestPeriod period : QuestPeriod.values()) {
            msg(p, "quests.header", "period", plugin.lang().raw("quests.periods." + period.name()),
                    "reset", plugin.lang().duration(plugin.quests().secondsUntilReset(period)));
            for (QuestDef d : plugin.quests().definitions(period)) {
                if (c.level() < d.minLevel()) {
                    p.sendMessage(plugin.lang().plain("quests.line-locked", "quest", d.name(), "level", String.valueOf(d.minLevel())));
                    continue;
                }
                QuestProgress pr = plugin.quests().progress(c, d);
                p.sendMessage(plugin.lang().plain("quests.line", "quest", d.name(),
                        "progress", String.valueOf(Math.min(pr.value(), d.amount())), "amount", String.valueOf(d.amount()),
                        "status", plugin.lang().raw(pr.completed() ? "quests.status-done" : "quests.status-active")));
            }
        }
    }

    private void glow(Player p, String[] a) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        if (a.length == 0) {
            msg(p, "usage.glow");
            return;
        }
        if (a[0].equalsIgnoreCase("color") && a.length > 1) {
            plugin.lang().send(p, plugin.clans().setGlowColor(c, p, a[1]));
            return;
        }
        GlowMode mode = switch (a[0].toLowerCase(Locale.ROOT)) {
            case "off" -> GlowMode.OFF;
            case "glow" -> GlowMode.GLOW;
            case "name", "color", "name_color" -> GlowMode.NAME_COLOR;
            default -> null;
        };
        if (mode == null) {
            msg(p, "usage.glow");
        } else {
            plugin.lang().send(p, plugin.clans().setGlowMode(c, p, mode));
        }
    }

    private void role(Player p, String[] a) {
        Clan c = need(p);
        if (c == null) {
            return;
        }
        if (a.length == 0 || a[0].equalsIgnoreCase("list")) {
            for (ClanRole r : c.rolesSorted()) {
                p.sendMessage(plugin.lang().plain("role.line", "id", r.id(), "role", plugin.clans().roleName(r),
                        "weight", String.valueOf(r.weight()),
                        "perms", r.isLeader() ? plugin.lang().raw("roles.all-perms")
                                : r.permissions().stream().map(Enum::name).sorted().toList().toString()));
            }
            return;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (a.length < 2) {
                    msg(p, "usage.role");
                } else {
                    plugin.lang().send(p, plugin.clans().createRole(c, p, String.join(" ", Arrays.copyOfRange(a, 1, a.length))));
                }
            }
            case "delete" -> {
                if (a.length < 2) {
                    msg(p, "usage.role");
                } else {
                    plugin.lang().send(p, plugin.clans().deleteRole(c, p, a[1].toUpperCase(Locale.ROOT)));
                }
            }
            case "perm" -> {
                ClanPermission perm = a.length < 3 ? null : ClanPermission.parse(a[2]);
                if (perm == null) {
                    msg(p, "usage.role");
                } else {
                    plugin.lang().send(p, plugin.clans().togglePermission(c, p, a[1].toUpperCase(Locale.ROOT), perm));
                }
            }
            default -> {
                if (plugin.gui().enabled()) {
                    plugin.gui().openRoles(p);
                } else {
                    msg(p, "usage.role");
                }
            }
        }
    }

    // ------------------------------------------------------------------ автодополнение

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        if (args.length == 2) {
            switch (sub) {
                case "invite":
                    return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
                case "kick", "promote", "demote", "transfer", "setrole":
                    return c == null ? List.of() : filter(c.members().stream().map(ClanMember::name).toList(), args[1]);
                case "deposit", "withdraw":
                    return filter(List.of("money", "points"), args[1]);
                case "top":
                    return filter(List.of("level", "points", "balance"), args[1]);
                case "home", "delhome":
                    return c == null ? List.of() : filter(c.homes().values().stream().map(ClanHome::name).toList(), args[1]);
                case "glow":
                    return filter(List.of("off", "glow", "name", "color"), args[1]);
                case "ff":
                    return filter(List.of("on", "off"), args[1]);
                case "role":
                    return filter(List.of("list", "create", "delete", "perm"), args[1]);
                case "accept", "deny":
                    return filter(plugin.invites().get(p.getUniqueId()).stream()
                            .map(i -> plugin.clans().byId(i.clanId())).filter(x -> x != null).map(Clan::name).toList(), args[1]);
                default:
                    return List.of();
            }
        }
        if (args.length == 3 && c != null) {
            if (sub.equals("setrole")) {
                return filter(c.roles().keySet().stream().filter(id -> !id.equals(ClanRole.LEADER_ID)).toList(), args[2]);
            }
            if (sub.equals("glow") && args[1].equalsIgnoreCase("color")) {
                return filter(plugin.settings().cfg().getStringList("glow.colors"), args[2]);
            }
            if (sub.equals("role") && (args[1].equalsIgnoreCase("perm") || args[1].equalsIgnoreCase("delete"))) {
                return filter(c.roles().keySet().stream().filter(id -> !id.equals(ClanRole.LEADER_ID)).toList(), args[2]);
            }
        }
        if (args.length == 4 && sub.equals("role") && args[1].equalsIgnoreCase("perm")) {
            return filter(Arrays.stream(ClanPermission.values()).map(Enum::name).toList(), args[3]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
