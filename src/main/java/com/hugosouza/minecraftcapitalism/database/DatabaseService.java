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
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                     connection.prepareStatement(
                             "UPDATE accounts SET balance = ? WHERE uuid = ?")) {

            stmt.setInt(1, value);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

            recordTransaction(null, uuid, value, "ADMIN_SET");

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public static void addBalance(UUID uuid, int delta) throws SQLException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                     connection.prepareStatement(
                             "UPDATE accounts SET balance = balance + ? WHERE uuid = ?")) {

            stmt.setInt(1, delta);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

            recordTransaction(null, uuid, delta, "ADMIN_ADD");

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public static boolean transfer(UUID from, UUID to, int amount) throws SQLException {
        connection.setAutoCommit(false);

        try (
                PreparedStatement check =
                        connection.prepareStatement(
                                "SELECT balance FROM accounts WHERE uuid = ?"
                        );
                PreparedStatement debit =
                        connection.prepareStatement(
                                "UPDATE accounts SET balance = balance - ? WHERE uuid = ?"
                        );
                PreparedStatement credit =
                        connection.prepareStatement(
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

            recordTransaction(from, to, amount, "PIX");

            connection.commit();
            return true;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void recordTransaction(
            UUID from,
            UUID to,
            int amount,
            String type
    ) throws SQLException {

        String sql = """
            INSERT INTO transactions
            (from_uuid, to_uuid, amount, type, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (from == null) stmt.setNull(1, Types.VARCHAR);
            else stmt.setString(1, from.toString());

            if (to == null) stmt.setNull(2, Types.VARCHAR);
            else stmt.setString(2, to.toString());

            stmt.setInt(3, amount);
            stmt.setString(4, type);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_uuid TEXT NULL,
                    to_uuid   TEXT NULL,
                    amount INTEGER NOT NULL CHECK (amount > 0),
                    type TEXT NOT NULL, -- PIX, TAX, REWARD, ADMIN_SET, ADMIN_ADD
                    created_at INTEGER NOT NULL, -- epoch millis
                    FOREIGN KEY (from_uuid) REFERENCES accounts(uuid),
                    FOREIGN KEY (to_uuid)   REFERENCES accounts(uuid)
                );
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_from
                ON transactions(from_uuid);
            """);

                stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_to
                ON transactions(to_uuid);
            """);

                stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_created_at
                ON transactions(created_at);
            """);
        }
    }
}
