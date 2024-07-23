package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondeconomy.DiamondEconomy;
import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface.ShopType;

import java.util.ArrayList;

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
                if (shopType == ShopType.NONE) {
                    // early return if this not a shop
                    return;
                } else if (shopType == ShopType.SELL) {
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
            if (!iSign.diamondchestshop_getOwner().equals(player.getStringUUID()) || iSign.diamondchestshop_getIsAdminShop()) {
                BlockState state = level.getBlockState(blockPos);
                var shopType = iSign.diamondchestshop_getShopType();
                if (shopType == ShopType.BUY) {
                    buyShop((SignBlockEntity) iSign, state, blockPos);
                    cir.setReturnValue(0.0F);
                }
                if (shopType == ShopType.SELL) {
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
        BlockPos hangingPos = blockPos.offset(oppSignDir.getStepX(), oppSignDir.getStepY(), oppSignDir.getStepZ());
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
            DiamondChestShopUtil.sendHotbarMessage(player, "You don't have enough money!", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }
        if (Integer.MAX_VALUE - shopBalance < shopPrice && !iSign.diamondchestshop_getIsAdminShop()) {
            DiamondChestShopUtil.sendHotbarMessage(player, "The owner is too rich!", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }

        // Save items that will be added to the player inv here
        ArrayList<ItemStack> addToPlayerInv = new ArrayList<ItemStack>();

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

            // first check if shop has item in proper quantity
            if (inventory.countItem(sellItem) < shopQuantity) {
                DiamondChestShopUtil.sendHotbarMessage(player, "The shop is sold out", DiamondChestShopUtil.ERROR_STYLE);
                return;
            }
            // take items from chest
            int itemCount = shopQuantity;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item.getItem().equals(sellItem)) {
                    int takeAmount = Math.min(item.getCount(), itemCount);
                    addToPlayerInv.add(item.copyWithCount(takeAmount)); // add to player inv later
                    inventory.removeItem(i, takeAmount); // remove from container
                    itemCount -= takeAmount;
                    if (itemCount <= 0) break;
                }
            }
        } else {
            // admin shop - create item from thin air
            int itemCount = shopQuantity;
            while (itemCount >= 0) {
                int takeAmount = Math.min(itemCount, sellItem.getDefaultMaxStackSize());
                addToPlayerInv.add(new ItemStack(sellItem, takeAmount));
                itemCount -= takeAmount;
            }
        }

        // give the player the items
        for (ItemStack stack : addToPlayerInv) {
            player.getInventory().placeItemBackInInventory(stack);
        }

        // alter balance account
        dm.setBalance(player.getStringUUID(), playerBalance - shopPrice);
        if (!iSign.diamondchestshop_getIsAdminShop()) {
            // only give owner money if it is not an admin shop
            dm.setBalance(owner, dm.getBalanceFromUUID(owner) + shopPrice);
        }
        // log trade
        DiamondChestShop.getDatabaseManager().logTrade(
                iSign.diamondchestshop_getId(),
                shopQuantity,
                shopPrice,
                player.getStringUUID(),
                iSign.diamondchestshop_getIsAdminShop() ? "admin" : owner,
                "sell"
        );
        // get prev trade info (last 10 sec) to combine chat messages
        Tuple<Integer, Integer> prevTradesInfo = DiamondChestShop.getDatabaseManager().similarTrades(
                player.getStringUUID(),
                iSign.diamondchestshop_getId(),
                ShopType.SELL
        );
        if (prevTradesInfo != null) {
            shopQuantity = prevTradesInfo.getA();
            shopPrice = prevTradesInfo.getB();
        }
        // construct and send success message to player
        MutableComponent successMsg =
                Component.empty().withStyle(DiamondChestShopUtil.SUCCESS_STYLE)
                        .append("Bought ")
                        .append(Component.literal(shopQuantity + "x ").withStyle(Style.EMPTY.withBold(true)))
                        .append(sellItem.getDescription().getString() + " for ")
                        .append(DiamondChestShopConfig.currencyToLiteral(shopPrice));
        String ownerName = DiamondUtils.getDatabaseManager().getNameFromUUID(owner);
        if (ownerName != null)
                successMsg.append(" from " + ownerName);
        DiamondChestShopUtil.sendHotbarMessage(player, successMsg);
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
            DiamondChestShopUtil.sendHotbarMessage(player, "The owner hasn't got enough money", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }
        if (Integer.MAX_VALUE - playerBalance < shopPrice ) {
            DiamondChestShopUtil.sendHotbarMessage(player, "You are to rich!", DiamondChestShopUtil.ERROR_STYLE);
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
                emptySpaces += buyItem.getDefaultMaxStackSize();
                continue;
            }
            if (item.getItem().equals(buyItem)) {
                emptySpaces += buyItem.getDefaultMaxStackSize() - item.getCount();
            }
        }
        if (emptySpaces < shopQuantity) {
            player.displayClientMessage(Component.literal("The chest is full"), true);
            return;
        }

        // check if player has item in proper quantity
        if (player.getInventory().countItem(buyItem) < shopQuantity) {
            DiamondChestShopUtil.sendHotbarMessage(player, "You don't have enough of that item", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }

        // take items from player
        ArrayList<ItemStack> addToShop = new ArrayList<ItemStack>();
        int itemCount = shopQuantity;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item.getItem().equals(buyItem)) {
                int takeAmount = Math.min(item.getCount(), itemCount);
                addToShop.add(item.copyWithCount(takeAmount));
                player.getInventory().removeItem(i, takeAmount);
                itemCount -= takeAmount;
                if (itemCount <= 0) break;
            }
        }


        // give the chest the items (only if not admin shop)
        if (!iSign.diamondchestshop_getIsAdminShop()) {
            for (ItemStack shopStack : addToShop) {
                DiamondChestShop.LOGGER.info(addToShop.toString());
                if (DiamondChestShopUtil.addToContainer(inventory, shopStack) != 0) {
                    // could not add the whole stack
                    DiamondChestShop.LOGGER.error("Could not add {} items to shop ({})", shopStack.getCount(), iSign.diamondchestshop_getId());
                    //player.getInventory().placeItemBackInInventory(shopStack); // give the remaining stack back to the player
                }
            }
        }

        // update balance accounts
        if (!iSign.diamondchestshop_getIsAdminShop()) {
            dm.setBalance(owner, shopBalance - shopPrice);
        }
        dm.setBalance(player.getStringUUID(), playerBalance + shopPrice);
        // log trade
        DiamondChestShop.getDatabaseManager().logTrade(
                iSign.diamondchestshop_getId(),
                shopQuantity,
                shopPrice,
                iSign.diamondchestshop_getIsAdminShop() ? "admin" : owner,
                player.getStringUUID(),
                "buy"
        );
        // get prev trade info (last 10 sec) to combine chat messages
        Tuple<Integer, Integer> prevTradesInfo = DiamondChestShop.getDatabaseManager().similarTrades(
                player.getStringUUID(),
                iSign.diamondchestshop_getId(),
                ShopType.BUY
        );
        if (prevTradesInfo != null) {
            shopQuantity = prevTradesInfo.getA();
            shopPrice = prevTradesInfo.getB();
        }
        // construct and send success message to player
        MutableComponent successMsg =
                Component.empty().withStyle(DiamondChestShopUtil.SUCCESS_STYLE)
                        .append("Sold ")
                        .append(Component.literal(shopQuantity + "x ").withStyle(Style.EMPTY.withBold(true)))
                        .append(buyItem.getDescription().getString() + " for ")
                        .append(DiamondChestShopConfig.currencyToLiteral(shopPrice));
        String ownerName = DiamondUtils.getDatabaseManager().getNameFromUUID(owner);
        if (ownerName != null)
                successMsg.append(" to " + ownerName);
        DiamondChestShopUtil.sendHotbarMessage(player, successMsg);
    }
}