package com.hugosouza.minecraftcapitalism.database;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.*;

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


    public static void markAsPaid(int id) throws SQLException {
        String sql = "UPDATE invoices SET status = 'PAID' WHERE id = ?";
        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public static void markAsCancelled(int id) throws SQLException {
        String sql = "UPDATE invoices SET status = 'CANCELLED' WHERE id = ?";
        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }



    private static void initPragmas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA foreign_keys=ON;");
        }
    }

    private static void createAccountTable(Statement stmt) throws SQLException {
        stmt.execute("""
                   CREATE TABLE IF NOT EXISTS accounts (
                        uuid TEXT PRIMARY KEY,
                        INTEGER NOT NULL
                   );
                """);
    }

    private static void createInvoiceTable(Statement stmt) throws SQLException {
        stmt.execute("""
                   CREATE TABLE IF NOT EXISTS invoices (
                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                       from_uuid TEXT NOT NULL,
                       to_uuid TEXT NOT NULL,
                       amount INTEGER NOT NULL CHECK (amount > 0),
                       status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED
                       created_at INTEGER NOT NULL,
                       FOREIGN KEY(from_uuid) REFERENCES accounts(uuid),
                       FOREIGN KEY(to_uuid) REFERENCES accounts(uuid)
                   );
                """);
    }

    private static void createTransactionsTable(Statement stmt) throws SQLException {
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

    private static void initSchema() throws SQLException {
        LOGGER.info("Iniciando tabelas no banco de dados do capitalismo");

        try (Statement stmt = connection.createStatement()) {
            createAccountTable(stmt);
            createTransactionsTable(stmt);
            createInvoiceTable(stmt);
        }
    }
}
