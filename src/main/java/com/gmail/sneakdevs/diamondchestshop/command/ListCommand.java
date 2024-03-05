package com.gmail.sneakdevs.diamondchestshop.command;

import com.gmail.sneakdevs.diamondchestshop.DiamondChestShop;
import com.gmail.sneakdevs.diamondchestshop.config.DiamondChestShopConfig;
import com.gmail.sneakdevs.diamondchestshop.interfaces.SignBlockEntityInterface.ShopType;
import com.gmail.sneakdevs.diamondchestshop.sql.ChestshopDatabaseManager;
import com.gmail.sneakdevs.diamondchestshop.sql.ShopRecord;
import com.gmail.sneakdevs.diamondchestshop.util.DiamondChestShopUtil;
import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.command.CommandExceptions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class ListCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal("list")
                .then(
                        Commands.argument("playerName", StringArgumentType.string())
                                .executes(e -> {
                                    String playerName = StringArgumentType.getString(e, "playerName");
                                    String playerUUID = DiamondUtils.getDatabaseManager().getUUIDFromName(playerName);
                                    if (playerUUID != null) {
                                        return sendListToPlayer(e, playerUUID, Component.literal(playerName));
                                    } else {
                                        throw CommandExceptions.PLAYER_NOT_FOUND.create();
                                    }
                                })
                )
                .then(
                        Commands.argument("player", EntityArgument.player())
                                .executes(e -> {
                                    Player player = EntityArgument.getPlayer(e, "player");
                                    return sendListToPlayer(e, player.getStringUUID(), player.getName());
                                })
                )
                .executes(e -> {
                    Player player = e.getSource().getPlayer();
                    if (player != null) {
                        return sendListToPlayer(e, player.getStringUUID(), player.getName());
                    } else {
                        throw CommandExceptions.PLAYER_NOT_FOUND.create();
                    }
                });
    }

    private static int sendListToPlayer(CommandContext<CommandSourceStack> ctx, String playerUUID, Component playerName) {
        ChestshopDatabaseManager csdm = DiamondChestShop.getDatabaseManager();
        List<ShopRecord> shops = csdm.getAllShops(playerUUID);
        var out = DiamondChestShopConfig.ChatPrefix()
                .append(playerName)
                .append(" has " + shops.size() + " shop(s)" + (!shops.isEmpty() ? ":" : ""));
        MutableComponent divider = Component.literal(" ┃ ").setStyle(Style.EMPTY.withBold(true));
        for (ShopRecord shop : shops) {
            String itemString = Component.translatable(shop.item().getDescriptionId()).getString();
            String currentyString = DiamondChestShopConfig.currencyToLiteral(shop.priceSum()).getString();
            out
                    .append("\n")
                    .append(Component.literal("#" + shop.id())
                            .setStyle(Style.EMPTY.withBold(true))
                    )
                    .append(divider)
                    .append(itemString)
                    .append(divider)
                    .append(shop.amountSum() + " ")
                    .append(Component.literal(shop.type() == ShopType.SELL ? "sales (" : "bought (")
                            .setStyle(Style.EMPTY.withItalic(true))
                    )
                    .append(currentyString + ")")
                    .append(divider);
            if (shop.valid()) {
                out.append(" ").append(Component.literal("[close]")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestshop close " + shop.id()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to close the shop!")))
                        )

                );
            } else {
                out.append(Component.literal(" closed!")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
            }

        }
        ctx.getSource().sendSuccess(() -> out, false);
        return 1;
    }
}
