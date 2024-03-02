package com.gmail.sneakdevs.diamondchestshop;

import com.gmail.sneakdevs.diamondchestshop.command.DiamondChestshopCommands;
import com.gmail.sneakdevs.diamondchestshop.command.HelpCommand;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.sql.ChestshopDatabaseManager;
import com.gmail.sneakdevs.diamondchestshop.sql.ChestshopSQLiteDatabaseManager;
import com.gmail.sneakdevs.diamondchestshop.util.ShopDisplayManager;
import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiamondChestShop implements ModInitializer {
    public static final String MODID = "diamondchestshop";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    private static ShopDisplayManager shopDisplayManager;
    public static ShopDisplayManager getShopDisplayManager() {
        if (shopDisplayManager == null) {
            shopDisplayManager = new ShopDisplayManager(false);
        }
        return shopDisplayManager;
    }

    public static ChestshopDatabaseManager getDatabaseManager() {
        return new ChestshopSQLiteDatabaseManager();
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->  DiamondChestshopCommands.register(dispatcher, registryAccess) );
        AutoConfig.register(DiamondChestShopConfig.class, JanksonConfigSerializer::new);
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            DiamondChestShop.getDatabaseManager().execute("""
INSERT INTO sqlite_sequence (name, seq) SELECT 'chestshop', 100 
WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = 'chestshop');
UPDATE sqlite_sequence SET seq=100 WHERE name='chestshop' AND seq < 100;"""
            );
            shopDisplayManager = new ShopDisplayManager(true);
        });

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
}