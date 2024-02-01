package com.gmail.sneakdevs.diamondchestshop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public class Hologram {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandSourceStack commandSourceStack;

    public Hologram(MinecraftServer server) {
        Commands commands = server.getCommands();
        dispatcher = commands.getDispatcher();
        commandSourceStack = server.createCommandSourceStack();
    }

    private String getShopName(int shopId) {
        return "shop_" + shopId;
    }

    private void execute(String command) {
        try {
            dispatcher.execute(dispatcher.parse(command, commandSourceStack));
        } catch (CommandSyntaxException ignored) {}
    }

    public void createShopHolo(Player player, int shopId, BlockPos pos) {
        System.out.println("createShopHolo " + getShopName(shopId));
        execute(String.format("hd create %s %s %s %s", getShopName(shopId), pos.getX() + 0.5, pos.getY() + 1.55, pos.getZ() + 0.5));
        execute(String.format("hd modify %s lines add item nbt %s", getShopName(shopId), BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem())));
        execute(String.format("hd modify %s lines remove 0", getShopName(shopId)));
    }

    public void removeShopHolo(int shopId) {
        System.out.println("removeShopHolo " + getShopName(shopId));
        execute(String.format("hd remove %s", getShopName(shopId)));
    }
}
