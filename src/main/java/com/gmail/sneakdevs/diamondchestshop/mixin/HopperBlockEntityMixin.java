package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(value = HopperBlockEntity.class, priority = 100)
public class HopperBlockEntityMixin {
    @Inject(method = "tryMoveItems", at = @At("HEAD"), cancellable = true)
    private static void diamondchestshop_tryMoveItemsMixin(Level level, BlockPos blockPos, BlockState blockState, HopperBlockEntity hopperBlockEntity, BooleanSupplier booleanSupplier, CallbackInfoReturnable<Boolean> cir) {
        if (!DiamondChestShopConfig.getInstance().shopProtectHopper)
            return;
        if (level.getBlockEntity(blockPos.above()) instanceof BaseContainerBlockEntity shop) {
            BaseContainerBlockEntityInterface iShop = (BaseContainerBlockEntityInterface) shop;
            if (iShop.diamondchestshop_getIsShop() &&
                    !iShop.diamondchestshop_getOwner().equals(((BaseContainerBlockEntityInterface)hopperBlockEntity).diamondchestshop_getOwner())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void diamondchestshop_suckInItemsMixin(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (DiamondChestShopConfig.getInstance().shopProtectHopperMinecart && hopper instanceof MinecartHopper) {
            Container container = HopperBlockEntity.getContainerAt(level, new BlockPos((int)hopper.getLevelX(), (int)hopper.getLevelY() + 1, (int)hopper.getLevelZ()));
            if (container instanceof BaseContainerBlockEntity && ((BaseContainerBlockEntityInterface)container).diamondchestshop_getIsShop()) {
                cir.setReturnValue(false);
            }
        }
    }
}