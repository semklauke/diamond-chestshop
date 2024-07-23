package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopNBT;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import com.gmail.sneakdevs.diamondchestshop.util.ShopDisplayManager;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.Direction;

import java.util.Objects;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityInterface  {

    @Shadow public abstract SignText getFrontText();
    @Unique
    private String diamondchestshop_owner;
    @Unique
    private int diamondchestshop_id;
    @Unique
    private boolean diamondchestshop_isShop;
    @Unique
    private boolean diamondchestshop_isAdminShop;
    @Unique
    private String diamondchestshop_item;
    @Unique
    private String diamondchestshop_nbt;
    @Unique
    private HolderAttachment diamondchestshop_itemDisplay;

    public SignBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }
    public void diamondchestshop_setShop(int id, String owner, String item, String nbt, boolean adminShop) {
        this.diamondchestshop_isShop = true;
        this.diamondchestshop_isAdminShop = adminShop;
        this.diamondchestshop_id = id;
        this.diamondchestshop_nbt = nbt;
        this.diamondchestshop_item = item;
        this.diamondchestshop_owner = owner;
        diamondchestshop_createItemDisplay();
    }
    public void diamondchestshop_removeShop() {
        this.diamondchestshop_isShop = false;
        this.diamondchestshop_isAdminShop = false;
        this.diamondchestshop_id = -1;
        String emptyString = "";
        this.diamondchestshop_nbt = emptyString;
        this.diamondchestshop_item = emptyString;
        this.diamondchestshop_owner = emptyString;
        if (this.diamondchestshop_itemDisplay != null)
            this.diamondchestshop_itemDisplay.destroy();
    }
    public void diamondchestshop_setNbt(String newNbt) {
        this.diamondchestshop_nbt = newNbt;
    }
    public void diamondchestshop_setIsAdminShop(boolean newAdminShop) {
        this.diamondchestshop_isAdminShop = newAdminShop;
    }
    public void diamondchestshop_setOwner(String newOwner) {
        this.diamondchestshop_owner = newOwner;
    }
    public boolean diamondchestshop_getIsAdminShop() {
        return this.diamondchestshop_isAdminShop;
    }
    public boolean diamondchestshop_getIsShop() {
        return this.diamondchestshop_isShop;
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
    public int diamondchestshop_getId() {
        return this.diamondchestshop_id;
    }
    public int diamondchestshop_getPrice() {
        try {
            String line3 = DiamondChestShopUtil.signTextToReadable(this.getFrontText().getMessage(2, true).getString());
            return Integer.parseInt(line3.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
    public int diamondchestshop_getQuantity() {
        try {
            String line2 = DiamondChestShopUtil.signTextToReadable(this.getFrontText().getMessage(1,true).getString());
            return Integer.parseInt(line2.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public ShopType diamondchestshop_getShopType() {
        String line1 = this.getFrontText().getMessage(0,true).getString().toLowerCase();
        if (line1.contains(DiamondChestShopConfig.getInstance().buyKeyword))
            return ShopType.BUY;
        if (line1.contains(DiamondChestShopConfig.getInstance().sellKeyword))
            return ShopType.SELL;
        return ShopType.NONE;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void diamondchestshop_saveAdditionalMixin(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        // save if this is a shop sign
        nbt.putBoolean(DiamondChestShopNBT.IS_SHOP, this.diamondchestshop_isShop);
        // for shop signs save the data
        if (this.diamondchestshop_isShop) {
            if (this.diamondchestshop_owner == null) diamondchestshop_owner = "";
            nbt.putString(DiamondChestShopNBT.OWNER, diamondchestshop_owner);
            nbt.putBoolean(DiamondChestShopNBT.IS_ADMIN_SHOP, diamondchestshop_isAdminShop);
            nbt.putInt(DiamondChestShopNBT.ID, diamondchestshop_id);
            nbt.putString(DiamondChestShopNBT.ITEM, diamondchestshop_item);
            if (!Objects.equals(diamondchestshop_nbt, ""))
                nbt.putString(DiamondChestShopNBT.NTB, diamondchestshop_nbt);
        }
        // else {} TODO: if this isn't a shop, should this remove the nbt tags
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void diamondchestshop_loadMixin(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        this.diamondchestshop_isShop = nbt.getBoolean(DiamondChestShopNBT.IS_SHOP);
        // if this should be shop, load data
        if (this.diamondchestshop_isShop) {
            this.diamondchestshop_owner = nbt.getString(DiamondChestShopNBT.OWNER);
            this.diamondchestshop_isAdminShop = nbt.getBoolean(DiamondChestShopNBT.IS_ADMIN_SHOP);
            this.diamondchestshop_id = nbt.getInt(DiamondChestShopNBT.ID);
            if (this.diamondchestshop_id == 0) this.diamondchestshop_id = -1;
            this.diamondchestshop_item = nbt.getString(DiamondChestShopNBT.ITEM);
            this.diamondchestshop_nbt = nbt.getString(DiamondChestShopNBT.NTB);
            // queue for item display creation
            ShopDisplayManager.registerItemDisplayCreation(this);
        }
    }

    @Unique
    public void diamondchestshop_createItemDisplay() {
        ServerLevel world = (ServerLevel) this.getLevel();
        // if the world isn't loaded yes, abort
        if (!(world instanceof ServerLevel)) return;
        // destroy old display
        if (this.diamondchestshop_itemDisplay != null)
            this.diamondchestshop_itemDisplay.destroy();
        // determine position of the item display
        Direction oppositeBlockDir = this.getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        BlockPos hangingPos = this.getBlockPos().offset(oppositeBlockDir.getStepX(), oppositeBlockDir.getStepY(), oppositeBlockDir.getStepZ());
        Vec3 displayPos = hangingPos.getCenter();
        // if this is a double chest, shift position in the center of the chest
        BlockState shopBlock = world.getBlockState(hangingPos);
        if (shopBlock.getBlock().equals(Blocks.CHEST) && !ChestBlock.getBlockType(shopBlock).equals(DoubleBlockCombiner.BlockType.SINGLE)) {
            Direction dir = ChestBlock.getConnectedDirection(shopBlock);
            displayPos = displayPos.add(new Vec3(0.5 * dir.getStepX(), 0.5 * dir.getStepY(), 0.5 * dir.getStepZ()));
        }
        this.diamondchestshop_itemDisplay = ShopDisplayManager.attachItemDisplay(this.diamondchestshop_item, (ServerLevel) this.getLevel(), displayPos);
    }

}