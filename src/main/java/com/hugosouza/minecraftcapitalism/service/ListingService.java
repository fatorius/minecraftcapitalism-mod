package com.hugosouza.minecraftcapitalism.service;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.interfaces.MarketListing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
            (owner_uuid, item_id, quantity, price, created_at)
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

    public static MarketListing getById(int id) throws SQLException {
        String sql = """
                SELECT id, owner_uuid, item_id, quantity, price, created_at
                FROM market_listings
                WHERE id = ?
                """;

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return new MarketListing(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("owner_uuid")),
                    rs.getString("item_id"),
                    rs.getInt("quantity"),
                    rs.getInt("price"),
                    rs.getLong("created_at")
            );
        }
    }

    public static ArrayList<MarketListing> list(int limit, int offset) throws SQLException {
        String sql = """
            SELECT id, owner_uuid, item_id, quantity, price, created_at
            FROM market_listings
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;

        ArrayList<MarketListing> list = new ArrayList<>();

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new MarketListing(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getString("item_id"),
                        rs.getInt("quantity"),
                        rs.getInt("price"),
                        rs.getLong("created_at")
                ));
            }
        }
        return list;
    }

    public static ArrayList<MarketListing> listMyAds(int limit, int offset, String owner_id) throws SQLException {
        String sql = """
            SELECT id, owner_uuid, item_id, quantity, price, created_at
            FROM market_listings WHERE owner_uuid = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;

        ArrayList<MarketListing> list = new ArrayList<>();

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setString(1, owner_id);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new MarketListing(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getString("item_id"),
                        rs.getInt("quantity"),
                        rs.getInt("price"),
                        rs.getLong("created_at")
                ));
            }
        }
        return list;
    }

    public static boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM market_listings WHERE id = ?";

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
    }

    public static boolean buyListing(
            MarketListing listing,
            UUID buyer
    ) throws SQLException {

        Connection conn = DatabaseService.get();
        conn.setAutoCommit(false);

        try {
            int total = listing.quantity() * listing.unitPrice();

            AccountService.removeBalance(buyer, total, "COMPRA de " + listing.itemId());

            AccountService.addBalance(
                    listing.owner(),
                    total,
                    "VENDA de " + listing.itemId()
            );

            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM market_listings WHERE id = ?"
            )) {
                stmt.setInt(1, listing.id());
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

}
