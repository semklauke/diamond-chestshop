package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void diamondchestshop_isPushableMixin(BlockState blockState, Level level, BlockPos blockPos, Direction direction, boolean bl, Direction direction2, CallbackInfoReturnable<Boolean> cir) {
        if (DiamondChestShopConfig.getInstance().shopProtectPiston) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof SignBlockEntity && ((SignBlockEntityInterface) be).diamondchestshop_getIsShop()) {
                cir.setReturnValue(false);
            }
        }
    }
}