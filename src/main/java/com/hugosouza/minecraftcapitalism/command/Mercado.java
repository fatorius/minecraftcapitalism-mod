package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.interfaces.MarketListing;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.hugosouza.minecraftcapitalism.service.ListingService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Mercado {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int PAGE_SIZE = 5;

    public static int sell(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {

        ServerPlayer player = ctx.getSource().getPlayerOrException();

        ItemInput input = ItemArgument.getItem(ctx, "item");
        Item item = input.getItem();

        int quantity = IntegerArgumentType.getInteger(ctx, "quantidade");
        int unitPrice = IntegerArgumentType.getInteger(ctx, "valor");

        if (!userHasItem(player, item, quantity)) {
            player.sendSystemMessage(
                    Component.literal("Você não possui itens suficientes")
            );
            return 0;
        }

        removeItemFromPlayerInventory(player, item, quantity);

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

    public static int buy(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {

        ServerPlayer buyer = ctx.getSource().getPlayerOrException();
        int listingId = IntegerArgumentType.getInteger(ctx, "id");

        DbExecutor.runAsync(() -> {
            try {
                MarketListing listing = ListingService.getById(listingId);

                if (listing == null) {
                    ctx.getSource().getServer().execute(() ->
                            buyer.sendSystemMessage(Component.literal("Anúncio não encontrado"))
                    );
                    return;
                }

                UUID buyerId = buyer.getUUID();
                UUID sellerId = listing.owner();

                if (buyerId.equals(sellerId)) {
                    ctx.getSource().getServer().execute(() ->
                            buyer.sendSystemMessage(Component.literal("Você não pode comprar seu próprio item"))
                    );
                    return;
                }

                int totalPrice = listing.quantity() * listing.unitPrice();
                int balance = AccountService.getBalance(buyerId);

                if (balance < totalPrice) {
                    ctx.getSource().getServer().execute(() ->
                            buyer.sendSystemMessage(Component.literal("Saldo insuficiente"))
                    );
                    return;
                }

                boolean success = ListingService.buyListing(
                        listing,
                        buyerId
                );

                ctx.getSource().getServer().execute(() -> {
                    if (success) {
                        ResourceLocation itemId = ResourceLocation.parse(listing.itemId());

                        ItemStack stack = new ItemStack(
                                BuiltInRegistries.ITEM.get(itemId),
                                listing.quantity()
                        );

                        buyer.getInventory().add(stack);

                        buyer.sendSystemMessage(
                                Component.literal("Compra realizada com sucesso!")
                        );

                        ServerPlayer seller =
                                ctx.getSource().getServer().getPlayerList().getPlayer(sellerId);

                        if (seller != null) {
                            seller.sendSystemMessage(
                                    Component.literal(
                                            buyer.getName().getString() +
                                                    " comprou seu item por $" + totalPrice
                                    )
                            );
                        }
                    } else {
                        buyer.sendSystemMessage(
                                Component.literal("Erro ao processar compra")
                        );
                    }
                });

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        buyer.sendSystemMessage(
                                Component.literal("Erro interno ao comprar item")
                        )
                );
                LOGGER.error(e.toString());
            }
        });

        return 1;
    }

    public static int list(CommandContext<CommandSourceStack> ctx, int page)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        DbExecutor.runAsync(() -> {
            try {
                ArrayList<MarketListing> listings =
                        ListingService.list(PAGE_SIZE, (page - 1) * PAGE_SIZE);

                ctx.getSource().getServer().execute(() ->
                        sendPage(player, listings, page, ctx)
                );

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(Component.literal("Erro ao carregar mercado"))
                );
            }
        });

        return 1;
    }

    private static boolean userHasItem(ServerPlayer player, Item item, int quantity) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
                if (count >= quantity) return true;
            }
        }
        return false;
    }

    private static void removeItemFromPlayerInventory(ServerPlayer player, Item item, int quantity) {
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

    private static void sendPage(
            ServerPlayer player,
            ArrayList<MarketListing> list,
            int page,
            CommandContext<CommandSourceStack> ctx
    ) {
        player.sendSystemMessage(
                Component.literal("§6=== Mercado (Página " + page + ") ===")
        );

        if (list.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Sem anúncios"));
            return;
        }

        for (MarketListing l : list) {
            Component line = Component.literal(
                            l.quantity() + "x" + " - " + l.itemId()
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(0x00FFAA)))
                    .append(Component.literal(
                            " | $" + l.unitPrice()
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFF55))))
                    .append(Component.literal(" "))
                    .append(
                            Component.literal("[Comprar]")
                                    .withStyle(style -> style
                                            .withColor(TextColor.fromRgb(0x00FF00))
                                            .withBold(true)
                                            .withClickEvent(
                                                    new ClickEvent(
                                                            ClickEvent.Action.RUN_COMMAND,
                                                            "/mercado buy " + l.id()
                                                    )
                                            )
                                            .withHoverEvent(
                                                    new HoverEvent(
                                                            HoverEvent.Action.SHOW_TEXT,
                                                            Component.literal("Clique para comprar")
                                                    )
                                            )
                                    )
                    );
            player.sendSystemMessage(line);
        }

        player.sendSystemMessage(buildPaginationComponent(page));
    }

    private static Component buildPaginationComponent(int page) {
        MutableComponent prev = Component.literal("« Anterior ")
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/mercado " + (page - 1)
                        )
                ));

        Component next = Component.literal(" Próxima »")
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/mercado " + (page + 1)
                        )
                ));

        Component middle = Component.literal(
                " §7[Página " + page + "] "
        );

        if (page <= 1) {
            prev = Component.literal("§7« Anterior ");
        }

        return prev.append(middle).append(next);
    }


}
