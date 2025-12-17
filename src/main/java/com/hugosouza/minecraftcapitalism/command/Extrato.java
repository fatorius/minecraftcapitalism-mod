package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.interfaces.Transaction;
import com.hugosouza.minecraftcapitalism.service.TransactionService;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Extrato {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int PAGE_SIZE = 5;

    public static int run(CommandContext<CommandSourceStack> ctx, int page)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        DbExecutor.runAsync(() -> {
            try {
                List<Transaction> list =
                        TransactionService.getStatement(player.getUUID(), PAGE_SIZE, (page - 1) * PAGE_SIZE);

                ctx.getSource().getServer().execute(() ->
                        sendPage(player, list, page, ctx)
                );

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(
                                Component.literal("Erro ao carregar extrato")
                        )
                );

                LOGGER.error(e.toString());
            }
        });

        return 1;
    }

    private static void sendPage(ServerPlayer player, List<Transaction> list, int page, CommandContext<CommandSourceStack> ctx) {
        player.sendSystemMessage(
                Component.literal("§6=== Extrato (Página " + page + ") ===")
        );

        if (list.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Sem transações"));
            return;
        }

        for (Transaction tx : list) {
            boolean isIncoming = player.getUUID().toString().equals(tx.to());
            String otherPlayerUUID = isIncoming ? tx.from() : tx.to();

            String senderName = "";

            if (!isIncoming){
                if (Objects.equals(tx.type(), "TIGRINHO_BET")){
                    senderName = "Tigrinho";
                }
                else{
                    try{
                        senderName = ctx.getSource().getServer().getPlayerList().getPlayer(UUID.fromString(otherPlayerUUID)).getName().getString();
                    } catch (NullPointerException ignored) {}
                }
            }
            else{
                if (Objects.equals(tx.type(), "TIGRINHO_WIN")){
                    senderName = "Tigrinho";
                }
                else{
                    try{
                        senderName = ctx.getSource().getServer().getPlayerList().getPlayer(UUID.fromString(otherPlayerUUID)).getName().getString();
                    } catch (NullPointerException ignored) {}
                }
            }

            int amount = tx.amount();

            String sign = isIncoming ? "+" : "-";
            int color = isIncoming ? 0x00FF00 : 0xFF5555;

            Component line = Component.literal(
                            "[" + tx.type() + "] " +
                                    sign + amount + " | " + senderName + " | "
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(color)))
                    .append(Component.literal(
                            Instant.ofEpochMilli(tx.timestamp())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime().toString()
                    ).withStyle(style -> style.withColor(TextColor.fromRgb(0xAAAAAA))));

            player.sendSystemMessage(line);
        }

        player.sendSystemMessage(buildPaginationComponent(page));
    }


    private static Component buildPaginationComponent(int page) {
        MutableComponent prev = Component.literal("« Anterior ")
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/extrato " + (page - 1)
                        )
                ));

        Component next = Component.literal(" Próxima »")
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/extrato " + (page + 1)
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
