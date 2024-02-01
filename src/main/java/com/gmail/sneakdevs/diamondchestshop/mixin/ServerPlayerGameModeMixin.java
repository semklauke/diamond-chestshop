package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.item.ItemEntity;
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

import java.text.SimpleDateFormat;
import java.util.Date;

@Mixin(value = ServerPlayerGameMode.class, priority = 999)
public class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Final
    @Shadow
    protected ServerPlayer player;

    @Shadow
    private int gameTicks;

    @Unique
    private SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

    @Inject(method = "destroyAndAck", at = @At("HEAD"), cancellable = true)
    private void diamondchestshop_destroyAndAckMixin(BlockPos blockPos, int i, String string, CallbackInfo ci) {
        if (DiamondChestShopConfig.getInstance().shopProtectPlayerBreak) {
            if (player.isCreative()) return;
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be != null) {
                if (be instanceof BaseContainerBlockEntity) {
                    if (!((BaseContainerBlockEntityInterface) be).diamondchestshop_getShop()) return;
                    if (!((BaseContainerBlockEntityInterface) be).diamondchestshop_getOwner().equals(player.getStringUUID())) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(level, blockPos));
                        BlockEntity blockEntity = level.getBlockEntity(blockPos);
                        Packet<ClientGamePacketListener> updatePacket = blockEntity.getUpdatePacket();
                        if (updatePacket != null) {
                            this.player.connection.send(updatePacket);
                        }
                        ci.cancel();
                    }
                }
                if (be instanceof SignBlockEntity) {
                    if (!((SignBlockEntityInterface) be).diamondchestshop_getShop()) return;
                    if (!((SignBlockEntityInterface) be).diamondchestshop_getOwner().equals(player.getStringUUID())) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(level, blockPos));
                        BlockEntity blockEntity = level.getBlockEntity(blockPos);
                        Packet<ClientGamePacketListener> updatePacket = blockEntity.getUpdatePacket();
                        if (updatePacket != null) {
                            this.player.connection.send(updatePacket);
                        }
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "useItemOn", cancellable = true)
    private void diamondchestshop_useItemMixin(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockEntity be = level.getBlockEntity(blockHitResult.getBlockPos());
        if (be instanceof SignBlockEntity) {
            if (((SignBlockEntityInterface)be).diamondchestshop_getShop() && !itemStack.getItem().equals(Items.COMMAND_BLOCK)) {
                if (((SignBlockEntity)be).getFrontText().getMessage(0,true).getString().toLowerCase().contains("sell")) {
                    sellShop((SignBlockEntity) be, level.getBlockState(blockHitResult.getBlockPos()), blockHitResult.getBlockPos());
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
                if (((SignBlockEntity)be).getFrontText().getMessage(0,true).getString().toLowerCase().contains("buy")) {
                    buyShop((SignBlockEntity) be, level.getBlockState(blockHitResult.getBlockPos()), blockHitResult.getBlockPos());
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
            }
        }
    }

    @Inject(method = "incrementDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void diamondchestshop_incrementDestroyProgressMixin(BlockState blockState, BlockPos blockPos, int j, CallbackInfoReturnable<Float> cir) {
        if (j + 1 == gameTicks) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof SignBlockEntity) {
                if (((SignBlockEntityInterface)be).diamondchestshop_getShop()) {
                    if (!((SignBlockEntityInterface) be).diamondchestshop_getOwner().equals(player.getStringUUID()) || ((SignBlockEntityInterface) be).diamondchestshop_getAdminShop()) {
                        BlockState state = level.getBlockState(blockPos);
                        if (((SignBlockEntity)be).getFrontText().getMessage(0,true).getString().toLowerCase().contains("buy")) {
                            buyShop((SignBlockEntity) be, state, blockPos);
                            cir.setReturnValue(0.0F);
                        }
                        if (((SignBlockEntity)be).getFrontText().getMessage(0,true).getString().toLowerCase().contains("sell")) {
                            sellShop((SignBlockEntity) be, state, blockPos);
                            cir.setReturnValue(0.0F);
                        }
                    }
                }
            }
        }
    }

    private void sellShop(SignBlockEntity be, BlockState state, BlockPos blockPos) {
        try {
            int quantity = Integer.parseInt(DiamondChestShop.signTextToReadable(be.getFrontText().getMessage(1, true).getString()));
            int quantity1 = quantity;
            int money = Integer.parseInt(DiamondChestShop.signTextToReadable(be.getFrontText().getMessage(2, true).getString()));
            DatabaseManager dm = DiamondUtils.getDatabaseManager();
            BlockPos hangingPos = blockPos.offset(state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepX(), state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepY(), state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepZ());
            RandomizableContainerBlockEntity shop = (RandomizableContainerBlockEntity) level.getBlockEntity(hangingPos);
            assert shop != null;
            String owner = ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getOwner();
            Item sellItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getItem()));

            if (dm.getBalanceFromUUID(player.getStringUUID()) < money) {
                player.displayClientMessage(Component.literal("You don't have enough money"), true);
                return;
            }
            if (dm.getBalanceFromUUID(owner) + money >= Integer.MAX_VALUE && !((SignBlockEntityInterface) be).diamondchestshop_getAdminShop()) {
                player.displayClientMessage(Component.literal("The owner is too rich"), true);
                return;
            }

            //check shop has item in proper quantity
            if (!((SignBlockEntityInterface) be).diamondchestshop_getAdminShop()) {
                Block shopBlock = level.getBlockState(hangingPos).getBlock();
                Container inventory;
                if (shop instanceof ChestBlockEntity && shopBlock instanceof ChestBlock) {
                    inventory = ChestBlock.getContainer((ChestBlock) shopBlock, level.getBlockState(hangingPos), level, hangingPos, true);
                } else {
                    inventory = shop;
                }

                int itemCount = 0;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (inventory.getItem(i).getItem().equals(sellItem) && (!inventory.getItem(i).hasTag() || inventory.getItem(i).getTag().getAsString().equals(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()))) {
                        itemCount += inventory.getItem(i).getCount();
                    }
                }
                if (itemCount < quantity) {
                    player.displayClientMessage(Component.literal("The shop is sold out"), true);
                    return;
                }

                //take items from chest
                itemCount = quantity;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (inventory.getItem(i).getItem().equals(sellItem) && (!inventory.getItem(i).hasTag() || inventory.getItem(i).getTag().getAsString().equals(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()))) {
                        itemCount -= inventory.getItem(i).getCount();
                        inventory.setItem(i, new ItemStack(Items.AIR));
                        if (itemCount < 0) {
                            ItemStack stack = new ItemStack(sellItem, Math.abs(itemCount));
                            stack.setTag(DiamondChestShop.getNbtData(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()));
                            inventory.setItem(i, stack);
                            break;
                        }
                    }
                }
            }

            //give the player the items
            while (quantity > sellItem.getMaxStackSize()) {
                ItemStack stack = new ItemStack(sellItem, sellItem.getMaxStackSize());
                stack.setTag(DiamondChestShop.getNbtData(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()));
                ItemEntity itemEntity = player.drop(stack, false);
                assert itemEntity != null;
                itemEntity.setNoPickUpDelay();
                quantity -= sellItem.getMaxStackSize();
            }

            ItemStack stack2 = new ItemStack(sellItem, quantity);
            if (!((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt().equals("{}")) {
                stack2.setTag(DiamondChestShop.getNbtData(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()));
            }
            ItemEntity itemEntity2 = player.drop(stack2, true);
            assert itemEntity2 != null;
            itemEntity2.setNoPickUpDelay();

            dm.setBalance(player.getStringUUID(), dm.getBalanceFromUUID(player.getStringUUID()) - money);
            if (!((SignBlockEntityInterface) be).diamondchestshop_getAdminShop()) {
                dm.setBalance(owner, dm.getBalanceFromUUID(owner) + money);
            }

            DiamondChestShop.getDatabaseManager().logTrade(
                    ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getId(),
                    quantity1,
                    money,
                    player.getStringUUID(),
                    ((SignBlockEntityInterface) be).diamondchestshop_getAdminShop() ? "admin" : owner,
                    "sell"
            );
            player.displayClientMessage(Component.literal("Bought " + quantity1 + " " + sellItem.getDescription().getString() + " for $" + money), true);
        } catch (NumberFormatException | CommandSyntaxException | NullPointerException ignored) {}
    }

    private void buyShop(SignBlockEntity be, BlockState state, BlockPos blockPos) {
        try {
            int quantity = Integer.parseInt(DiamondChestShop.signTextToReadable(be.getFrontText().getMessage(1, true).getString()));
            int money = Integer.parseInt(DiamondChestShop.signTextToReadable(be.getFrontText().getMessage(2, true).getString()));
            DatabaseManager dm = DiamondUtils.getDatabaseManager();
            BlockPos hangingPos = blockPos.offset(state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepX(), state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepY(), state.getValue(HorizontalDirectionalBlock.FACING).getOpposite().getStepZ());
            RandomizableContainerBlockEntity shop = (RandomizableContainerBlockEntity) level.getBlockEntity(hangingPos);
            assert shop != null;
            String owner = ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getOwner();
            Item buyItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getItem()));

            if (dm.getBalanceFromUUID(owner) < money && !((SignBlockEntityInterface) be).diamondchestshop_getAdminShop()) {
                player.displayClientMessage(Component.literal("The owner hasn't got enough money"), true);
                return;
            }

            if (dm.getBalanceFromUUID(player.getStringUUID()) + money >= Integer.MAX_VALUE) {
                player.displayClientMessage(Component.literal("You are too rich"), true);
                return;
            }

            //check player has item in proper quantity
            int itemCount = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).getItem().equals(buyItem) && (!player.getInventory().getItem(i).hasTag() || player.getInventory().getItem(i).getTag().getAsString().equals(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()))) {
                    itemCount += player.getInventory().getItem(i).getCount();
                }
            }
            if (itemCount < quantity) {
                player.displayClientMessage(Component.literal("You don't have enough of that item"), true);
                return;
            }
            int emptySpaces = 0;
            Block shopBlock = level.getBlockState(hangingPos).getBlock();
            Container inventory;
            if (shop instanceof ChestBlockEntity && shopBlock instanceof ChestBlock) {
                inventory = ChestBlock.getContainer((ChestBlock) shopBlock, level.getBlockState(hangingPos), level, hangingPos, true);
            } else {
                inventory = shop;
            }
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).getItem().equals(Items.AIR)) {
                    emptySpaces += buyItem.getMaxStackSize();
                    continue;
                }
                if (inventory.getItem(i).getItem().equals(buyItem)) {
                    emptySpaces += buyItem.getMaxStackSize() - inventory.getItem(i).getCount();
                }
            }
            if (emptySpaces < quantity) {
                player.displayClientMessage(Component.literal("The chest is full"), true);
                return;
            }

            //take items from player
            itemCount = quantity;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).getItem().equals(buyItem) && (!player.getInventory().getItem(i).hasTag() || player.getInventory().getItem(i).getTag().getAsString().equals(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()))) {
                    itemCount -= player.getInventory().getItem(i).getCount();
                    player.getInventory().setItem(i, new ItemStack(Items.AIR));
                    if (itemCount < 0) {
                        ItemStack stack = new ItemStack(buyItem, Math.abs(itemCount));
                        stack.setTag(DiamondChestShop.getNbtData(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()));
                        player.getInventory().setItem(i, stack);
                        break;
                    }
                }
            }

            //give the chest the items
            if (!((SignBlockEntityInterface) be).diamondchestshop_getAdminShop()) {
                int itemsToAdd = quantity;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (inventory.getItem(i).getItem().equals(buyItem) && (!inventory.getItem(i).hasTag() || inventory.getItem(i).getTag().getAsString().equals(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()))) {
                        itemsToAdd += inventory.getItem(i).getCount();
                        itemsToAdd -= buyItem.getMaxStackSize();
                        ItemStack stack = new ItemStack(buyItem, buyItem.getMaxStackSize());
                        stack.setTag(DiamondChestShop.getNbtData(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()));
                        inventory.setItem(i, stack);
                    }
                    if (inventory.getItem(i).getItem().equals(Items.AIR)) {
                        itemsToAdd -= buyItem.getMaxStackSize();
                        ItemStack stack = new ItemStack(buyItem, buyItem.getMaxStackSize());
                        stack.setTag(DiamondChestShop.getNbtData(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()));
                        inventory.setItem(i, stack);
                    }
                    if (itemsToAdd < 0) {
                        ItemStack stack = new ItemStack(buyItem, buyItem.getMaxStackSize() + itemsToAdd);
                        stack.setTag(DiamondChestShop.getNbtData(((BaseContainerBlockEntityInterface) shop).diamondchestshop_getNbt()));
                        inventory.setItem(i, stack);
                        break;
                    }
                }
            }

            if (!((SignBlockEntityInterface) be).diamondchestshop_getAdminShop()) {
                dm.setBalance(owner, dm.getBalanceFromUUID(owner) - money);
            }
            dm.setBalance(player.getStringUUID(), dm.getBalanceFromUUID(player.getStringUUID()) + money);
            DiamondChestShop.getDatabaseManager().logTrade(
                    ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getId(),
                    quantity,
                    money,
                    ((SignBlockEntityInterface) be).diamondchestshop_getAdminShop() ? "admin" : owner,
                    player.getStringUUID(),
                    "buy"
            );
            player.displayClientMessage(Component.literal("Sold " + quantity + " " + buyItem.getDescription().getString() + " for $" + money), true);
        } catch (NumberFormatException | CommandSyntaxException | NullPointerException ignored) {}
    }
}