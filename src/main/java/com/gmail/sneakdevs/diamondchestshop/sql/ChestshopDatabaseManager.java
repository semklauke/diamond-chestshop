package com.gmail.sneakdevs.diamondchestshop.sql;

import net.minecraft.world.phys.Vec3;

public interface ChestshopDatabaseManager {
    int addShop(String item, String nbt, Vec3 location);
    int getMostRecentId();
    String getItem(int id);
    String getNbt(int id);
    void removeShop(int id);
    void logTrade(int shopId, int money, int quantity, String buyer, String seller, String type);
}