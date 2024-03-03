package com.gmail.sneakdevs.diamondchestshop.sql;

import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface ChestshopDatabaseManager {
    void execute(String sql);
    int addShop(String item, String nbt, Vec3 location);
    int getMostRecentId();
    String getItem(int id);
    String getNbt(int id);
    void removeShop(int id);
    void logTrade(int shopId, int money, int quantity, String buyer, String seller, String type);
    List<ShopRecord> getAllShops();
    Tuple<Integer, Integer> similarTrades(String player, int shopId, SignBlockEntityInterface.ShopType type);
}