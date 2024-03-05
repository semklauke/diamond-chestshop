package com.gmail.sneakdevs.diamondchestshop.sql;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface.ShopType;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import com.gmail.sneakdevs.diamondeconomy.sql.SQLiteDatabaseManager;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
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

    public int addShop(String owner, String item, String nbt, Vec3 location) {
        String sql = "INSERT INTO chestshop(item,nbt,location,owner) VALUES(?,?,?,?)";
        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, item);
            pstmt.setString(2, nbt);
            pstmt.setString(3, location.toString());
            pstmt.setString(4, owner);
            pstmt.executeUpdate();
            return getMostRecentId();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public ShopRecord getShop(int id) {
        String sql = "SELECT * FROM shoprecords WHERE shopId = " + id;
        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            if(rs.next())
                return parseShoprecord(rs);
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
        return getAllShops(null);
    }
    public List<ShopRecord> getAllShops(String shopOwner) {
        String sql = "SELECT * FROM shoprecords";
        if (shopOwner != null) sql += " WHERE owner='" + shopOwner + "'";
        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ArrayList<ShopRecord> out = new ArrayList<>();
            while (rs.next()) {
                out.add(parseShoprecord(rs));
            }
            return out;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Tuple<Integer, Integer> similarTrades(String player, int shopId, ShopType type) {
        String sql = """
            SELECT amount, price, buyer, seller FROM (SELECT SUM(amount) as amount, SUM(price) as price, seller, buyer 
            FROM chestshop_trades WHERE "timestamp" >= Datetime('now', '-9 seconds')
            AND shopId = ? GROUP BY """;
        if (type == ShopType.SELL) sql += " buyer ) WHERE buyer = ?;";
        else sql += " seller ) WHERE seller = ?;";

        try (Connection conn = this.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, shopId);
            stmt.setString(2, player);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int sum_amount = rs.getInt("amount");
                int sum_price = rs.getInt("price");
                return new Tuple<>(sum_amount, sum_price);
            }
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

    private ShopRecord parseShoprecord(ResultSet rs) throws SQLException {
        int id = rs.getInt("shopId");
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(rs.getString("item")));
        String ntb = rs.getString("nbt");
        String owner = rs.getString("owner");
        int priceSum = rs.getInt("priceSum");
        int amountSum = rs.getInt("amountSum");
        String location = rs.getString("location");
        ShopType type = ShopType.NONE;
        if (rs.getString("type") != null) {
            if (rs.getString("type").contains("sell"))
                type = ShopType.SELL;
            else if (rs.getString("type").contains("buy"))
                type = ShopType.BUY;
        }

        boolean valid = rs.getInt("valid") == 1;
        // parse location string. Java stream kinda sucks
        int[] cords =  Arrays.stream(location
                .substring(1, location.length() - 1)
                .split(",", 3))
                .map(Double::parseDouble)
                .map(Math::floor)
                .mapToInt(Double::intValue)
                .toArray();
        Vec3i pos = new Vec3i(cords[0], cords[1], cords[2]);

        return new ShopRecord(id, item, ntb, pos, owner, amountSum, priceSum, type, valid);
    }
}