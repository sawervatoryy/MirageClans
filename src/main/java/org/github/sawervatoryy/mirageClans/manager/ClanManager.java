package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.config.Settings;
import org.github.sawervatoryy.mirageClans.gui.Menu;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanMember;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.model.GlowMode;
import org.github.sawervatoryy.mirageClans.util.Result;
import org.github.sawervatoryy.mirageClans.util.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;

/**
 * Главный менеджер кланов: реестр, создание/роспуск, участники, роли и права.
 * Все изменяющие методы вызываются из главного потока; сохранение в БД — асинхронное.
 */
public final class ClanManager {

    /** Тип рейтинга. */
    public enum TopType {
        LEVEL, POINTS, BALANCE;

        public static TopType parse(String s) {
            if (s == null) {
                return LEVEL;
            }
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "points", "xp", "очки" -> POINTS;
                case "balance", "bal", "money", "баланс" -> BALANCE;
                default -> LEVEL;
            };
        }
    }

    private final MirageClans plugin;
    private final Map<UUID, Clan> clans = new ConcurrentHashMap<>();
    private final Map<String, Clan> names = new ConcurrentHashMap<>();
    private final Map<String, Clan> tags = new ConcurrentHashMap<>();
    private final Map<UUID, Clan> byMember = new ConcurrentHashMap<>();

    public ClanManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ реестр

    public void registerLoaded(Collection<Clan> list) {
        for (Clan c : list) {
            ensureRoles(c);
            index(c);
        }
    }

    private void index(Clan c) {
        clans.put(c.id(), c);
        names.put(c.name().toLowerCase(Locale.ROOT), c);
        tags.put(c.tag().toLowerCase(Locale.ROOT), c);
        for (UUID u : c.memberIds()) {
            byMember.put(u, c);
        }
    }

    private void ensureRoles(Clan c) {
        boolean hasOther = c.roles().values().stream().anyMatch(r -> !r.isLeader());
        if (!hasOther) {
            applyTemplates(c);
        }
        ClanMember lm = c.member(c.leader());
        if (lm != null) {
            lm.setRoleId(ClanRole.LEADER_ID);
        }
    }

    private void applyTemplates(Clan c) {
        for (Settings.RoleTemplate t : plugin.settings().roleTemplates().values()) {
            ClanRole r = new ClanRole(t.id(), t.name(), t.weight());
            r.permissions().addAll(t.permissions());
            c.roles().put(r.id(), r);
        }
        if (c.roles().size() == 1) {
            String id = plugin.settings().defaultRoleId();
            c.roles().put(id, new ClanRole(id, id, 10));
        }
    }

    public Clan byPlayer(UUID player) {
        return byMember.get(player);
    }

    public Clan byId(UUID id) {
        return clans.get(id);
    }

    public Clan byName(String name) {
        return name == null ? null : names.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Clan> all() {
        return clans.values();
    }

    public ClanRole defaultRole(Clan c) {
        ClanRole r = c.role(plugin.settings().defaultRoleId());
        return r != null && !r.isLeader() ? r : c.lowestRole();
    }

    /** Отображаемое имя роли (для лидера берётся из lang.yml). */
    public String roleName(ClanRole role) {
        return role.isLeader() ? plugin.lang().raw("roles.leader") : role.displayName();
    }

    public ClanMember findMember(Clan c, String name) {
        for (ClanMember m : c.members()) {
            if (m.name().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public void save(Clan c) {
        plugin.repository().saveClan(c);
    }

    /** Помечает клан «грязным» — он будет сохранён периодической задачей. */
    public void markDirty(Clan c) {
        c.setDirty(true);
    }

    /** Сохраняет все «грязные» кланы (вызывается периодически). */
    public void flushDirty() {
        for (Clan c : clans.values()) {
            if (c.dirty()) {
                plugin.repository().saveClan(c);
            }
        }
    }

    public void saveAll() {
        for (Clan c : clans.values()) {
            plugin.repository().saveClan(c);
        }
    }

    /** Отправляет сообщение всем онлайн-участникам клана. */
    public void broadcast(Clan c, String key, String... kv) {
        for (UUID u : c.memberIds()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                plugin.lang().send(p, key, kv);
            }
        }
    }

    public int onlineCount(Clan c) {
        int n = 0;
        for (UUID u : c.memberIds()) {
            if (Bukkit.getPlayer(u) != null) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ создание / роспуск

    /** Проверка названия и тега. */
    public Result validate(String name, String tag) {
        Settings s = plugin.settings();
        int nmin = s.cfg().getInt("clan.name.min-length", 3);
        int nmax = s.cfg().getInt("clan.name.max-length", 16);
        int tmin = s.cfg().getInt("clan.tag.min-length", 2);
        int tmax = s.cfg().getInt("clan.tag.max-length", 5);
        if (name.length() < nmin || name.length() > nmax) {
            return Result.fail("create.name-length", "min", String.valueOf(nmin), "max", String.valueOf(nmax));
        }
        if (tag.length() < tmin || tag.length() > tmax) {
            return Result.fail("create.tag-length", "min", String.valueOf(tmin), "max", String.valueOf(tmax));
        }
        try {
            if (!name.matches(s.cfg().getString("clan.name.regex", "^[A-Za-z0-9_]+$"))) {
                return Result.fail("create.name-invalid");
            }
            if (!tag.matches(s.cfg().getString("clan.tag.regex", "^[A-Za-z0-9]+$"))) {
                return Result.fail("create.tag-invalid");
            }
        } catch (PatternSyntaxException ex) {
            plugin.getLogger().warning("Некорректный regex в config.yml: " + ex.getMessage());
        }
        if (names.containsKey(name.toLowerCase(Locale.ROOT))) {
            return Result.fail("create.name-taken");
        }
        if (tags.containsKey(tag.toLowerCase(Locale.ROOT))) {
            return Result.fail("create.tag-taken");
        }
        return Result.ok();
    }

    /** Создаёт клан (стоимость списывается с лидера). */
    public Result create(Player leader, String name, String tag) {
        if (byMember.containsKey(leader.getUniqueId())) {
            return Result.fail("create.already-in-clan");
        }
        Result v = validate(name, tag);
        if (!v.isOk()) {
            return v;
        }
        double costMoney = plugin.settings().decimal("clan.create-cost.money");
        long costPoints = plugin.settings().cfg().getLong("clan.create-cost.points");
        if (costMoney > 0) {
            if (!plugin.vault().available() || !plugin.vault().has(leader, costMoney)) {
                return Result.fail("create.not-enough-money", "amount", Text.num(costMoney));
            }
        }
        if (costPoints > 0) {
            if (!plugin.points().available() || plugin.points().balance(leader.getUniqueId()) < costPoints) {
                return Result.fail("create.not-enough-points", "amount", String.valueOf(costPoints));
            }
        }
        if (costMoney > 0 && !plugin.vault().withdraw(leader, costMoney)) {
            return Result.fail("create.not-enough-money", "amount", Text.num(costMoney));
        }
        if (costPoints > 0 && !plugin.points().take(leader.getUniqueId(), (int) costPoints)) {
            if (costMoney > 0) {
                plugin.vault().deposit(leader, costMoney);
            }
            return Result.fail("create.not-enough-points", "amount", String.valueOf(costPoints));
        }

        Clan clan = new Clan(UUID.randomUUID(), name, tag, leader.getUniqueId(), System.currentTimeMillis());
        clan.setFriendlyFire(plugin.settings().bool("clan.default-friendly-fire"));
        clan.setGlowColor(plugin.settings().str("clan.default-glow-color").toUpperCase(Locale.ROOT));
        applyTemplates(clan);
        clan.addMember(new ClanMember(leader.getUniqueId(), leader.getName(), ClanRole.LEADER_ID,
                System.currentTimeMillis()));
        index(clan);
        save(clan);
        return Result.ok("create.success", "clan", name, "tag", tag);
    }

    /** Распускает клан. */
    public void disband(Clan c) {
        for (UUID u : new ArrayList<>(c.memberIds())) {
            closeMenus(u);
            byMember.remove(u);
            plugin.chat().untoggle(u);
        }
        clans.remove(c.id());
        names.remove(c.name().toLowerCase(Locale.ROOT));
        tags.remove(c.tag().toLowerCase(Locale.ROOT));
        plugin.storage().onDisband(c);
        plugin.glow().onClanRemoved(c);
        plugin.repository().deleteClan(c.id());
    }

    private void closeMenus(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            InventoryHolder h = p.getOpenInventory().getTopInventory().getHolder(false);
            if (h instanceof Menu || h instanceof StorageHolder) {
                p.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------ участники

    public Result join(Clan c, Player p) {
        if (byMember.containsKey(p.getUniqueId())) {
            return Result.fail("create.already-in-clan");
        }
        if (c.memberCount() >= plugin.levels().maxMembers(c)) {
            return Result.fail("invite.clan-full");
        }
        ClanRole role = defaultRole(c);
        c.addMember(new ClanMember(p.getUniqueId(), p.getName(), role.id(), System.currentTimeMillis()));
        byMember.put(p.getUniqueId(), c);
        plugin.invites().clear(p.getUniqueId());
        save(c);
        broadcast(c, "invite.joined", "player", p.getName());
        return Result.ok();
    }

    /** Выход игрока из клана (лидер выйти не может). */
    public Result leave(Player p) {
        Clan c = byMember.get(p.getUniqueId());
        if (c == null) {
            return Result.fail("general.no-clan");
        }
        if (c.isLeader(p.getUniqueId())) {
            return Result.fail("leave.leader");
        }
        removeMember(c, p.getUniqueId());
        broadcast(c, "leave.left", "player", p.getName());
        return Result.ok("leave.success", "clan", c.name());
    }

    private void removeMember(Clan c, UUID u) {
        closeMenus(u);
        c.removeMember(u);
        byMember.remove(u);
        plugin.chat().untoggle(u);
        plugin.glow().onMemberRemoved(u);
        save(c);
    }

    public Result kick(Clan c, Player actor, UUID target) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.KICK)) {
            return Result.fail("general.no-permission");
        }
        ClanMember tm = c.member(target);
        if (tm == null) {
            return Result.fail("general.not-member");
        }
        if (target.equals(actor.getUniqueId())) {
            return Result.fail("kick.self");
        }
        if (c.weightOf(actor.getUniqueId()) <= c.weightOf(target)) {
            return Result.fail("general.hierarchy");
        }
        removeMember(c, target);
        Player tp = Bukkit.getPlayer(target);
        if (tp != null) {
            plugin.lang().send(tp, "kick.you-kicked", "clan", c.name(), "player", actor.getName());
        }
        broadcast(c, "kick.broadcast", "player", tm.name(), "actor", actor.getName());
        return Result.ok();
    }

    public Result transfer(Clan c, Player actor, UUID target) {
        if (!c.isLeader(actor.getUniqueId())) {
            return Result.fail("general.leader-only");
        }
        ClanMember tm = c.member(target);
        if (tm == null) {
            return Result.fail("general.not-member");
        }
        if (target.equals(actor.getUniqueId())) {
            return Result.fail("transfer.self");
        }
        ClanMember old = c.member(actor.getUniqueId());
        ClanRole best = c.rolesSorted().stream().filter(r -> !r.isLeader()).findFirst().orElse(defaultRole(c));
        if (old != null) {
            old.setRoleId(best.id());
        }
        tm.setRoleId(ClanRole.LEADER_ID);
        c.setLeader(target);
        save(c);
        broadcast(c, "transfer.broadcast", "player", tm.name());
        return Result.ok();
    }

    // ------------------------------------------------------------------ роли участников

    /** Повышает (+1) или понижает (-1) участника на одну ступень иерархии. */
    public Result shiftRole(Clan c, Player actor, UUID target, int direction) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.MANAGE_ROLES)) {
            return Result.fail("general.no-permission");
        }
        ClanMember tm = c.member(target);
        if (tm == null) {
            return Result.fail("general.not-member");
        }
        if (c.isLeader(target)) {
            return Result.fail("role.leader-fixed");
        }
        int actorWeight = c.weightOf(actor.getUniqueId());
        if (actorWeight <= c.weightOf(target)) {
            return Result.fail("general.hierarchy");
        }
        List<ClanRole> ladder = new ArrayList<>(c.roles().values());
        ladder.removeIf(ClanRole::isLeader);
        ladder.sort(Comparator.comparingInt(ClanRole::weight));
        int cur = -1;
        for (int i = 0; i < ladder.size(); i++) {
            if (ladder.get(i).id().equals(tm.roleId())) {
                cur = i;
                break;
            }
        }
        int next = cur + direction;
        if (cur < 0 || next < 0 || next >= ladder.size()) {
            return Result.fail(direction > 0 ? "role.already-top" : "role.already-bottom");
        }
        ClanRole nr = ladder.get(next);
        if (nr.weight() >= actorWeight) {
            return Result.fail("general.hierarchy");
        }
        tm.setRoleId(nr.id());
        save(c);
        broadcast(c, "role.changed", "player", tm.name(), "role", roleName(nr));
        return Result.ok();
    }

    public Result assignRole(Clan c, Player actor, UUID target, String roleId) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.MANAGE_ROLES)) {
            return Result.fail("general.no-permission");
        }
        ClanMember tm = c.member(target);
        if (tm == null) {
            return Result.fail("general.not-member");
        }
        ClanRole nr = c.role(roleId.toUpperCase(Locale.ROOT));
        if (nr == null || nr.isLeader()) {
            return Result.fail("role.not-found", "role", roleId);
        }
        if (c.isLeader(target)) {
            return Result.fail("role.leader-fixed");
        }
        int aw = c.weightOf(actor.getUniqueId());
        if (aw <= c.weightOf(target) || aw <= nr.weight()) {
            return Result.fail("general.hierarchy");
        }
        tm.setRoleId(nr.id());
        save(c);
        broadcast(c, "role.changed", "player", tm.name(), "role", roleName(nr));
        return Result.ok();
    }

    // ------------------------------------------------------------------ управление ролями

    private Result checkRoleEdit(Clan c, Player actor, ClanRole role) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.MANAGE_ROLES)) {
            return Result.fail("general.no-permission");
        }
        if (role.isLeader()) {
            return Result.fail("role.leader-fixed");
        }
        if (c.weightOf(actor.getUniqueId()) <= role.weight()) {
            return Result.fail("general.hierarchy");
        }
        return Result.ok();
    }

    /** Создаёт кастомную роль. Идентификатор получается из названия. */
    public Result createRole(Clan c, Player actor, String displayName) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.MANAGE_ROLES)) {
            return Result.fail("general.no-permission");
        }
        String id = Text.strip(displayName).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9А-ЯЁ_]", "_");
        if (id.length() > 16) {
            id = id.substring(0, 16);
        }
        if (id.length() < 2 || ClanRole.LEADER_ID.equals(id)) {
            return Result.fail("role.bad-name");
        }
        if (c.roles().containsKey(id)) {
            return Result.fail("role.exists", "role", id);
        }
        long custom = c.roles().values().stream()
                .filter(r -> !r.isLeader() && !plugin.settings().roleTemplates().containsKey(r.id())).count();
        if (custom >= plugin.settings().integer("clan.max-custom-roles")) {
            return Result.fail("role.limit");
        }
        int weight = Math.max(1, Math.min(c.weightOf(actor.getUniqueId()) - 1, 30));
        c.roles().put(id, new ClanRole(id, displayName, weight));
        save(c);
        return Result.ok("role.created", "role", displayName);
    }

    public Result deleteRole(Clan c, Player actor, String roleId) {
        ClanRole role = c.role(roleId);
        if (role == null) {
            return Result.fail("role.not-found", "role", roleId);
        }
        Result chk = checkRoleEdit(c, actor, role);
        if (!chk.isOk()) {
            return chk;
        }
        if (role.id().equals(plugin.settings().defaultRoleId())) {
            return Result.fail("role.default-protected");
        }
        ClanRole fallback = defaultRole(c);
        for (ClanMember m : c.members()) {
            if (m.roleId().equals(role.id())) {
                m.setRoleId(fallback.id());
            }
        }
        c.roles().remove(role.id());
        save(c);
        return Result.ok("role.deleted", "role", role.displayName());
    }

    public Result renameRole(Clan c, Player actor, String roleId, String newName) {
        ClanRole role = c.role(roleId);
        if (role == null) {
            return Result.fail("role.not-found", "role", roleId);
        }
        Result chk = checkRoleEdit(c, actor, role);
        if (!chk.isOk()) {
            return chk;
        }
        role.setDisplayName(newName);
        save(c);
        return Result.ok("role.renamed", "role", newName);
    }

    /** Меняет вес роли на delta (в пределах ниже веса актора). */
    public Result shiftWeight(Clan c, Player actor, String roleId, int delta) {
        ClanRole role = c.role(roleId);
        if (role == null) {
            return Result.fail("role.not-found", "role", roleId);
        }
        Result chk = checkRoleEdit(c, actor, role);
        if (!chk.isOk()) {
            return chk;
        }
        int max = c.weightOf(actor.getUniqueId()) - 1;
        int nw = Math.max(1, Math.min(max, role.weight() + delta));
        role.setWeight(nw);
        save(c);
        return Result.ok();
    }

    public Result togglePermission(Clan c, Player actor, String roleId, ClanPermission perm) {
        ClanRole role = c.role(roleId);
        if (role == null) {
            return Result.fail("role.not-found", "role", roleId);
        }
        Result chk = checkRoleEdit(c, actor, role);
        if (!chk.isOk()) {
            return chk;
        }
        role.toggle(perm);
        save(c);
        return Result.ok();
    }

    // ------------------------------------------------------------------ настройки клана

    public Result setFriendlyFire(Clan c, Player actor, boolean value) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.PVP_MANAGE)) {
            return Result.fail("general.no-permission");
        }
        c.setFriendlyFire(value);
        save(c);
        return Result.ok(value ? "settings.ff-on" : "settings.ff-off");
    }

    public Result setGlowMode(Clan c, Player actor, GlowMode mode) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.SETTINGS)) {
            return Result.fail("general.no-permission");
        }
        c.setGlowMode(mode);
        save(c);
        plugin.glow().refresh(c);
        return Result.ok("settings.glow-mode", "mode", plugin.lang().raw("glow-modes." + mode.name()));
    }

    public Result setGlowColor(Clan c, Player actor, String color) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.SETTINGS)) {
            return Result.fail("general.no-permission");
        }
        List<String> allowed = plugin.settings().cfg().getStringList("glow.colors");
        String up = color.toUpperCase(Locale.ROOT);
        if (!allowed.contains(up)) {
            return Result.fail("settings.bad-color", "colors", String.join(", ", allowed));
        }
        c.setGlowColor(up);
        save(c);
        plugin.glow().refresh(c);
        return Result.ok("settings.glow-color", "color", Text.colorCode(up) + up);
    }

    // ------------------------------------------------------------------ приглашения

    public Result invite(Clan c, Player actor, String targetName) {
        if (!c.hasPermission(actor.getUniqueId(), ClanPermission.INVITE)) {
            return Result.fail("general.no-permission");
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            return Result.fail("general.player-offline", "player", targetName);
        }
        if (byMember.containsKey(target.getUniqueId())) {
            return Result.fail("invite.target-in-clan", "player", target.getName());
        }
        if (c.memberCount() >= plugin.levels().maxMembers(c)) {
            return Result.fail("invite.clan-full");
        }
        plugin.invites().invite(target.getUniqueId(), c.id(), actor.getUniqueId(), actor.getName());
        plugin.lang().send(target, "invite.received", "clan", c.name(), "player", actor.getName(),
                "seconds", String.valueOf(plugin.settings().integer("clan.invite-expire-seconds")));
        return Result.ok("invite.sent", "player", target.getName());
    }

    public Result accept(Player p, UUID clanId) {
        InviteManager.Invite inv = plugin.invites().find(p.getUniqueId(), clanId);
        Clan c = clans.get(clanId);
        if (inv == null || c == null) {
            return Result.fail("invite.not-found");
        }
        Result r = join(c, p);
        if (r.isOk()) {
            return Result.ok("invite.accepted", "clan", c.name());
        }
        return r;
    }

    public Result deny(Player p, UUID clanId) {
        Clan c = clans.get(clanId);
        plugin.invites().remove(p.getUniqueId(), clanId);
        return Result.ok("invite.denied", "clan", c == null ? "-" : c.name());
    }

    // ------------------------------------------------------------------ рейтинг

    public List<Clan> top(TopType type, int limit) {
        Comparator<Clan> cmp = switch (type) {
            case LEVEL -> Comparator.<Clan>comparingInt(Clan::level).thenComparingLong(Clan::xp);
            case POINTS -> Comparator.<Clan>comparingLong(Clan::totalXp);
            case BALANCE -> Comparator.<Clan>comparingDouble(Clan::money);
        };
        List<Clan> list = new ArrayList<>(clans.values());
        list.sort(cmp.reversed());
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    /** Позиция клана в рейтинге (1..N). */
    public int rank(Clan c, TopType type) {
        List<Clan> t = top(type, Integer.MAX_VALUE);
        return t.indexOf(c) + 1;
    }
}
