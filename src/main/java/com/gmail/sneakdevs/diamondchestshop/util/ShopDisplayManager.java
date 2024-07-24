package com.gmail.sneakdevs.diamondchestshop.util;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondeconomy.DiamondEconomy;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import  net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShopDisplayManager {
    private static List<SignBlockEntityInterface> initQueue = new ArrayList<>();
    public ShopDisplayManager(boolean init) {
        if (init) init();
    }

    public void init() {
        DiamondChestShop.LOGGER.info("init item displays with " + initQueue.size() + " shops");
        for (SignBlockEntityInterface iSign : initQueue) {
            iSign.diamondchestshop_createItemDisplay();
        }
        initQueue = new ArrayList<>();
    }

    public static void registerItemDisplayCreation(SignBlockEntityInterface iSign) {
        initQueue.add(iSign);
    }

    private static class RotatingItemDisplay extends ItemDisplayElement {
        public RotatingItemDisplay(Item item) {
            super(item);
        }

        @Override
        public void tick() {
            this.setYaw(this.getYaw() + 3 % 360);
            super.tick();
        }
    }

    public static ElementHolder getItemDisplay(Item item) {
        // create ElementHolder and ItemDisplayElement
        var holder = new ElementHolder();
        // item display

        ItemDisplayElement display = DiamondChestShopConfig.getInstance().shopDisplayRotate ? new RotatingItemDisplay(item) : new ItemDisplayElement(item);
        display.setScale(new Vector3f(0.4f, 0.4f, 0.4f));
        display.setNoGravity(true);
        //display.setInitialPosition(new Vec3d(20, 70, 20));
        display.setTranslation(new Vector3f(0, 0.8f, 0));
        display.ignorePositionUpdates();
        //display.setBillboardMode(BillboardConstraints.FIXED);
        display.setViewRange(0.21f);
        display.setInterpolationDuration(5);
        display.setTeleportDuration(5);
        display.setLeftRotation(new Quaternionf().rotateY(Mth.PI * (float) Math.random() * 2)); // random offset
        holder.addElement(display);
        // text display
        TextDisplayElement text = new TextDisplayElement();
        text.setText(item.getDescription());
        text.setTranslation(new Vector3f(0, 1.07f, 0));
        text.setBillboardMode(BillboardConstraints.VERTICAL);
        text.setTextOpacity((byte)190);
        text.setViewRange(0.07f);
        text.setScale(new Vector3f(0.7f, 0.7f, 0.7f));
        holder.addElement(text);
        return holder;
    }
    public static HolderAttachment attachItemDisplay(String itemString, ServerLevel world, Vec3 pos) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemString));
        return ShopDisplayManager.attachItemDisplay(item, world, pos);
    }
    public static HolderAttachment attachItemDisplay(Item item, ServerLevel world, Vec3 pos) {
        return ChunkAttachment.ofTicking(ShopDisplayManager.getItemDisplay(item), world, pos);
    }
}
