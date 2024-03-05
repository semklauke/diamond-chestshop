package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = SignBlock.class, priority = 999)
public abstract class SignBlockMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void diamondchestshop_useMixin(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        // only server side
        if (world.isClientSide()) return;

        // get Item and Sign entity
        Item handItem = player.getItemInHand(hand).getItem();
        SignBlockEntity signEntity = (SignBlockEntity) world.getBlockEntity(pos);
        if (signEntity == null) return;
        SignBlockEntityInterface iSign = (SignBlockEntityInterface) signEntity;

        // create admin shop if hand item is command block
        if (iSign.diamondchestshop_getIsShop() && handItem.equals(Items.COMMAND_BLOCK)) {
            iSign.diamondchestshop_setIsAdminShop(!iSign.diamondchestshop_getIsAdminShop());
            signEntity.setChanged();
            String msg = iSign.diamondchestshop_getIsAdminShop() ? "Created admin shop" : "Removed admin shop";
            DiamondChestShopUtil.sendHotbarMessage(player, msg, DiamondChestShopUtil.SUCCESS_STYLE);
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        // hand item must be a currency item (first in the list)
        if (!handItem.equals(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(DiamondEconomyConfig.getInstance().currencies[0])))) {
            return;
        }

        /* From here on this is a shop interaction with the sign.
         * Hence, we always want to cancel the tail of the sign use method */

        cir.setReturnValue(InteractionResult.PASS);
        // if this shop already exists, exit
        if (iSign.diamondchestshop_getIsShop()) {
            DiamondChestShopUtil.sendHotbarMessage(player, "This is already a shop!", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }

        // get container entity
        Direction oppositeBlockDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        BlockPos hangingPos = pos.offset(oppositeBlockDir.getStepX(), oppositeBlockDir.getStepY(), oppositeBlockDir.getStepZ());
        BlockEntity chestEntity = world.getBlockEntity(hangingPos);
        if (!(chestEntity instanceof RandomizableContainerBlockEntity shop)) {
            // sign is not attached to a container
            DiamondChestShopUtil.sendHotbarMessage(player, "Sign must be on a valid container!", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }

        BaseContainerBlockEntityInterface iShop = ((BaseContainerBlockEntityInterface) shop);
        // TODO: check here if the shop is really a shop. Currently not done since the variables on the chest aren't getting set correctly

        // sign and chest must be placed down by the current player
        // TODO: we skip the chest check here, since it is not a shop chest before the shop is created (|| (!iShop.diamondchestshop_getOwner().equals(player.getStringUUID()))
        if (!iSign.diamondchestshop_getOwner().equals(player.getStringUUID())) {
            player.displayClientMessage(Component.literal("You can only activated shops when you place down the sign and chest yourself"), true);
            return;
        }

        // offhand must have an item
        if (player.getOffhandItem().isEmpty()) {
            DiamondChestShopUtil.sendHotbarMessage(player, "Put the item you want to sell/buy in your offhand", DiamondChestShopUtil.INFO_STYLE);
            return;
        }

        // don't create a shop with the same chest twice
        if (iShop.diamondchestshop_getId() != -1) {
            DiamondChestShopUtil.sendHotbarMessage(player, "That chest already is a shop!", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }

        // Parse first line, exit if not sell or buy keyword
        if (iSign.diamondchestshop_getShopType() == SignBlockEntityInterface.ShopType.NONE) {
            MutableComponent msg = Component.empty().withStyle(DiamondChestShopUtil.ERROR_STYLE)
                    .append("The first line must be either ")
                    .append(Component.literal('"' + DiamondChestShopConfig.getInstance().buyKeyword + '"')
                            .withStyle(Style.EMPTY.withBold(true)))
                    .append(" or ")
                    .append(Component.literal('"' + DiamondChestShopConfig.getInstance().sellKeyword + '"')
                            .withStyle(Style.EMPTY.withBold(true)));
            DiamondChestShopUtil.sendHotbarMessage(player, msg);
            return;
        }

        // parse line 2,3 (quantity, money) to integers
        int quantity = iSign.diamondchestshop_getQuantity();
        int price = iSign.diamondchestshop_getPrice();
        // quantity must be at least 1
        if (quantity < 1) {
            DiamondChestShopUtil.sendHotbarMessage(player, "Line 2 (quantity) must be a positive number!", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }
        // money must be at least 0
        if (price < 0) {
            DiamondChestShopUtil.sendHotbarMessage(player, "Line 3 (price) must be a non negative number!", DiamondChestShopUtil.ERROR_STYLE);
            return;
        }

        // save nbt tags for the item in the offhand
        String nbt;
        try { nbt = Objects.requireNonNull(player.getOffhandItem().getTag()).getAsString(); }
        catch (NullPointerException ignore) { nbt = "{}"; }

        // start creating shop
        String itemStr = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
        String owner = player.getStringUUID();
        // add shop to database
        int shopId = DiamondChestShop.getDatabaseManager().addShop(owner, itemStr, nbt, pos.getCenter());
        // add data to sign and container entity
        iSign.diamondchestshop_setShop(shopId, owner, itemStr, nbt, false);
        iShop.diamondchestshop_setId(shopId);
        iShop.diamondchestshop_setOwner(owner);

        // saves sing entity
        signEntity.setWaxed(true);
        signEntity.setChanged();
        shop.setChanged();

        // if the sign is attached to a double chest, add data to other chest as well
        BlockEntity dc = DiamondChestShopUtil.getDoubleChest(world, hangingPos);
        if (dc != null) {
            ((BaseContainerBlockEntityInterface) dc).diamondchestshop_setId(shopId);
            ((BaseContainerBlockEntityInterface) dc).diamondchestshop_setOwner(owner);

        }

        // send success message
        MutableComponent msg = DiamondChestShopConfig.ChatPrefix().withStyle(DiamondChestShopUtil.SUCCESS_STYLE)
                .append("Created shop #" + shopId + " that ")
                .append(Component.literal(iSign.diamondchestshop_getShopType() == SignBlockEntityInterface.ShopType.SELL ? "sells " : "buys ")
                        .withStyle(Style.EMPTY.withBold(true)))
                .append(quantity + "x ")
                .append(Component.translatable(player.getOffhandItem().getItem().getDescriptionId()).getString())
                .append(" for ")
                .append(DiamondChestShopConfig.currencyToLiteral(price));

        player.displayClientMessage(msg, false);
    }
}