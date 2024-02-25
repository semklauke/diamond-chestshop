package com.gmail.sneakdevs.diamondchestshop.sql;

import net.minecraft.world.item.Item;
import net.minecraft.core.Vec3i;

public record ShopRecord(
        int id,
        Item item,
        String ntb,
        Vec3i location
) {}