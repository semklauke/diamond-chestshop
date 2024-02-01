package com.gmail.sneakdevs.diamondchestshop.sql;

import com.gmail.sneakdevs.diamondeconomy.sql.SQLiteDatabaseManager;
import net.minecraft.world.phys.Vec3;

import java.sql.*;

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