package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerGameMode.class, priority = 999)
public class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Final
    @Shadow
    protected ServerPlayer player;

    @Shadow
    private int gameTicks;

    @Inject(method = "destroyAndAck", at = @At("HEAD"), cancellable = true)
    private void diamondchestshop_destroyAndAckMixin(BlockPos blockPos, int i, String string, CallbackInfo ci) {
        if (DiamondChestShopConfig.getInstance().shopProtectPlayerBreak) {
            if (player.isCreative()) return;
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be == null) return;

            if (be instanceof BaseContainerBlockEntityInterface shop) {
                if (!shop.diamondchestshop_getIsShop()) return;
                if (shop.diamondchestshop_getOwner().equals(player.getStringUUID())) return;
            } else if (be instanceof SignBlockEntityInterface sign) {
                if (!sign.diamondchestshop_getIsShop()) return;
                if (sign.diamondchestshop_getOwner().equals(player.getStringUUID())) return;
            } else return;

            // cancel block destroy
            // TODO: do we really need the ack here ? we could just inject in ServerPlayerGameMode::destoryBlock() and return false.
            this.player.connection.send(new ClientboundBlockUpdatePacket(level, blockPos));
            be = level.getBlockEntity(blockPos);
            if (be != null) {
                Packet<ClientGamePacketListener> updatePacket = be.getUpdatePacket();
                if (updatePacket != null) {
                    this.player.connection.send(updatePacket);
                }
            }
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "useItemOn", cancellable = true)
    private void diamondchestshop_useItemMixin(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.getBlockEntity(blockHitResult.getBlockPos()) instanceof SignBlockEntity sign) {
            SignBlockEntityInterface iSign = (SignBlockEntityInterface) sign;
            // trigger a sell/buy process if the sign belongs to a shop and the player doesn't use a command block (admin shop)
            if (iSign.diamondchestshop_getIsShop() && !itemStack.getItem().equals(Items.COMMAND_BLOCK)) {
                var shopType = iSign.diamondchestshop_getShopType();
                if (shopType == SignBlockEntityInterface.ShopType.NONE) {
                    // early return if this not a shop
                    return;
                } else if (shopType == SignBlockEntityInterface.ShopType.SELL) {
                    // shop sells something
                    sellShop(sign, level.getBlockState(blockHitResult.getBlockPos()), blockHitResult.getBlockPos());
                    cir.setReturnValue(InteractionResult.SUCCESS);
                } else {
                    // shop buys something
                    buyShop(sign, level.getBlockState(blockHitResult.getBlockPos()), blockHitResult.getBlockPos());
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
            }
        }
    }

    @Inject(method = "incrementDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void diamondchestshop_incrementDestroyProgressMixin(BlockState blockState, BlockPos blockPos, int j, CallbackInfoReturnable<Float> cir) {
        if (j + 1 != gameTicks) return;
        // is this a shop sign ?
        if (level.getBlockEntity(blockPos) instanceof SignBlockEntityInterface iSign && iSign.diamondchestshop_getIsShop()) {
            // this an admin shop or not the players own shop
            if (iSign.diamondchestshop_getOwner().equals(player.getStringUUID()) || iSign.diamondchestshop_getIsAdminShop()) {
                BlockState state = level.getBlockState(blockPos);
                var shopType = iSign.diamondchestshop_getShopType();
                if (shopType == SignBlockEntityInterface.ShopType.BUY) {
                    buyShop((SignBlockEntity) iSign, state, blockPos);
                    cir.setReturnValue(0.0F);
                }
                if (shopType == SignBlockEntityInterface.ShopType.SELL) {
                    sellShop((SignBlockEntity) iSign, state, blockPos);
                    cir.setReturnValue(0.0F);
                }
            }
        }
    }

    @Unique
    private void sellShop(SignBlockEntity sign, BlockState state, BlockPos blockPos) {
        SignBlockEntityInterface iSign = (SignBlockEntityInterface) sign;
        // get container block of shop
        Direction oppSignDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        BlockPos hangingPos = blockPos.offset(oppSignDir.getStepX(), oppSignDir.getStepY(),  oppSignDir.getStepZ());
        BaseContainerBlockEntityInterface shop;
        // exit if this sign is not attached to a valid shop block
        if (level.getBlockEntity(hangingPos) instanceof RandomizableContainerBlockEntity containerBe)
            shop = (BaseContainerBlockEntityInterface) containerBe;
        else return;
        if (!shop.diamondchestshop_getIsShop()) return;
        // gather shop data
        int shopQuantity = iSign.diamondchestshop_getQuantity();
        int shopPrice = iSign.diamondchestshop_getPrice();
        String owner = iSign.diamondchestshop_getOwner();
        Item sellItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(iSign.diamondchestshop_getItem()));

        // gather balances from the database
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        int playerBalance = dm.getBalanceFromUUID(player.getStringUUID());
        int shopBalance = dm.getBalanceFromUUID(owner);
        // check if balance is sufficient/ready to receive fund
        if (playerBalance < shopPrice) {
            player.displayClientMessage(Component.literal("You don't have enough money"), true);
            return;
        }
        if (Integer.MAX_VALUE - shopBalance < shopPrice && !iSign.diamondchestshop_getIsAdminShop()) {
            player.displayClientMessage(Component.literal("The owner is too rich"), true);
            return;
        }

        // remove items from container (only if not admin shop)
        if (!iSign.diamondchestshop_getIsAdminShop()) {
            Block shopBlock = level.getBlockState(hangingPos).getBlock();
            // get inventory of the shop block
            Container inventory;
            if (shop instanceof ChestBlockEntity && shopBlock instanceof ChestBlock) {
                Container container = ChestBlock.getContainer((ChestBlock) shopBlock, level.getBlockState(hangingPos), level, hangingPos, true);
                if (container != null) {
                    inventory = container;
                } else {
                    DiamondChestShop.LOGGER.error("Shop sign seems to be attached to a block that is not a container, but a shop was created.");
                    return;
                }
            } else {
                inventory = (Container) shop;
            }

            // first if check shop has item in proper quantity
            // calculate how many items the shop has to offer
            // TODO: use inventory.countItem()
            int itemCount = 0;
            int firstItemIndex = -1;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item.getItem().equals(sellItem) && (!item.hasTag() || item.getTag().getAsString().equals(iSign.diamondchestshop_getNbt()))) {
                    itemCount += item.getCount();
                    if (firstItemIndex == -1) firstItemIndex = i;
                    if (itemCount >= shopQuantity) break;
                }
            }
            if (itemCount < shopQuantity) {
                player.displayClientMessage(Component.literal("The shop is sold out"), true);
                return;
            }

            // take items from chest
            itemCount = shopQuantity;
            for (int i = firstItemIndex; i < inventory.getContainerSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item.getItem().equals(sellItem) && (!item.hasTag() || item.getTag().getAsString().equals(iSign.diamondchestshop_getNbt()))) {
                    int takeAmount = Math.min(item.getCount(), itemCount);
                    itemCount -= takeAmount;
                    inventory.removeItem(i, takeAmount);
                    if (itemCount == 0) break;
                }
            }
        }

        // give the player the items
        int itemCount = shopQuantity;
        while (itemCount > 0) {
            int itemOut = Math.min(itemCount, sellItem.getMaxStackSize());
            ItemStack stack = new ItemStack(sellItem, itemOut);
            if (!iSign.diamondchestshop_getNbt().equals("{}")) {
                var tag = DiamondChestShopUtil.getNbtData(iSign.diamondchestshop_getNbt());
                if (tag != null) stack.setTag(tag);
            }
            player.getInventory().placeItemBackInInventory(stack);
            itemCount -= itemOut;
        }

        // alter balance account
        dm.setBalance(player.getStringUUID(), playerBalance - shopPrice);
        if (!iSign.diamondchestshop_getIsAdminShop()) {
            // only give owner money if it is not an admin shop
            dm.setBalance(owner, dm.getBalanceFromUUID(owner) + shopPrice);
        }
        DiamondChestShop.getDatabaseManager().logTrade(
                iSign.diamondchestshop_getId(),
                shopQuantity,
                shopPrice,
                player.getStringUUID(),
                iSign.diamondchestshop_getIsAdminShop() ? "admin" : owner,
                "sell"
        );
        player.displayClientMessage(Component.literal("Bought " + shopQuantity + " " + sellItem.getDescription().getString() + " for $" + shopPrice), true);
    }

    @Unique
    private void buyShop(SignBlockEntity sign, BlockState state, BlockPos blockPos) {
        SignBlockEntityInterface iSign = (SignBlockEntityInterface) sign;
        // get container block of shop
        Direction oppSignDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        BlockPos hangingPos = blockPos.offset(oppSignDir.getStepX(), oppSignDir.getStepY(),  oppSignDir.getStepZ());
        BaseContainerBlockEntityInterface shop;
        // exit if this sign is not attached to a valid shop block
        if (level.getBlockEntity(hangingPos) instanceof RandomizableContainerBlockEntity containerBe)
            shop = (BaseContainerBlockEntityInterface) containerBe;
        else return;
        if (!shop.diamondchestshop_getIsShop()) return;
        // gather shop data
        int shopQuantity = iSign.diamondchestshop_getQuantity();
        int shopPrice = iSign.diamondchestshop_getPrice();
        String owner = iSign.diamondchestshop_getOwner();
        Item buyItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(iSign.diamondchestshop_getItem()));

        // gather balances from the database
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        int playerBalance = dm.getBalanceFromUUID(player.getStringUUID());
        int shopBalance = dm.getBalanceFromUUID(owner);

        // check if shop owner has sufficient funds/player can receive fund
        if (shopBalance < shopPrice && !iSign.diamondchestshop_getIsAdminShop()) {
            player.displayClientMessage(Component.literal("The owner hasn't got enough money"), true);
            return;
        }
        if (Integer.MAX_VALUE - playerBalance < shopPrice ) {
            player.displayClientMessage(Component.literal("You are too rich"), true);
            return;
        }

        // get shop block container
        Block shopBlock = level.getBlockState(hangingPos).getBlock();
        Container inventory;
        if (shop instanceof ChestBlockEntity && shopBlock instanceof ChestBlock) {
            Container container = ChestBlock.getContainer((ChestBlock) shopBlock, level.getBlockState(hangingPos), level, hangingPos, true);
            if (container != null) {
                inventory = container;
            } else {
                DiamondChestShop.LOGGER.error("Shop sign seems to be attached to a block that is not a container, but a shop was created.");
                return;
            }
        } else {
            inventory = (RandomizableContainerBlockEntity) shop;
        }

        // check if shop container has enough space
        int emptySpaces = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item.getItem().equals(Items.AIR)) {
                emptySpaces += buyItem.getMaxStackSize();
                continue;
            }
            if (item.getItem().equals(buyItem)) {
                emptySpaces += buyItem.getMaxStackSize() - item.getCount();
            }
        }
        if (emptySpaces < shopQuantity) {
            player.displayClientMessage(Component.literal("The chest is full"), true);
            return;
        }

        // check if player has item in proper quantity
        // TODO: use inventory.countItem()
        int itemCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item.getItem().equals(buyItem) && (!item.hasTag() || player.getInventory().getItem(i).getTag().getAsString().equals((iSign.diamondchestshop_getNbt())))) {
                itemCount += item.getCount();
            }
        }
        if (itemCount < shopQuantity) {
            player.displayClientMessage(Component.literal("You don't have enough of that item"), true);
            return;
        }

        // take items from player
        itemCount = shopQuantity;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item.getItem().equals(buyItem) && (!item.hasTag() || item.getTag().getAsString().equals(iSign.diamondchestshop_getNbt()))) {
                int takeAmount = Math.min(item.getCount(), itemCount);
                itemCount -= takeAmount;
                player.getInventory().removeItem(i, takeAmount);
                if (itemCount == 0) break;
            }
        }

        // give the chest the items (only if not admin shop)
        if (!iSign.diamondchestshop_getIsAdminShop()) {
            String nbtTag = iSign.diamondchestshop_getNbt().equals("{}") ? null : iSign.diamondchestshop_getNbt();
            int couldNotBeAdded = DiamondChestShopUtil.addToContainer(inventory, buyItem, shopQuantity, nbtTag);
            if (couldNotBeAdded != 0) {
                DiamondChestShop.LOGGER.error("Could not add " + couldNotBeAdded + " items to shop (" + iSign.diamondchestshop_getId() + ")");
            }
        }

        // update balance accounts
        if (!iSign.diamondchestshop_getIsAdminShop()) {
            dm.setBalance(owner, shopBalance - shopPrice);
        }
        dm.setBalance(player.getStringUUID(), playerBalance + shopPrice);
        DiamondChestShop.getDatabaseManager().logTrade(
                iSign.diamondchestshop_getId(),
                shopQuantity,
                shopPrice,
                iSign.diamondchestshop_getIsAdminShop() ? "admin" : owner,
                player.getStringUUID(),
                "buy"
        );
        player.displayClientMessage(Component.literal("Sold " + shopQuantity + " " + buyItem.getDescription().getString() + " for $" + shopPrice), true);

    }
}