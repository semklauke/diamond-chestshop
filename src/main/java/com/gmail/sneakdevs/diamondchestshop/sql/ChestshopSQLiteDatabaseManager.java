package com.gmail.sneakdevs.diamondchestshop.sql;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondeconomy.sql.SQLiteDatabaseManager;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.*;

public class ChestshopSQLiteDatabaseManager implements ChestshopDatabaseManager {
    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(SQLiteDatabaseManager.url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void execute(String sql) {
        try (var conn = this.connect(); var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int addShop(String item, String nbt, Vec3 location) {
        String sql = "INSERT INTO chestshop(item,nbt,location) VALUES(?,?,?)";
        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, item);
            pstmt.setString(2, nbt);
            pstmt.setString(3, location.toString());
            pstmt.executeUpdate();
            return getMostRecentId();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getItem(int id) {
        String sql = "SELECT item FROM chestshop WHERE id = " + id;
        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            if(rs.next())
                return rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getNbt(int id) {
        String sql = "SELECT nbt FROM chestshop WHERE id = " + id;
        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            if(rs.next())
                return rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeShop(int id) {
        // not delete, just invalidate
        // String sql = "DELETE FROM chestshop WHERE id = ?";
        String sql = "UPDATE chestshop SET valid = 0 WHERE id = ?";
        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getMostRecentId() {
        String sql = "SELECT id FROM chestshop ORDER BY id DESC LIMIT 1";
        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<ShopRecord> getAllShops() {
        String sql = "SELECT id, item, nbt, location FROM chestshop WHERE valid = 1 ORDER BY id DESC";
        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ArrayList<ShopRecord> out = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(rs.getString("item")));
                String ntb = rs.getString("nbt");
                String location = rs.getString("location");
                // parse location string. Java stream kinda sucks
                int[] cords =  Arrays.stream(location
                        .substring(1, location.length() - 1)
                        .split(",", 3))
                        .map(Double::parseDouble)
                        .map(Math::floor)
                        .mapToInt(Double::intValue)
                        .toArray();
                Vec3i pos = new Vec3i(cords[0], cords[1], cords[2]);
                // add Record to output list
                out.add(new ShopRecord(id, item, ntb, pos));
            }
            return out;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void logTrade(int shopId, int amount, int price, String buyer, String seller, String type) {
        String sql = "INSERT INTO chestshop_trades(shopId,amount,price,buyer,seller,type) VALUES(?,?,?,?,?,?)";
        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, shopId);
            pstmt.setInt(2, amount);
            pstmt.setInt(3, price);
            pstmt.setString(4, buyer);
            pstmt.setString(5, seller);
            pstmt.setString(6, type);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}