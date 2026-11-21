package org.github.sawervatoryy.mirageClans.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.github.sawervatoryy.mirageClans.MirageClans;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/** Пул соединений HikariCP: SQLite (по умолчанию) или MySQL/MariaDB. */
public final class Database implements AutoCloseable {

    public enum Type { SQLITE, MYSQL, MARIADB }

    private final HikariDataSource dataSource;
    private final Type type;

    public Database(MirageClans plugin, YamlConfiguration cfg) {
        Type t;
        try {
            t = Type.valueOf(cfg.getString("database.type", "SQLITE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            t = Type.SQLITE;
        }
        this.type = t;

        HikariConfig hc = new HikariConfig();
        hc.setPoolName("MirageClans-Pool");
        if (type == Type.SQLITE) {
            plugin.getDataFolder().mkdirs();
            File file = new File(plugin.getDataFolder(), cfg.getString("database.sqlite-file", "clans.db"));
            hc.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hc.setDriverClassName("org.sqlite.JDBC");
            hc.setMaximumPoolSize(1);
            hc.setMinimumIdle(1);
            hc.setConnectionInitSql("PRAGMA journal_mode=WAL");
        } else {
            String host = cfg.getString("database.host", "localhost");
            int port = cfg.getInt("database.port", 3306);
            String db = cfg.getString("database.name", "mirageclans");
            String ssl = cfg.getBoolean("database.use-ssl", false) ? "trust" : "disable";
            // MariaDB Connector/J одинаково работает и с MySQL, и с MariaDB
            hc.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + db
                    + "?useUnicode=true&characterEncoding=utf8&sslMode=" + ssl);
            hc.setDriverClassName("org.mariadb.jdbc.Driver");
            hc.setUsername(cfg.getString("database.user", "root"));
            hc.setPassword(cfg.getString("database.password", ""));
            hc.setMaximumPoolSize(Math.max(2, cfg.getInt("database.pool-size", 6)));
        }
        this.dataSource = new HikariDataSource(hc);
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean sqlite() {
        return type == Type.SQLITE;
    }

    public Type type() {
        return type;
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
