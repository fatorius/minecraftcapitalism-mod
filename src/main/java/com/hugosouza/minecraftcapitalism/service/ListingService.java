package com.hugosouza.minecraftcapitalism.service;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class ListingService {
    public static void create(
            UUID owner,
            String itemId,
            int quantity,
            int unitPrice
    ) throws SQLException {

        String sql = """
            INSERT INTO market_listings
            (owner_uuid, item_id, quantity, unit_price, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setString(1, owner.toString());
            stmt.setString(2, itemId);
            stmt.setInt(3, quantity);
            stmt.setInt(4, unitPrice);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
}
