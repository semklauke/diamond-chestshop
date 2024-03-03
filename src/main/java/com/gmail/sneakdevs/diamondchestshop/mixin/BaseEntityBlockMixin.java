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
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        DiamondChestShop.LOGGER.info("Called playerWillDestroy in "+ this.getClass());
        if (world.isClientSide()) return;
        var be = world.getBlockEntity(pos);
        if (be == null) return;
        if (be instanceof SignBlockEntityInterface iSign && iSign.diamondchestshop_getIsShop()) {
            DiamondChestShop.LOGGER.info("Destroyed shop " + iSign.diamondchestshop_getId() + " via Sign");
            // destroying a sign
            // get shop entity
            Direction nonFacingDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            BlockPos hangingPos = pos.offset(nonFacingDir.getStepX(), nonFacingDir.getStepY(), nonFacingDir.getStepZ());
            // if sign belongs to a shop entity, remove that shop
            if (world.getBlockEntity(hangingPos) instanceof BaseContainerBlockEntity shop && ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getIsShop()) {
                diamondchestshop_destroyShopContainer(shop, world, hangingPos);
            }
            // remove shop from database and SignEntity
            int shopId = iSign.diamondchestshop_getId();
            DiamondChestShop.getDatabaseManager().removeShop(shopId);
            iSign.diamondchestshop_removeShop();
        } else if (be instanceof BaseContainerBlockEntity shop && ((BaseContainerBlockEntityInterface)be).diamondchestshop_getIsShop()) {
            // destroying a container
            int shopId = ((BaseContainerBlockEntityInterface)be).diamondchestshop_getId();
            DiamondChestShop.LOGGER.info("Destroyed shop " + shopId + " via Container!");
            DiamondChestShop.getDatabaseManager().removeShop(shopId);
            diamondchestshop_destroyShopContainer(shop, world, pos);
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    @Unique
    private static void diamondchestshop_destroyShopContainer(BaseContainerBlockEntity shop, Level world, BlockPos pos) {
        BaseContainerBlockEntityInterface iShop = (BaseContainerBlockEntityInterface) shop;
        iShop.diamondchestshop_removeShop();
        shop.setChanged();
        // check if this a double chest
        BlockEntity dc = DiamondChestShopUtil.getDoubleChest(world, pos);
        if (dc != null) {
            ((BaseContainerBlockEntityInterface) dc).diamondchestshop_removeShop();
            dc.setChanged();
        }
    }
}
