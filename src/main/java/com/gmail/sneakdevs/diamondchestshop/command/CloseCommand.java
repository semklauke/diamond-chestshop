package com.gmail.sneakdevs.diamondchestshop.command;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.sql.ChestshopDatabaseManager;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import com.gmail.sneakdevs.diamondchestshop.sql.ShopRecord;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import com.gmail.sneakdevs.diamondeconomy.command.CommandExceptions;
import net.minecraft.world.level.Level;

public class CloseCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal("close")
                .then(
                        Commands.argument("shopId", IntegerArgumentType.integer(1))
                                .executes(e -> {
                                    int shopId = IntegerArgumentType.getInteger(e, "shopId");
                                    return closeCommand(e, shopId);
                                })
                );
    }

    private static int closeCommand(CommandContext<CommandSourceStack> ctx, int shopId) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayer();
        Level world = ctx.getSource().getLevel();
        if (player == null) {
            throw CommandExceptions.PLAYER_NOT_FOUND.create();
        }
        ChestshopDatabaseManager dm = DiamondChestShop.getDatabaseManager();
        ShopRecord shopRecord = dm.getShop(shopId);
        BlockPos signPos = new BlockPos(shopRecord.location());
        if (world.isClientSide()) return 0;
        BlockEntity be = world.getBlockEntity(signPos);
        BlockState state = world.getBlockState(signPos);
        if (be == null) return 0;
        if (be instanceof SignBlockEntityInterface iSign && iSign.diamondchestshop_getIsShop()) {
            DiamondChestShop.LOGGER.debug("Destroyed shop " + shopId + " via command");
            // get shop entity
            Direction nonFacingDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            BlockPos hangingPos = signPos.offset(nonFacingDir.getStepX(), nonFacingDir.getStepY(), nonFacingDir.getStepZ());
            // if sign belongs to a shop entity, remove that shop
            if (world.getBlockEntity(hangingPos) instanceof BaseContainerBlockEntity shop && ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getIsShop()) {
                DiamondChestShopUtil.destroyShopContainer(shop, world, hangingPos);
            }
            // remove shop from database and SignEntity
            dm.removeShop(shopId);
            iSign.diamondchestshop_removeShop();
            // send success message
            ctx.getSource().sendSuccess(() ->
                    DiamondChestShopConfig.ChatPrefix()
                            .append("Closed your shop #"+shopId+". The sign and container need to be removed manually!")
            , false);
            return 1;
        } else {
            ctx.getSource().sendFailure(
                    Component.empty()
                            .append("Could not find sign at " + signPos.toString())
            );
            return 0;
        }
     }

}
