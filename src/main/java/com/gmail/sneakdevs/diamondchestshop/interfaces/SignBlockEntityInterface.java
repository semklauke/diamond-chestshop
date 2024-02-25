package com.gmail.sneakdevs.diamondchestshop.interfaces;

import net.minecraft.world.item.Item;

public interface SignBlockEntityInterface {
    void diamondchestshop_setShop(int id, String owner, String item, String nbt, boolean adminShop);
    void diamondchestshop_setNbt(String newNbt);
    void diamondchestshop_setIsAdminShop(boolean newAdminShop);
    void diamondchestshop_removeShop();
    void diamondchestshop_setOwner(String newOwner);
    String diamondchestshop_getOwner();
    int diamondchestshop_getId();
    boolean diamondchestshop_getIsShop();
    boolean diamondchestshop_getIsAdminShop();
    String diamondchestshop_getItem();
    String diamondchestshop_getNbt();
    int diamondchestshop_getQuantity();
    int diamondchestshop_getPrice();
    ShopType diamondchestshop_getShopType();
    void diamondchestshop_createItemDisplay();
    enum ShopType {
        NONE, SELL, BUY
    }
}