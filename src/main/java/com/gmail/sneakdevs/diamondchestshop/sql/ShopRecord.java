package com.gmail.sneakdevs.diamondchestshop.sql;

import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import net.minecraft.world.item.Item;
import net.minecraft.core.Vec3i;

public record ShopRecord(
        int id,
        Item item,
        String ntb,
        Vec3i location,
        String owner,
        int amountSum,
        int priceSum,
        SignBlockEntityInterface.ShopType type,
        boolean valid
) {}