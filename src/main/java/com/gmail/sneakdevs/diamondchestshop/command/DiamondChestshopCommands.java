package com.gmail.sneakdevs.diamondchestshop.command;

import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.function.Consumer;

public class DiamondChestshopCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        Consumer<LiteralArgumentBuilder<CommandSourceStack>> registerFunc;
        var chestShopConfig = DiamondChestShopConfig.getInstance();
        var economyConfig = DiamondEconomyConfig.getInstance();
        if (chestShopConfig.useBaseCommand) {
            registerFunc = (builder) -> dispatcher.register(
                    Commands.literal(economyConfig.commandName)
                            .then(Commands.literal(chestShopConfig.chestshopCommandName))
                            .then(builder)
            );
        } else {
            registerFunc = (builder) -> dispatcher.register(Commands.literal(chestShopConfig.chestshopCommandName).then(builder));
        }

        registerFunc.accept(HelpCommand.buildCommand());
        registerFunc.accept(ListCommand.buildCommand());
        registerFunc.accept(CloseCommand.buildCommand());

    }
}
