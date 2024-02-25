package com.gmail.sneakdevs.diamondchestshop.mixin;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.DiamondChestShopUtil;
import com.gmail.sneakdevs.diamondchestshop.interfaces.BaseContainerBlockEntityInterface;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = SignBlock.class, priority = 999)
public abstract class SignBlockMixin extends BaseEntityBlock {
    
    protected SignBlockMixin(Properties properties) {
        super(properties);
    }

    //remove shop from chest
    @Override
    public void playerWillDestroy(Level world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        DiamondChestShopUtil.diamondchestshop_destroyShop(world, pos, state);
        super.playerWillDestroy(world, pos, state, player);
    }

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
            player.displayClientMessage(Component.literal((iSign.diamondchestshop_getIsAdminShop()) ? "Created admin shop" : "Removed admin shop"), true);
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
            player.displayClientMessage(Component.literal("This is already a shop"), true);
            return;
        }

        // get container entity
        Direction oppositeBlockDir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        BlockPos hangingPos = pos.offset(oppositeBlockDir.getStepX(), oppositeBlockDir.getStepY(), oppositeBlockDir.getStepZ());
        BlockEntity chestEntity = world.getBlockEntity(hangingPos);
        if (!(chestEntity instanceof RandomizableContainerBlockEntity shop)) {
            // sign is not attached to a container
            player.displayClientMessage(Component.literal("Sign must be on a valid container"), true);
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
            player.displayClientMessage(Component.literal("Put the item you want to sell/buy in your offhand"), true);
            return;
        }

        // don't create a shop with the same chest twice
        if (iShop.diamondchestshop_getId() != -1) {
            player.displayClientMessage(Component.literal("That chest already is a shop"), true);
            return;
        }

        // Parse first line, exit if not sell or buy keyword
        if (iSign.diamondchestshop_getShopType() == SignBlockEntityInterface.ShopType.NONE) {
            // TODO use keywords from config
            player.displayClientMessage(Component.literal("The first line must be either \"Buy\" or \"Sell\""), true);
            return;
        }

        // parse line 2,3 (quantity, money) to integers
        int quantity = iSign.diamondchestshop_getQuantity();
        int price = iSign.diamondchestshop_getPrice();
        // quantity must be at least 1
        if (quantity < 1) {
            player.displayClientMessage(Component.literal("Line 2 (quantity) must be a positive number!"), true);
            return;
        }
        // money must be at least 0
        if (price < 0) {
            player.displayClientMessage(Component.literal("Line 3 (price) must be a non negative number!"), true);
            return;
        }

        // save nbt tags for the item in the offhand
        String nbt;
        try { nbt = Objects.requireNonNull(player.getOffhandItem().getTag()).getAsString(); }
        catch (NullPointerException ignore) { nbt = "{}"; }

        // start creating shop
        String itemStr = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
        String owner = player.getStringUUID();
        int shopId = DiamondChestShop.getDatabaseManager().addShop(itemStr, nbt, hangingPos.getCenter());
        iSign.diamondchestshop_setShop(shopId, owner, itemStr, nbt, false);
        iShop.diamondchestshop_setId(shopId);
        iShop.diamondchestshop_setOwner(owner);

        // saves sing entity
        signEntity.setWaxed(true);
        signEntity.setChanged();
        shop.setChanged();

        // if the sign is attached to a double chest, add data to other chest as well
        BlockState shopBlock = world.getBlockState(hangingPos);
        if (shopBlock.getBlock().equals(Blocks.CHEST) && !ChestBlock.getBlockType(shopBlock).equals(DoubleBlockCombiner.BlockType.SINGLE)) {
            Direction dir = ChestBlock.getConnectedDirection(shopBlock);
            BlockEntity be2 = world.getBlockEntity(new BlockPos(shop.getBlockPos().getX() + dir.getStepX(), shop.getBlockPos().getY(), shop.getBlockPos().getZ() + dir.getStepZ()));
            if (be2 != null) {
                ((BaseContainerBlockEntityInterface) be2).diamondchestshop_setId(shopId);
                ((BaseContainerBlockEntityInterface) be2).diamondchestshop_setOwner(owner);
            }
        }
        // oh my what a TODO
        player.displayClientMessage(
                Component.literal("Created shop with " + quantity + " " +
                        Component.translatable(player.getOffhandItem().getItem().getDescriptionId()).getString() +
                        (signEntity.getFrontText().getMessage(0,true).getString().toLowerCase().contains("sell") ?
                                (((signEntity.getFrontText().getMessage(0,true).getString().toLowerCase().contains("buy")) ?
                                        " sold and bought" :
                                        " sold"))
                                : " bought") +
                        " for $" + price),
                true);
    }
}