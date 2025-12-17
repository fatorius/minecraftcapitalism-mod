package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.interfaces.ServerMarketListing;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.hugosouza.minecraftcapitalism.service.ServerMarketService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
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

public class ServerMarket {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int PAGE_SIZE = 5;

    public static int sell(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {

        ServerPlayer seller = ctx.getSource().getPlayerOrException();

        int listingId = IntegerArgumentType.getInteger(ctx, "id");

        DbExecutor.runAsync(() -> {
            try {
                ServerMarketListing listing = ServerMarketService.getSellAdById(listingId);

                if (listing == null) {
                    ctx.getSource().getServer().execute(() ->
                            seller.sendSystemMessage(Component.literal("Anúncio não encontrado"))
                    );
                    return;
                }

                int totalPrice = listing.price();

                AccountService.addBalance(seller.getUUID(), totalPrice, "Venda de " + listing.itemId());

                ctx.getSource().getServer().execute(() -> {
                    ResourceLocation rl = ResourceLocation.parse(listing.itemId());
                    Item item = BuiltInRegistries.ITEM.get(rl);

                    removeItemFromPlayerInventory(
                            seller,
                            item
                    );

                    seller.sendSystemMessage(
                            Component.literal("Venda realizada com sucesso!")
                    );
                });

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        seller.sendSystemMessage(
                                Component.literal("Erro interno ao comprar item")
                        )
                );
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
                ServerMarketListing listing = ServerMarketService.getBuyAdById(listingId);

                if (listing == null) {
                    ctx.getSource().getServer().execute(() ->
                            buyer.sendSystemMessage(Component.literal("Anúncio não encontrado"))
                    );
                    return;
                }

                UUID buyerId = buyer.getUUID();

                int totalPrice = listing.price();
                int balance = AccountService.getBalance(buyerId);

                if (balance < totalPrice) {
                    ctx.getSource().getServer().execute(() ->
                            buyer.sendSystemMessage(Component.literal("Saldo insuficiente"))
                    );
                    return;
                }

                AccountService.removeBalance(buyerId, totalPrice, "Compra de " + listing.itemId());

                ctx.getSource().getServer().execute(() -> {
                    ResourceLocation itemId = ResourceLocation.parse(listing.itemId());

                    ItemStack stack = new ItemStack(
                            BuiltInRegistries.ITEM.get(itemId),
                            1
                    );

                    buyer.getInventory().add(stack);

                    buyer.sendSystemMessage(
                            Component.literal("Compra realizada com sucesso!")
                    );
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

    public static int listBuy(CommandContext<CommandSourceStack> ctx, int page)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        DbExecutor.runAsync(() -> {
            try {
                ArrayList<ServerMarketListing> listings =
                        ServerMarketService.listBuy(PAGE_SIZE, (page - 1) * PAGE_SIZE);

                ctx.getSource().getServer().execute(() ->
                        sendBuyPage(player, listings, page)
                );

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(Component.literal("Erro ao carregar mercado do server"))
                );
                LOGGER.error(e.toString());
            }
        });

        return 1;
    }

    public static int listSell(CommandContext<CommandSourceStack> ctx, int page)
            throws CommandSyntaxException {

        ServerPlayer player = ctx.getSource().getPlayerOrException();

        DbExecutor.runAsync(() -> {
            try {
                ArrayList<ServerMarketListing> listings =
                        ServerMarketService.listSell(PAGE_SIZE, (page - 1) * PAGE_SIZE, player.getUUID().toString());

                ctx.getSource().getServer().execute(() ->
                        sendSellPage(player, listings, page)
                );

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(Component.literal("Erro ao carregar mercado do server"))
                );
                LOGGER.error(e.toString());
            }
        });

        return 1;
    }

    private static void removeItemFromPlayerInventory(ServerPlayer player, Item item) {
        int remaining = 1;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                int removed = Math.min(stack.getCount(), remaining);
                stack.shrink(removed);
                remaining -= removed;
                if (remaining == 0) break;
            }
        }
    }

    private static void sendSellPage(
            ServerPlayer player,
            ArrayList<ServerMarketListing> list,
            int page
    ) {
        player.sendSystemMessage(
                Component.literal("§6=== Mercado (Página " + page + ") ===")
        );

        if (list.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Sem anúncios"));
            return;
        }

        for (ServerMarketListing l : list) {
            Component line = Component.literal(
                            "1x - " + l.itemId()
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(0x00FFAA)))
                    .append(Component.literal(
                            " | $" + l.price()
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFF55))))
                    .append(Component.literal(" "))
                    .append(
                            Component.literal("[Vender]")
                                    .withStyle(style -> style
                                            .withColor(TextColor.fromRgb(0x00FF00))
                                            .withBold(true)
                                            .withClickEvent(
                                                    new ClickEvent(
                                                            ClickEvent.Action.RUN_COMMAND,
                                                            "/servermarket sell item " + l.id()
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

        player.sendSystemMessage(buildSellPaginationComponent(page));
    }

    private static Component buildSellPaginationComponent(int page) {
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

    private static void sendBuyPage(
            ServerPlayer player,
            ArrayList<ServerMarketListing> list,
            int page
    ) {
        player.sendSystemMessage(
                Component.literal("§6=== Mercado (Página " + page + ") ===")
        );

        if (list.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Sem anúncios"));
            return;
        }

        for (ServerMarketListing l : list) {
            Component line = Component.literal(
                            "1x - " + l.itemId()
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(0x00FFAA)))
                    .append(Component.literal(
                            " | $" + l.price()
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFF55))))
                    .append(Component.literal(" "))
                    .append(
                            Component.literal("[Comprar]")
                                    .withStyle(style -> style
                                            .withColor(TextColor.fromRgb(0x00FF99))
                                            .withBold(true)
                                            .withClickEvent(
                                                    new ClickEvent(
                                                            ClickEvent.Action.RUN_COMMAND,
                                                            "/servermarket buy item " + l.id()
                                                    )
                                            )
                                            .withHoverEvent(
                                                    new HoverEvent(
                                                            HoverEvent.Action.SHOW_TEXT,
                                                            Component.literal("Clique para remover esse anuncio")
                                                    )
                                            )
                                    )
                    );
            player.sendSystemMessage(line);
        }

        player.sendSystemMessage(buildBuyPaginationComponent(page));
    }

    private static Component buildBuyPaginationComponent(int page) {
        MutableComponent prev = Component.literal("« Anterior ")
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/mercado meusanuncios" + (page - 1)
                        )
                ));

        Component next = Component.literal(" Próxima »")
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/mercado meusanuncios" + (page + 1)
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
