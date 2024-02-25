package com.gmail.sneakdevs.diamondchestshop.interfaces;

public interface BaseContainerBlockEntityInterface {
    void diamondchestshop_setOwner(String newOwner);
    void diamondchestshop_setId(int newId);
    void diamondchestshop_removeShop();
    String diamondchestshop_getOwner();
    int diamondchestshop_getId();
    boolean diamondchestshop_getIsShop();
}