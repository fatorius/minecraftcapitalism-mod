package com.hugosouza.minecraftcapitalism.service;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.interfaces.Transaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionService {
    public static List<Transaction> getStatement(UUID uuid, int limit, int offset)
            throws SQLException {

        String sql = """
            SELECT from_uuid, to_uuid, amount, type, created_at
            FROM transactions
            WHERE from_uuid = ? OR to_uuid = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;

        List<Transaction> result = new ArrayList<>();

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, uuid.toString());
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new Transaction(
                        rs.getString("from_uuid"),
                        rs.getString("to_uuid"),
                        rs.getInt("amount"),
                        rs.getString("type"),
                        rs.getLong("created_at")
                ));
            }
        }
        return result;
    }


    static void recordTransaction(
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

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
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

}
