package com.gmail.sneakdevs.diamondchestshop.util;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DiamondChestShopUtil {
    public static String signTextToReadable(String text) {
        return text.replaceAll("[\\D]", "").toLowerCase();
    }

    public static CompoundTag getNbtData(String text) {
        try {
            CompoundTag nbt = NbtUtils.snbtToStructure(text);
            if (!text.contains("palette")) nbt.remove("palette");
            return nbt;
        } catch (CommandSyntaxException ignore) {
            return null;
        }
    }

    public static int removeFromContainer(Container container, Item item, int count, String nbt) {
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (!stack.getItem().equals(item)) continue;
            if (nbt == null || !stack.hasTag() || stack.getTag().getAsString().equals(nbt)) {
                int takeAmount = Math.min(stack.getCount(), count);
                count -= takeAmount;
                container.removeItem(i, takeAmount);
                if (count == 0) break;
            }
        }
        return count;
    }

    public static int addToContainer(Container container, Item item, int count, String nbt) {
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            ItemStack newStack = null;
            if (stack.equals(ItemStack.EMPTY)) {
                // empty slot
                int itemsToAdd = Math.min(count, item.getMaxStackSize());
                newStack = new ItemStack(item, itemsToAdd);
                count -= itemsToAdd;
            } else if (stack.getItem().equals(item) && (nbt == null || !stack.hasTag() || stack.getTag().getAsString().equals(nbt))) {
                // same item is in this slot
                int itemsToAdd = Math.min(count, item.getMaxStackSize() - stack.getCount());
                newStack = new ItemStack(item, itemsToAdd + stack.getCount());
                count -= itemsToAdd;
            }
            // add item if there is space in this slot
            if (newStack != null) container.setItem(i, newStack);
            if (count == 0) break;
        }
        return count;
    }

    public static void diamondchestshop_destroyShop(Level world, BlockPos pos, BlockState state) {
        if (world.isClientSide()) return;
        var be = world.getBlockEntity(pos);
        if (be == null) return;
        if (be instanceof SignBlockEntityInterface iSign && iSign.diamondchestshop_getIsShop()) {
            DiamondChestShop.LOGGER.info("destoryShop");
            // destroying a sign
            // get shop entity
            Direction nonFacingDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            BlockPos hangingPos = pos.offset(nonFacingDir.getStepX(), nonFacingDir.getStepY(), nonFacingDir.getStepZ());
            // if sign belongs to a shop entity, remove that shop
            if (world.getBlockEntity(hangingPos) instanceof BaseContainerBlockEntity shop && ((BaseContainerBlockEntityInterface) shop).diamondchestshop_getIsShop()) {
                diamondchestshop_destroyShopContainer(shop, world, hangingPos);
            }
            // remove shop from database and SignEntity
            int shopId = iSign.diamondchestshop_getId();
            DiamondChestShop.getDatabaseManager().removeShop(shopId);
            iSign.diamondchestshop_removeShop();
        } else if (be instanceof BaseContainerBlockEntity shop && ((BaseContainerBlockEntityInterface)be).diamondchestshop_getIsShop()) {
            // destroying a container
            int shopId = ((BaseContainerBlockEntityInterface)be).diamondchestshop_getId();
            DiamondChestShop.getDatabaseManager().removeShop(shopId);
            diamondchestshop_destroyShopContainer(shop, world, pos);
        }
    }

    private static void diamondchestshop_destroyShopContainer(BaseContainerBlockEntity shop, Level world, BlockPos pos) {
        BaseContainerBlockEntityInterface iShop = (BaseContainerBlockEntityInterface) shop;
        iShop.diamondchestshop_removeShop();
        shop.setChanged();
        // check if this a double chest
        BlockState shopBlock = world.getBlockState(pos);
        if (shopBlock.getBlock().equals(Blocks.CHEST) && !ChestBlock.getBlockType(shopBlock).equals(DoubleBlockCombiner.BlockType.SINGLE)) {
            Direction dir = ChestBlock.getConnectedDirection(shopBlock);
            BlockEntity be2 = world.getBlockEntity(new BlockPos(shop.getBlockPos().getX() + dir.getStepX(), shop.getBlockPos().getY(), shop.getBlockPos().getZ() + dir.getStepZ()));
            if (be2 != null) {
                ((BaseContainerBlockEntityInterface) be2).diamondchestshop_removeShop();
                be2.setChanged();
            }
        }
    }
}
