package com.hugosouza.minecraftcapitalism.command;


import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.hugosouza.minecraftcapitalism.service.TransactionService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.sql.SQLException;
import java.util.Random;

public final class Tigrinho {
    private static final Random RANDOM = new Random();

    public static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int bet = IntegerArgumentType.getInteger(ctx, "value");

        DbExecutor.runAsync(() -> {
            try {
                int balance = AccountService.getBalance(player.getUUID());

                if (balance < bet) {
                    ctx.getSource().getServer().execute(() ->
                            player.sendSystemMessage(Component.literal("Saldo insuficiente"))
                    );
                    return;
                }

                double multiplier = rollMultiplier();
                int resultAmount = (int) Math.floor(bet * multiplier);

                AccountService.addBalance(player.getUUID(), -bet);

                if (multiplier > 0) {
                    AccountService.addBalance(player.getUUID(), resultAmount);

                    TransactionService.recordTransaction(
                            null,
                            player.getUUID(),
                            resultAmount,
                            "TIGRINHO_WIN"
                    );
                }

                TransactionService.recordTransaction(
                        player.getUUID(),
                        null,
                        bet,
                        "TIGRINHO_BET"
                );

                ctx.getSource().getServer().execute(() -> {
                    if (multiplier == 0) {
                        player.sendSystemMessage(
                                Component.literal("Tigrinho: Você perdeu " + bet)
                        );
                    } else {
                        player.sendSystemMessage(
                                Component.literal(
                                        "Tigrinho: VOCÊ GANHOU!!!! Multiplicador: " + multiplier +
                                                " | Resultado: +" + resultAmount
                                )
                        );
                    }
                });

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(Component.literal("Erro ao processar aposta"))
                );
            }
        });

        return 1;
    }

    private static double rollMultiplier() {
        int roll = RANDOM.nextInt(100) + 1;

        if (roll <= 51) return 0.0;
        if (roll <= 71) return 1.25;
        if (roll <= 86) return 1.5;
        if (roll <= 96) return 2.0;
        if (roll <= 99) return 5.0;
        return 10.0;
    }
}
