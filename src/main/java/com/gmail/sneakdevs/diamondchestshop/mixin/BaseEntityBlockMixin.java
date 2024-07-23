package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BaseEntityBlock.class, priority = 800)
public class BaseEntityBlockMixin extends Block {

    public BaseEntityBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (world.isClientSide()) return state;
        var be = world.getBlockEntity(pos);
        switch (be) {
            case null -> {
                return state;
            }
            case SignBlockEntityInterface iSign when iSign.diamondchestshop_getIsShop() -> {
                int shopId = iSign.diamondchestshop_getId();
                DiamondChestShop.LOGGER.debug("Destroyed shop " + shopId + " via Sign");
                // destroying a sign
                // get shop entity
                Direction nonFacingDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
                BlockPos hangingPos = pos.offset(nonFacingDir.getStepX(), nonFacingDir.getStepY(), nonFacingDir.getStepZ());
                // if sign belongs to a shop entity, remove that shop
                if (world.getBlockEntity(hangingPos) instanceof BaseContainerBlockEntity shop && ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getIsShop()) {
                    DiamondChestShopUtil.destroyShopContainer(shop, world, hangingPos);
                }
                // remove shop from database and SignEntity
                DiamondChestShop.getDatabaseManager().removeShop(shopId);
                iSign.diamondchestshop_removeShop();
            }
            case BaseContainerBlockEntity shop when ((BaseContainerBlockEntityInterface) be).diamondchestshop_getIsShop() -> {
                // destroying a container
                int shopId = ((BaseContainerBlockEntityInterface) be).diamondchestshop_getId();
                DiamondChestShop.LOGGER.debug("Destroyed shop " + shopId + " via Container!");
                DiamondChestShop.getDatabaseManager().removeShop(shopId);
                DiamondChestShopUtil.destroyShopContainer(shop, world, pos);
            }
            default -> {
                return super.playerWillDestroy(world, pos, state, player);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
