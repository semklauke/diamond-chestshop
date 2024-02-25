package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow @Final
    private ObjectArrayList<BlockPos> toBlow;

    @Shadow @Final
    private Level level;

    @Inject(method = "explode", at = @At("TAIL"))
    private void diamondchestshop_explodeMixin(CallbackInfo ci) {
        if (DiamondChestShopConfig.getInstance().shopProtectExplosion) {
            this.toBlow.removeIf((b) -> {
                BlockEntity be = level.getBlockEntity(b);
                return ((be instanceof SignBlockEntity && ((SignBlockEntityInterface) be).diamondchestshop_getIsShop()) || (be instanceof BaseContainerBlockEntity && ((BaseContainerBlockEntityInterface) be).diamondchestshop_getIsShop()));
            });
        }
    }
}