package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin extends BlockEntity implements BaseContainerBlockEntityInterface {
    private String diamondchestshop_owner;
    private String diamondchestshop_item;
    private String diamondchestshop_nbt;
    private boolean diamondchestshop_isShop;
    private int diamondchestshop_id;

    public BaseContainerBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public void diamondchestshop_setOwner(String newOwner) {
        this.diamondchestshop_owner = newOwner;
    }
    public void diamondchestshop_setItem(String newItem) {
        this.diamondchestshop_item = newItem;
    }
    public void diamondchestshop_setTag(String newTag) {
        this.diamondchestshop_nbt = newTag;
    }
    public void diamondchestshop_setShop(boolean newShop) {
        this.diamondchestshop_isShop = newShop;
    }
    public void diamondchestshop_setId(int newId){
        this.diamondchestshop_id = newId;
    }

    public String diamondchestshop_getOwner() {
        return this.diamondchestshop_owner;
    }
    public String diamondchestshop_getItem() {
        return this.diamondchestshop_item;
    }
    public String diamondchestshop_getNbt() {
        return this.diamondchestshop_nbt;
    }
    public boolean diamondchestshop_getShop() {
        return this.diamondchestshop_isShop;
    }
    public int diamondchestshop_getId() {
        return this.diamondchestshop_id;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void diamondchestshop_saveAdditionalMixin(CompoundTag nbt, CallbackInfo ci) {
        if (diamondchestshop_owner == null) diamondchestshop_owner = "";
        nbt.putString("diamondchestshop_ShopOwner", diamondchestshop_owner);
        nbt.putBoolean("diamondchestshop_IsShop", diamondchestshop_isShop);
        nbt.putInt("diamondchestshop_Id", diamondchestshop_id);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void diamondchestshop_loadMixin(CompoundTag nbt, CallbackInfo ci) {
        diamondchestshop_isShop = nbt.getBoolean("diamondchestshop_IsShop");
        diamondchestshop_owner = nbt.getString("diamondchestshop_ShopOwner");
        if (nbt.getString("diamondchestshop_NBT").length() > 1) {
            diamondchestshop_item = nbt.getString("diamondchestshop_ShopItem");
            diamondchestshop_nbt = nbt.getString("diamondchestshop_NBT");
            diamondchestshop_id = DiamondChestShop.getDatabaseManager().addShop(diamondchestshop_item, diamondchestshop_nbt, this.getBlockPos().getCenter());
        } else {
            if (nbt.getInt("diamondchestshop_Id") > 0) {
                diamondchestshop_id = nbt.getInt("diamondchestshop_Id");
                diamondchestshop_item = DiamondChestShop.getDatabaseManager().getItem(diamondchestshop_id);
                diamondchestshop_nbt = DiamondChestShop.getDatabaseManager().getNbt(diamondchestshop_id);
            }
        }
    }

    @Inject(method = "canOpen", at = @At("RETURN"), cancellable = true)
    private void diamondchestshop_canOpenMixin(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (AutoConfig.getConfigHolder(DiamondChestShopConfig.class).getConfig().shopProtectPlayerOpen) {
            if (!cir.getReturnValue()) return;
            if (player.isCreative()) return;
            if (diamondchestshop_isShop) {
                if (diamondchestshop_owner.equals(player.getStringUUID())) {
                    cir.setReturnValue(true);
                    return;
                }
                player.displayClientMessage(Component.literal("Cannot open another player's shop"), true);
                cir.setReturnValue(false);
            }
        }
    }
}