package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
    @Inject(method = "removeBlock", at = @At("HEAD"))
    private void diamondchestshop_removeBlockMixin(BlockPos blockPos, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        this.removeShopSign(blockPos);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void diamondchestshop_destroyBlockMixin(BlockPos blockPos, boolean bl, Entity entity, int i, CallbackInfoReturnable<Boolean> cir) {
        this.removeShopSign(blockPos);
    }

    @Unique
    private void removeShopSign(BlockPos blockPos) {
        Level world = ((Level)(Object)this);
        BlockState state = world.getBlockState(blockPos);
        DiamondChestShopUtil.diamondchestshop_destroyShop(world,blockPos,state);
    }
}