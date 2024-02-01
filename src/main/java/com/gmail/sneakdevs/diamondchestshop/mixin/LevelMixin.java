package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
    @Inject(method = "removeBlock", at = @At("HEAD"))
    private void diamondchestshop_removeBlockMixin(BlockPos blockPos, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity be = ((Level)(Object)this).getBlockEntity(blockPos);
        if (be instanceof SignBlockEntity && ((SignBlockEntityInterface)be).diamondchestshop_getShop()) {
            removeShopSign(blockPos, be);
        }
        if (be instanceof BaseContainerBlockEntity && ((BaseContainerBlockEntityInterface) be).diamondchestshop_getId() > 0) {
            DiamondChestShop.getDatabaseManager().removeShop(((BaseContainerBlockEntityInterface) be).diamondchestshop_getId());
            DiamondChestShop.hologramManager.removeShopHolo(((BaseContainerBlockEntityInterface) be).diamondchestshop_getId());
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void diamondchestshop_destroyBlockMixin(BlockPos blockPos, boolean bl, Entity entity, int i, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity be = ((Level)(Object)this).getBlockEntity(blockPos);
        if (be instanceof SignBlockEntity && ((SignBlockEntityInterface)be).diamondchestshop_getShop()) {
            removeShopSign(blockPos, be);
        }
        if (be instanceof BaseContainerBlockEntity && ((BaseContainerBlockEntityInterface)be).diamondchestshop_getId() > 0) {
            DiamondChestShop.getDatabaseManager().removeShop(((BaseContainerBlockEntityInterface) be).diamondchestshop_getId());
            DiamondChestShop.hologramManager.removeShopHolo(((BaseContainerBlockEntityInterface) be).diamondchestshop_getId());
        }
    }

    private void removeShopSign(BlockPos blockPos, BlockEntity be) {
        BlockState state = be.getBlockState();
        BlockPos hangingPos = blockPos.offset(state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepX(), state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepY(), state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepZ());
        BlockEntity shop = ((Level) (Object) this).getBlockEntity(hangingPos);
        if (shop instanceof BaseContainerBlockEntity) {
            DiamondChestShop.getDatabaseManager().removeShop(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getId());
            DiamondChestShop.hologramManager.removeShopHolo(((BaseContainerBlockEntityInterface) be).diamondchestshop_getId());
            BlockState shopState = shop.getBlockState();
            if (shopState.getBlock().equals(Blocks.CHEST) && !ChestBlock.getBlockType(shopState).equals(DoubleBlockCombiner.BlockType.SINGLE)) {
                Direction dir = ChestBlock.getConnectedDirection(shopState);
                BlockEntity be2 = ((Level) (Object) this).getBlockEntity(new BlockPos(shop.getBlockPos().getX() + dir.getStepX(), shop.getBlockPos().getY(), shop.getBlockPos().getZ() + dir.getStepZ()));
                ((BaseContainerBlockEntityInterface) be2).diamondchestshop_setShop(false);
            }
        }
    }
}