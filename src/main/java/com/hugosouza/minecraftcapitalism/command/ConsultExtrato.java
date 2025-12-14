package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.interfaces.Transaction;
import com.hugosouza.minecraftcapitalism.service.TransactionService;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class ConsultExtrato {
    private static final int PAGE_SIZE = 5;

    public static int run(CommandContext<CommandSourceStack> ctx, int page)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        DbExecutor.runAsync(() -> {
            try {
                List<Transaction> list =
                        TransactionService.getStatement(player.getUUID(), PAGE_SIZE, (page - 1) * PAGE_SIZE);

                ctx.getSource().getServer().execute(() ->
                        sendPage(player, list, page)
                );

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(
                                Component.literal("Erro ao carregar extrato")
                        )
                );
            }
        });

        return 1;
    }

    private static void sendPage(
            ServerPlayer player,
            List<Transaction> list,
            int page
    ) {
        player.sendSystemMessage(
                Component.literal("§6=== Extrato (Página " + page + ") ===")
        );

        if (list.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("Sem transações")
            );
            return;
        }

        for (Transaction tx : list) {
            String line =
                    tx.type() + " | " +
                            tx.amount() + " | " +
                            Instant.ofEpochMilli(tx.timestamp())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();

            player.sendSystemMessage(Component.literal(line));
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
