package com.hugosouza.minecraftcapitalism.service;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.interfaces.Invoice;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class InvoiceService {
    public static int createInvoice(UUID from, UUID to, int amount) throws SQLException {
        String sql = "INSERT INTO invoices (from_uuid, to_uuid, amount, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, from.toString());
            stmt.setString(2, to.toString());
            stmt.setInt(3, amount);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
            else throw new SQLException("Não foi possível criar a cobrança");
        }
    }

    public static Invoice getInvoice(int id) throws SQLException {
        String sql = "SELECT id, from_uuid, to_uuid, amount, status, created_at FROM invoices WHERE id = ?";
        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;

            return new Invoice(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("from_uuid")),
                    UUID.fromString(rs.getString("to_uuid")),
                    rs.getInt("amount"),
                    rs.getString("status"),
                    rs.getLong("created_at")
            );
        }
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
}
