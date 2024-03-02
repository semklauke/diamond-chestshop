package com.gmail.sneakdevs.diamondchestshop.config;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;

@Config(name = DiamondChestShop.MODID)
public class DiamondChestShopConfig implements ConfigData {
    private static final String DIAMOND_ECONOMY_PREFIX = "DIAMOND_ECONOMY_PREFIX";
    @Comment("Name of the command this plugin uses")
    public String chestshopCommandName = "chestshop";
    @Comment("Whether or not to use the base diamond economy command")
    public boolean useBaseCommand = false;
    @Comment("What to protect chest shops from")
    public boolean shopProtectPlayerOpen = true;
    public boolean shopProtectPlayerBreak = true;
    public boolean shopProtectExplosion = true;
    public boolean shopProtectPiston = true;
    public boolean shopProtectHopper = true;
    @Comment("Note: currently doesn't protect double chests")
    public boolean shopProtectHopperMinecart = true;
    @Comment("Keyword for selling or buying shop. These go in the first line of a shop sign.")
    public String buyKeyword = "buy";
    public String sellKeyword = "sell";
    @Comment("Use currency prefix/suffix from the DiamondEconomy config")
    public boolean useDiamondEconomyCurrencyConfig = true;
    @Comment("Use chat prefix from the DiamondEconomy config")
    public boolean useDiamondEconomyChatPrefix = true;

    @Comment("Prefix for messages above the hotbar (use \""+DIAMOND_ECONOMY_PREFIX+"\" if you want to use the prefix from the DiamondEconomy config )")
    public String hotbarMessagePrefix = DIAMOND_ECONOMY_PREFIX;

    public static DiamondChestShopConfig getInstance() {
        return AutoConfig.getConfigHolder(DiamondChestShopConfig.class).getConfig();
    }
    public static MutableComponent ChatPrefix() {
        var inst = DiamondChestShopConfig.getInstance();
        if (inst.useDiamondEconomyChatPrefix) {
            return DiamondEconomyConfig.ChatPrefix();
        } else {
            return Component.empty();
        }
    }

    public static MutableComponent HotbarPrefix() {
        var inst = DiamondChestShopConfig.getInstance();
        if (inst.hotbarMessagePrefix.equals(DIAMOND_ECONOMY_PREFIX)) {
            return DiamondEconomyConfig.ChatPrefix();
        } else {
            return Component.literal(inst.hotbarMessagePrefix);
        }
    }

    public static MutableComponent currencyToLiteral(int c) {
        var inst = DiamondChestShopConfig.getInstance();
        if (inst.useDiamondEconomyChatPrefix) {
            return DiamondEconomyConfig.currencyToLiteral(c);
        } else {
            return Component.literal("$" + c);
        }
    }

}