package org.github.sawervatoryy.mirageClans.storage;

import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;
import org.github.sawervatoryy.mirageClans.model.ClanHome;
import org.github.sawervatoryy.mirageClans.model.ClanMember;
import org.github.sawervatoryy.mirageClans.model.ClanPermission;
import org.github.sawervatoryy.mirageClans.model.ClanRole;
import org.github.sawervatoryy.mirageClans.model.GlowMode;
import org.github.sawervatoryy.mirageClans.model.QuestProgress;
import org.github.sawervatoryy.mirageClans.model.TreasuryEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Слой хранения. ВСЕ обращения к БД выполняются в отдельном однопоточном исполнителе — главный поток никогда
 * не блокируется. Однопоточность гарантирует порядок записей (казна/склад не «откатываются» старыми снимками).
 * Данные кланов целиком живут в памяти, БД — источник восстановления после рестарта.
 */
public final class ClanRepository {

    private final MirageClans plugin;
    private final Database db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MirageClans-DB");
        t.setDaemon(true);
        return t;
    });

    public ClanRepository(MirageClans plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

    // ------------------------------------------------------------------ схема

    /** Создаёт таблицы. */
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            String blob = db.sqlite() ? "BLOB" : "LONGBLOB";
            String auto = db.sqlite() ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "BIGINT PRIMARY KEY AUTO_INCREMENT";
            String idx = db.sqlite() ? "" : ", INDEX idx_mc_log_clan (clan_id)";
            try (Connection c = db.connection(); Statement st = c.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mc_clans ("
                        + "id VARCHAR(36) PRIMARY KEY, name VARCHAR(32) NOT NULL, tag VARCHAR(16) NOT NULL,"
                        + "leader VARCHAR(36) NOT NULL, created BIGINT NOT NULL, level INT NOT NULL,"
                        + "xp BIGINT NOT NULL, total_xp BIGINT NOT NULL, money DOUBLE NOT NULL,"
                        + "points BIGINT NOT NULL, friendly_fire INT NOT NULL, glow_mode VARCHAR(16) NOT NULL,"
                        + "glow_color VARCHAR(32) NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mc_members ("
                        + "uuid VARCHAR(36) PRIMARY KEY, clan_id VARCHAR(36) NOT NULL, name VARCHAR(32) NOT NULL,"
                        + "role VARCHAR(32) NOT NULL, joined BIGINT NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mc_roles ("
                        + "clan_id VARCHAR(36) NOT NULL, role_id VARCHAR(32) NOT NULL, display VARCHAR(128) NOT NULL,"
                        + "weight INT NOT NULL, perms VARCHAR(512) NOT NULL, PRIMARY KEY (clan_id, role_id))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mc_homes ("
                        + "clan_id VARCHAR(36) NOT NULL, name VARCHAR(32) NOT NULL, world VARCHAR(64) NOT NULL,"
                        + "x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL,"
                        + "pitch FLOAT NOT NULL, PRIMARY KEY (clan_id, name))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mc_quests ("
                        + "clan_id VARCHAR(36) NOT NULL, quest_id VARCHAR(64) NOT NULL, progress BIGINT NOT NULL,"
                        + "completed INT NOT NULL, period_key VARCHAR(32) NOT NULL, PRIMARY KEY (clan_id, quest_id))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mc_storage ("
                        + "clan_id VARCHAR(36) PRIMARY KEY, data " + blob + ")");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mc_treasury_log ("
                        + "id " + auto + ", clan_id VARCHAR(36) NOT NULL, player VARCHAR(36), "
                        + "player_name VARCHAR(64) NOT NULL, currency VARCHAR(16) NOT NULL, action VARCHAR(16) NOT NULL,"
                        + "amount DOUBLE NOT NULL, ts BIGINT NOT NULL" + idx + ")");
                if (db.sqlite()) {
                    st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mc_log_clan ON mc_treasury_log (clan_id, id)");
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    // ------------------------------------------------------------------ загрузка

    /** Загружает все кланы. */
    public CompletableFuture<List<Clan>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = db.connection()) {
                Map<UUID, Clan> map = new LinkedHashMap<>();
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                        "SELECT id,name,tag,leader,created,level,xp,total_xp,money,points,friendly_fire,"
                                + "glow_mode,glow_color FROM mc_clans")) {
                    while (rs.next()) {
                        UUID id = UUID.fromString(rs.getString("id"));
                        Clan clan = new Clan(id, rs.getString("name"), rs.getString("tag"),
                                UUID.fromString(rs.getString("leader")), rs.getLong("created"));
                        clan.restoreCore(rs.getInt("level"), rs.getLong("xp"), rs.getLong("total_xp"),
                                rs.getDouble("money"), rs.getLong("points"));
                        clan.setFriendlyFire(rs.getInt("friendly_fire") != 0);
                        clan.setGlowMode(GlowMode.parse(rs.getString("glow_mode")));
                        clan.setGlowColor(rs.getString("glow_color"));
                        map.put(id, clan);
                    }
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT uuid,clan_id,name,role,joined FROM mc_members")) {
                    while (rs.next()) {
                        Clan clan = map.get(UUID.fromString(rs.getString("clan_id")));
                        if (clan != null) {
                            clan.addMember(new ClanMember(UUID.fromString(rs.getString("uuid")),
                                    rs.getString("name"), rs.getString("role"), rs.getLong("joined")));
                        }
                    }
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT clan_id,role_id,display,weight,perms FROM mc_roles")) {
                    while (rs.next()) {
                        Clan clan = map.get(UUID.fromString(rs.getString("clan_id")));
                        if (clan == null || ClanRole.LEADER_ID.equals(rs.getString("role_id"))) {
                            continue;
                        }
                        ClanRole role = new ClanRole(rs.getString("role_id"), rs.getString("display"),
                                rs.getInt("weight"));
                        String perms = rs.getString("perms");
                        if (perms != null && !perms.isBlank()) {
                            for (String p : perms.split(",")) {
                                ClanPermission cp = ClanPermission.parse(p);
                                if (cp != null) {
                                    role.permissions().add(cp);
                                }
                            }
                        }
                        clan.roles().put(role.id(), role);
                    }
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT clan_id,name,world,x,y,z,yaw,pitch FROM mc_homes")) {
                    while (rs.next()) {
                        Clan clan = map.get(UUID.fromString(rs.getString("clan_id")));
                        if (clan != null) {
                            String n = rs.getString("name");
                            clan.homes().put(n.toLowerCase(), new ClanHome(n, rs.getString("world"), rs.getDouble("x"),
                                    rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
                        }
                    }
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT clan_id,quest_id,progress,completed,period_key FROM mc_quests")) {
                    while (rs.next()) {
                        Clan clan = map.get(UUID.fromString(rs.getString("clan_id")));
                        if (clan != null) {
                            QuestProgress qp = new QuestProgress(rs.getString("period_key"));
                            qp.set(rs.getLong("progress"));
                            qp.setCompleted(rs.getInt("completed") != 0);
                            clan.quests().put(rs.getString("quest_id"), qp);
                        }
                    }
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT clan_id,data FROM mc_storage")) {
                    while (rs.next()) {
                        Clan clan = map.get(UUID.fromString(rs.getString("clan_id")));
                        if (clan != null) {
                            clan.setStorageData(rs.getBytes("data"));
                        }
                    }
                }
                return new ArrayList<>(map.values());
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    // ------------------------------------------------------------------ запись

    /**
     * Ставит асинхронное сохранение клана в очередь. Повторные вызовы, пока сохранение ещё не началось,
     * схлопываются в одно.
     */
    public void saveClan(Clan clan) {
        if (clan.saveQueued().compareAndSet(false, true)) {
            executor.execute(() -> {
                clan.saveQueued().set(false);
                doSave(clan);
            });
        }
    }

    private void doSave(Clan clan) {
        try (Connection c = db.connection()) {
            c.setAutoCommit(false);
            try {
                String id = clan.id().toString();
                Clan.Core core = clan.core();
                try (PreparedStatement ps = c.prepareStatement(
                        "REPLACE INTO mc_clans (id,name,tag,leader,created,level,xp,total_xp,money,points,"
                                + "friendly_fire,glow_mode,glow_color) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, id);
                    ps.setString(2, clan.name());
                    ps.setString(3, clan.tag());
                    ps.setString(4, clan.leader().toString());
                    ps.setLong(5, clan.createdAt());
                    ps.setInt(6, core.level());
                    ps.setLong(7, core.xp());
                    ps.setLong(8, core.totalXp());
                    ps.setDouble(9, core.money());
                    ps.setLong(10, core.points());
                    ps.setInt(11, clan.friendlyFire() ? 1 : 0);
                    ps.setString(12, clan.glowMode().name());
                    ps.setString(13, clan.glowColor());
                    ps.executeUpdate();
                }
                deleteChildren(c, id);
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO mc_members (uuid,clan_id,name,role,joined) VALUES (?,?,?,?,?)")) {
                    for (ClanMember m : clan.members()) {
                        ps.setString(1, m.uuid().toString());
                        ps.setString(2, id);
                        ps.setString(3, m.name());
                        ps.setString(4, m.roleId());
                        ps.setLong(5, m.joinedAt());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO mc_roles (clan_id,role_id,display,weight,perms) VALUES (?,?,?,?,?)")) {
                    for (ClanRole r : clan.roles().values()) {
                        if (r.isLeader()) {
                            continue;
                        }
                        ps.setString(1, id);
                        ps.setString(2, r.id());
                        ps.setString(3, r.displayName());
                        ps.setInt(4, r.weight());
                        ps.setString(5, r.permissions().stream().map(Enum::name).collect(Collectors.joining(",")));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO mc_homes (clan_id,name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?,?)")) {
                    for (ClanHome h : clan.homes().values()) {
                        ps.setString(1, id);
                        ps.setString(2, h.name());
                        ps.setString(3, h.world());
                        ps.setDouble(4, h.x());
                        ps.setDouble(5, h.y());
                        ps.setDouble(6, h.z());
                        ps.setFloat(7, h.yaw());
                        ps.setFloat(8, h.pitch());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO mc_quests (clan_id,quest_id,progress,completed,period_key) VALUES (?,?,?,?,?)")) {
                    for (Map.Entry<String, QuestProgress> e : new HashMap<>(clan.quests()).entrySet()) {
                        QuestProgress q = e.getValue();
                        ps.setString(1, id);
                        ps.setString(2, e.getKey());
                        ps.setLong(3, q.value());
                        ps.setInt(4, q.completed() ? 1 : 0);
                        ps.setString(5, q.periodKey());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                clan.setDirty(false);
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить клан " + clan.name(), e);
        }
    }

    private void deleteChildren(Connection c, String id) throws SQLException {
        for (String table : new String[]{"mc_members", "mc_roles", "mc_homes", "mc_quests"}) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE clan_id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        }
    }

    /** Асинхронно удаляет клан и все связанные данные. */
    public void deleteClan(UUID clanId) {
        executor.execute(() -> {
            try (Connection c = db.connection()) {
                c.setAutoCommit(false);
                String id = clanId.toString();
                try {
                    deleteChildren(c, id);
                    for (String table : new String[]{"mc_clans:id", "mc_storage:clan_id", "mc_treasury_log:clan_id"}) {
                        String[] p = table.split(":");
                        try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + p[0] + " WHERE " + p[1] + "=?")) {
                            ps.setString(1, id);
                            ps.executeUpdate();
                        }
                    }
                    c.commit();
                } catch (SQLException ex) {
                    c.rollback();
                    throw ex;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось удалить клан " + clanId, e);
            }
        });
    }

    /** Асинхронно сохраняет содержимое склада (байты уже сериализованы в главном потоке). */
    public void saveStorage(UUID clanId, byte[] data) {
        executor.execute(() -> {
            try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                    "REPLACE INTO mc_storage (clan_id,data) VALUES (?,?)")) {
                ps.setString(1, clanId.toString());
                ps.setBytes(2, data);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить склад клана " + clanId, e);
            }
        });
    }

    /** Добавляет запись в историю казны. */
    public void logTreasury(TreasuryEntry e) {
        executor.execute(() -> {
            try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO mc_treasury_log (clan_id,player,player_name,currency,action,amount,ts) "
                            + "VALUES (?,?,?,?,?,?,?)")) {
                ps.setString(1, e.clanId().toString());
                ps.setString(2, e.player() == null ? null : e.player().toString());
                ps.setString(3, e.playerName());
                ps.setString(4, e.currency());
                ps.setString(5, e.action());
                ps.setDouble(6, e.amount());
                ps.setLong(7, e.timestamp());
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось записать историю казны", ex);
            }
        });
    }

    /** Последние записи истории казны (новые первыми). */
    public CompletableFuture<List<TreasuryEntry>> loadHistory(UUID clanId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<TreasuryEntry> list = new ArrayList<>();
            try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                    "SELECT player,player_name,currency,action,amount,ts FROM mc_treasury_log "
                            + "WHERE clan_id=? ORDER BY id DESC LIMIT ?")) {
                ps.setString(1, clanId.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String p = rs.getString("player");
                        list.add(new TreasuryEntry(clanId, p == null ? null : UUID.fromString(p),
                                rs.getString("player_name"), rs.getString("currency"), rs.getString("action"),
                                rs.getDouble("amount"), rs.getLong("ts")));
                    }
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
            return list;
        }, executor);
    }

    /** Удаляет записи истории старше N дней. */
    public void pruneHistory(int days) {
        if (days <= 0) {
            return;
        }
        executor.execute(() -> {
            try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM mc_treasury_log WHERE ts < ?")) {
                ps.setLong(1, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days));
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось очистить историю казны", e);
            }
        });
    }

    /** Дожидается завершения очереди записи (вызывается при выключении плагина). */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Очередь записи БД не успела завершиться за 20 секунд");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
