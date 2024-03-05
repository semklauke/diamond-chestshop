package com.gmail.sneakdevs.diamondchestshop.util;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.network.chat.Style;
public class DiamondChestShopUtil {
    public static Style ERROR_STYLE = Style.EMPTY.withColor(TextColor.parseColor("#D23006")).withBold(true);
    public static Style SUCCESS_STYLE = Style.EMPTY;
    public static Style INFO_STYLE = Style.EMPTY;
    public static String signTextToReadable(String text) {
        return text.replaceAll("[\\D]", "").toLowerCase();
    }

    public static void sendHotbarMessage(Player player, Component msg) {
        player.displayClientMessage(
                    DiamondChestShopConfig.HotbarPrefix().append(msg)
        , true);
    }

    public static void sendHotbarMessage(Player player, String msg, Style style) {
        if (style == null) style = Style.EMPTY;
        sendHotbarMessage(player, Component.literal(msg).withStyle(style));
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

    public static BlockEntity getDoubleChest(Level world, BlockPos chestPos) {
        BlockState chestBlock = world.getBlockState(chestPos);
        if (chestBlock.getBlock().equals(Blocks.CHEST) && !ChestBlock.getBlockType(chestBlock).equals(DoubleBlockCombiner.BlockType.SINGLE)) {
            // this is a double chest
            BlockPos doubleChestPos = chestPos.relative(ChestBlock.getConnectedDirection(chestBlock));
            BlockEntity be = world.getBlockEntity(doubleChestPos);
            if (be instanceof BaseContainerBlockEntityInterface) return be;
        }
        return null;
    }

    public static void destroyShopContainer(BaseContainerBlockEntity shop, Level world, BlockPos pos) {
        BaseContainerBlockEntityInterface iShop = (BaseContainerBlockEntityInterface) shop;
        iShop.diamondchestshop_removeShop();
        shop.setChanged();
        // check if this a double chest
        BlockEntity dc = DiamondChestShopUtil.getDoubleChest(world, pos);
        if (dc != null) {
            ((BaseContainerBlockEntityInterface) dc).diamondchestshop_removeShop();
            dc.setChanged();
        }
    }

    public static String padRight(String s, int n) {
        if (s.length() > n) return s.substring(0, n);
        else return String.format("%-" + n + "s", s);
    }

    public static String getRightPadding(String s, int n) {
        if (s.length() > n) return null;
        else return "+".repeat(n - s.length());
    }

    public static String padLeft(String s, int n) {
        if (s.length() > n) return s.substring(0, n);
        else return String.format("%" + n + "s", s);
    }

}
