package com.gmail.sneakdevs.diamondchestshop.sql;

import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface ChestshopDatabaseManager {
    void execute(String sql);
    int addShop(String owner, String item, String nbt, Vec3 location);
    int getMostRecentId();
    void removeShop(int id);
    void logTrade(int shopId, int money, int quantity, String buyer, String seller, String type);
    ShopRecord getShop(int id);
    List<ShopRecord> getAllShops();
    List<ShopRecord> getAllShops(String owner);
    Tuple<Integer, Integer> similarTrades(String player, int shopId, SignBlockEntityInterface.ShopType type);
}