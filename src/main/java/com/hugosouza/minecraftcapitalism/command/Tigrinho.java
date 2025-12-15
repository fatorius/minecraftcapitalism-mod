package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.Random;

public final class Tigrinho {
    private static final Random RANDOM = new Random();

    public static int apostar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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
                AccountService.removeBalance(player.getUUID(), bet, "TIGRINHO_BET");

                if (resultAmount > 0) {
                    AccountService.addBalance(player.getUUID(), resultAmount, "TIGRINHO_WIN");
                }

                ctx.getSource().getServer().execute(() -> {
                    if (multiplier == 0.0) {
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
                        player.sendSystemMessage(Component.literal("Erro ao processar aposta:" + e.getMessage()))
                );
            }
        });

        return 1;
    }

    private static double rollMultiplier() {
        int roll = RANDOM.nextInt(100) + 1;

        if (roll <= 51) return 0.0; // 51% de chance de não dar nada
        if (roll <= 71) return 1.25; // 20% de chance de voltar 1.25x
        if (roll <= 86) return 1.5; // 15% de chance de voltar 1.5x
        if (roll <= 96) return 2.0; // 10% de chance de voltar 2x
        if (roll <= 99) return 5.0; // 3% de chance de voltar 5x
        return 10.0;    // 1% de chance de voltar 10x
    }
}
