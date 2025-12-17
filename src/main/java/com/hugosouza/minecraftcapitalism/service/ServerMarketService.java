package com.hugosouza.minecraftcapitalism.service;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.interfaces.ServerMarketListing;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerMarketService {
    public static ServerMarketListing getBuyAdById(int id) throws SQLException {
        String sql = """
                SELECT id, item_id, price
                FROM server_buy_listings
                WHERE id = ?
                """;

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return new ServerMarketListing(
                    rs.getInt("id"),
                    rs.getString("item_id"),
                    rs.getInt("price")
            );
        }
    }

    public static ServerMarketListing getSellAdById(int id) throws SQLException {
        String sql = """
                SELECT id, item_id, price
                FROM server_sell_listings
                WHERE id = ?
                """;

        try (PreparedStatement stmt =
                     DatabaseService.get().prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return new ServerMarketListing(
                    rs.getInt("id"),
                    rs.getString("item_id"),
                    rs.getInt("price")
            );
        }
    }

    public static ArrayList<ServerMarketListing> listBuy(int limit, int offset) throws SQLException {
        String sql = """
            SELECT id, item_id, price
            FROM server_buy_listings
            LIMIT ? OFFSET ?
        """;

        ArrayList<ServerMarketListing> list = new ArrayList<>();

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new ServerMarketListing(
                        rs.getInt("id"),
                        rs.getString("item_id"),
                        rs.getInt("price")
                ));
            }
        }
        return list;
    }

    public static ArrayList<ServerMarketListing> listSell(int limit, int offset, String owner_id) throws SQLException {
        String sql = """
            SELECT id, item_id, price
            FROM server_sell_listings
            LIMIT ? OFFSET ?
        """;

        ArrayList<ServerMarketListing> list = new ArrayList<>();

        try (PreparedStatement stmt = DatabaseService.get().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new ServerMarketListing(
                        rs.getInt("id"),
                        rs.getString("item_id"),
                        rs.getInt("price")
                ));
            }
        }
        return list;
    }
}
