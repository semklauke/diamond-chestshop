package com.gmail.sneakdevs.diamondchestshop;

import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.sql.ChestshopDatabaseManager;
import com.gmail.sneakdevs.diamondchestshop.sql.ChestshopSQLiteDatabaseManager;
import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public class DiamondChestShop implements ModInitializer {
    public static final String MODID = "diamondschestshop";
    public static Hologram hologramManager;

    public static ChestshopDatabaseManager getDatabaseManager() {
        return new ChestshopSQLiteDatabaseManager();
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ChestshopCommand.register(dispatcher));
        AutoConfig.register(DiamondChestShopConfig.class, JanksonConfigSerializer::new);
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> hologramManager = new Hologram(server));

        DiamondUtils.registerTable("""
            CREATE TABLE IF NOT EXISTS chestshop (
                id integer PRIMARY KEY AUTOINCREMENT,
                item text NOT NULL,
                nbt text NOT NULL,
                location text DEFAULT NULL,
                valid integer DEFAULT 1 NOT NULL,
                CHECK (valid IN (0,1))
            );
        """);
        DiamondUtils.registerTable("""
            CREATE TABLE IF NOT EXISTS chestshop_trades (
                id integer PRIMARY KEY AUTOINCREMENT,
                shopId integer NOT NULL,
                amount integer NOT NULL,
                price integer NOT NULL,
                buyer text NOT NULL,
                seller text NOT NULL,
                type text NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(shopId) REFERENCES chestshop(id)
            );
        """);
    }

    public static String signTextToReadable(String text) {
        return text.replaceAll("[\\D]", "").toLowerCase();
    }

    public static CompoundTag getNbtData(String text) throws CommandSyntaxException {
        CompoundTag nbt = NbtUtils.snbtToStructure(text);
        if (!text.contains("palette")) nbt.remove("palette");
        return nbt;
    }
}