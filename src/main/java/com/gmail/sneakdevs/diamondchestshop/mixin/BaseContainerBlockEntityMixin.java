package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopNTB;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin implements BaseContainerBlockEntityInterface {
    @Unique
    private String diamondchestshop_owner = null;
    @Unique
    private int diamondchestshop_id = -1;

    public void diamondchestshop_setOwner(String newOwner) {
        this.diamondchestshop_owner = newOwner;
    }
    public void diamondchestshop_setId(int newId){
        this.diamondchestshop_id = newId;
    }
    public void diamondchestshop_removeShop() {
        this.diamondchestshop_owner = null;
        this.diamondchestshop_id = -1;
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
//        diamondchestshop_isShop = nbt.getBoolean("diamondchestshop_IsShop");
//        diamondchestshop_owner = nbt.getString("diamondchestshop_ShopOwner");
//        if (nbt.getString("diamondchestshop_NBT").length() > 1) {
//            diamondchestshop_item = nbt.getString("diamondchestshop_ShopItem");
//            diamondchestshop_nbt = nbt.getString("diamondchestshop_NBT");
//            diamondchestshop_id = DiamondChestShop.getDatabaseManager().addShop(diamondchestshop_item, diamondchestshop_nbt, this.getBlockPos().getCenter());
//        } else {
//            if (nbt.getInt("diamondchestshop_Id") > 0) {
//                diamondchestshop_id = nbt.getInt("diamondchestshop_Id");
//                diamondchestshop_item = DiamondChestShop.getDatabaseManager().getItem(diamondchestshop_id);
//                diamondchestshop_nbt = DiamondChestShop.getDatabaseManager().getNbt(diamondchestshop_id);
//            }
//        }
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
                player.displayClientMessage(Component.literal("Cannot open another player's shop"), true);
                cir.setReturnValue(false);
            }
        }
    }
}