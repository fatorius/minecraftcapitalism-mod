package com.hugosouza.minecraftcapitalism.service;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;


public class AccountService {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean transfer(UUID from, UUID to, int amount) throws SQLException {
        DatabaseService.get().setAutoCommit(false);

        try (
                PreparedStatement check =
                        DatabaseService.get().prepareStatement(
                                "SELECT balance FROM accounts WHERE uuid = ?"
                        );
                PreparedStatement debit =
                        DatabaseService.get().prepareStatement(
                                "UPDATE accounts SET balance = balance - ? WHERE uuid = ?"
                        );
                PreparedStatement credit =
                        DatabaseService.get().prepareStatement(
                                "UPDATE accounts SET balance = balance + ? WHERE uuid = ?"
                        )
        ) {
            check.setString(1, from.toString());
            ResultSet rs = check.executeQuery();

            if (!rs.next() || rs.getInt("balance") < amount) {
                DatabaseService.get().rollback();
                return false;
            }

            debit.setInt(1, amount);
            debit.setString(2, from.toString());
            debit.executeUpdate();

            credit.setInt(1, amount);
            credit.setString(2, to.toString());
            credit.executeUpdate();

            recordTransaction(from, to, amount, "PIX");

            DatabaseService.get().commit();
            return true;

        } catch (SQLException e) {
            DatabaseService.get().rollback();
            throw e;
        } finally {
            DatabaseService.get().setAutoCommit(true);
        }
    }

    private static void recordTransaction(
            UUID from,
            UUID to,
            int amount,
            String type
    ) throws SQLException {
        int modAmount = Math.abs(amount);

        String sql = """
            INSERT INTO transactions
            (from_uuid, to_uuid, amount, type, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            if (from == null) stmt.setNull(1, Types.VARCHAR);
            else stmt.setString(1, from.toString());

            if (to == null) stmt.setNull(2, Types.VARCHAR);
            else stmt.setString(2, to.toString());

            stmt.setInt(3, modAmount);
            stmt.setString(4, type);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    public static void ensureAccountExists(UUID uuid) {
        String sql = """
            INSERT OR IGNORE INTO accounts (uuid, balance)
            VALUES (?, 0);
        """;

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar conta para {}", uuid, e);
        }
    }

    public static int getBalance(UUID uuid) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE uuid = ?";

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("balance") : 0;
        }
    }

    public static void setBalance(UUID uuid, int value) throws SQLException {
        DatabaseService.get().setAutoCommit(false);

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(
                             "UPDATE accounts SET balance = ? WHERE uuid = ?")) {

            stmt.setInt(1, value);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

            recordTransaction(null, uuid, value, "ADMIN_SET");

            DatabaseService.get().commit();

        } catch (SQLException e) {
            DatabaseService.get().rollback();
            throw e;
        } finally {
            DatabaseService.get().setAutoCommit(true);
        }
    }

    public static void addBalance(UUID uuid, int delta) throws SQLException {
        DatabaseService.get().setAutoCommit(false);

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(
                             "UPDATE accounts SET balance = balance + ? WHERE uuid = ?")) {

            stmt.setInt(1, delta);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

            recordTransaction(null, uuid, delta, "ADMIN_ADD");

            DatabaseService.get().commit();

        } catch (SQLException e) {
            DatabaseService.get().rollback();
            throw e;
        } finally {
            DatabaseService.get().setAutoCommit(true);
        }
    }

    public static void addBalance(UUID uuid, int delta, String transactionType) throws SQLException {
        DatabaseService.get().setAutoCommit(false);

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(
                             "UPDATE accounts SET balance = balance + ? WHERE uuid = ?")) {

            stmt.setInt(1, delta);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

            recordTransaction(null, uuid, delta, transactionType);

            DatabaseService.get().commit();

        } catch (SQLException e) {
            DatabaseService.get().rollback();
            throw e;
        } finally {
            DatabaseService.get().setAutoCommit(true);
        }
    }

    public static void removeBalance(UUID uuid, int delta, String transactionType) throws SQLException {
        DatabaseService.get().setAutoCommit(false);

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(
                             "UPDATE accounts SET balance = balance - ? WHERE uuid = ?")) {

            stmt.setInt(1, delta);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

            recordTransaction(uuid, null, delta, transactionType);

            DatabaseService.get().commit();

        } catch (SQLException e) {
            DatabaseService.get().rollback();
            throw e;
        } finally {
            DatabaseService.get().setAutoCommit(true);
        }
    }
}
