package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.service.ListingService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.sql.SQLException;

public class Mercado {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int sell(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {

        ServerPlayer player = ctx.getSource().getPlayerOrException();

        ItemInput input = ItemArgument.getItem(ctx, "item");
        Item item = input.getItem();

        int quantity = IntegerArgumentType.getInteger(ctx, "quantidade");
        int unitPrice = IntegerArgumentType.getInteger(ctx, "valor");

        if (!hasItem(player, item, quantity)) {
            player.sendSystemMessage(
                    Component.literal("Você não possui itens suficientes")
            );
            return 0;
        }

        removeItem(player, item, quantity);

        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

        DbExecutor.runAsync(() -> {
            try {
                ListingService.create(
                        player.getUUID(),
                        itemId,
                        quantity,
                        unitPrice
                );

                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(
                                Component.literal("Item listado no mercado com sucesso!")
                        )
                );

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() -> {
                    player.getInventory().add(
                            new ItemStack(item, quantity)
                    );
                    player.sendSystemMessage(
                            Component.literal("Erro ao listar item no mercado")
                    );
                });

                LOGGER.error(e.toString());
            }
        });

        return 1;
    }

    private static boolean hasItem(ServerPlayer player, Item item, int quantity) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
                if (count >= quantity) return true;
            }
        }
        return false;
    }

    private static void removeItem(ServerPlayer player, Item item, int quantity) {
        int remaining = quantity;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                int removed = Math.min(stack.getCount(), remaining);
                stack.shrink(removed);
                remaining -= removed;
                if (remaining == 0) break;
            }
        }
    }
}
