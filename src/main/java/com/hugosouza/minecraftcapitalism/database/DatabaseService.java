package com.hugosouza.minecraftcapitalism.database;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

public final class DatabaseService {
    private static Connection connection;
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init(Path worldPath) throws SQLException {
        Path dbPath = worldPath.resolve("capitalism.db").toAbsolutePath().normalize();
        String url = "jdbc:sqlite:" + dbPath;

        connection = DriverManager.getConnection(url);

        initPragmas();
        initSchema();
    }

    public static Connection get() {
        return connection;
    }

    public static void ensureAccountExists(UUID uuid) {
        String sql = """
            INSERT OR IGNORE INTO accounts (uuid, balance)
            VALUES (?, 0);
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar conta para {}", uuid, e);
        }
    }

    public static int getBalance(UUID uuid) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("balance") : 0;
        }
    }

    private static void initPragmas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA foreign_keys=ON;");
        }
    }

    private static void initSchema() throws SQLException {
        LOGGER.info("Iniciando tabelas no banco de dados do capitalismo");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    uuid TEXT PRIMARY KEY,
                    balance INTEGER NOT NULL
                );
            """);
        }
    }
}
