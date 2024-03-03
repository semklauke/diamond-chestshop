package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopNTB;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin extends BlockEntity implements BaseContainerBlockEntityInterface {
    @Unique
    private String diamondchestshop_owner = null;
    @Unique
    private int diamondchestshop_id = -1;

    public BaseContainerBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public void diamondchestshop_setOwner(String newOwner) {
        this.diamondchestshop_owner = newOwner;
    }
    public void diamondchestshop_setId(int newId){
        this.diamondchestshop_id = newId;
    }
    public void diamondchestshop_removeShop() {
        this.diamondchestshop_owner = null;
        this.diamondchestshop_id = -1;
        SignBlockEntityInterface sign = this.diamondchestshop_findShopSign();
        if (sign != null) {
            sign.diamondchestshop_removeShop();
        }
    }
    public int diamondchestshop_getId() {
        return this.diamondchestshop_id;
    }
    public boolean diamondchestshop_getIsShop() {
        return this.diamondchestshop_id != -1;
    }
    public String diamondchestshop_getOwner() {
        return this.diamondchestshop_owner;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void diamondchestshop_saveAdditionalMixin(CompoundTag nbt, CallbackInfo ci) {
        // if this has an associated shop, save to nbt tags
        if (this.diamondchestshop_id != -1) {
            nbt.putInt(DiamondChestShopNTB.ID, diamondchestshop_id);
            if (this.diamondchestshop_owner == null) diamondchestshop_owner = "";
            nbt.putString(DiamondChestShopNTB.OWNER, diamondchestshop_owner);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void diamondchestshop_loadMixin(CompoundTag nbt, CallbackInfo ci) {
        int id = nbt.getInt(DiamondChestShopNTB.ID);
        // if there was a shop stored, load the nbt tags
        if (id > 0) {
            this.diamondchestshop_id = id;
            this.diamondchestshop_owner = nbt.getString(DiamondChestShopNTB.OWNER);
        }
    }

    @Inject(method = "canOpen", at = @At("RETURN"), cancellable = true)
    private void diamondchestshop_canOpenMixin(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (DiamondChestShopConfig.getInstance().shopProtectPlayerOpen) {
            if (!cir.getReturnValue()) return;
            if (player.isCreative()) return;
            if (diamondchestshop_id > -1) {
                if (diamondchestshop_owner.equals(player.getStringUUID())) {
                    cir.setReturnValue(true);
                    return;
                }
                DiamondChestShopUtil.sendHotbarMessage(player, "Cannot open another player's shop!", DiamondChestShopUtil.ERROR_STYLE);
                cir.setReturnValue(false);
            }
        }
    }

    @Unique
    private SignBlockEntityInterface diamondchestshop_findShopSign() {
        Level world = this.getLevel();
        if (world == null) return null;
        // see if a sign is attached to this block
        SignBlockEntityInterface sign = diamondchestshop_findShopSign(world, this.getBlockPos());
        if (sign != null) return sign;
        // there was no sign around this block -> check if this is a double chest
        BlockEntity dc = DiamondChestShopUtil.getDoubleChest(world, this.getBlockPos());
        if (dc != null) {
            // see if a sing is attached to the double chest block
            return diamondchestshop_findShopSign(world, dc.getBlockPos());
        }
        // could not find sign
        return null;
    }

    @Unique
    private SignBlockEntityInterface diamondchestshop_findShopSign(Level world, BlockPos chestPos) {
        // look around this block to check for SignBlockEntityInterface that belongs to a shop
        for (BlockPos pos : BlockPos.withinManhattan(chestPos, 1, 0, 1)) {
            if (world.getBlockEntity(pos) instanceof SignBlockEntityInterface sign) {
                if (sign.diamondchestshop_getIsShop()) {
                    return sign;
                }
            }
        }
        return null;
    }

}