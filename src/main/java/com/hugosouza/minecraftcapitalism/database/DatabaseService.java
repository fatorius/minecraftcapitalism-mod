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

    public static void setBalance(UUID uuid, int value) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, value);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        }
    }

    public static void addBalance(UUID uuid, int delta) throws SQLException {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        }
    }

    public static boolean transfer(UUID from, UUID to, int amount) throws SQLException {
        connection.setAutoCommit(false);

        try (
                PreparedStatement check = connection.prepareStatement(
                        "SELECT balance FROM accounts WHERE uuid = ?"
                );
                PreparedStatement debit = connection.prepareStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE uuid = ?"
                );
                PreparedStatement credit = connection.prepareStatement(
                        "UPDATE accounts SET balance = balance + ? WHERE uuid = ?"
                )
        ) {
            check.setString(1, from.toString());
            ResultSet rs = check.executeQuery();

            if (!rs.next() || rs.getInt("balance") < amount) {
                connection.rollback();
                return false;
            }

            debit.setInt(1, amount);
            debit.setString(2, from.toString());
            debit.executeUpdate();

            credit.setInt(1, amount);
            credit.setString(2, to.toString());
            credit.executeUpdate();

            connection.commit();
            return true;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
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
