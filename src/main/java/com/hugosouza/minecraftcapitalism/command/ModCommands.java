package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.sql.SQLException;
import java.util.UUID;

public final class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("saldo")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            UUID uuid = player.getUUID();

                            DbExecutor.runAsync(() -> {
                                int balance;

                                try {
                                    balance = DatabaseService.getBalance(uuid);
                                } catch (SQLException e) {
                                    ctx.getSource().getServer().execute(() ->
                                            player.sendSystemMessage(
                                                    Component.literal("Erro ao consultar saldo, mande uma mensagem para o hugo no discord: hugosouza0247")
                                            )
                                    );
                                    return;
                                }

                                ctx.getSource().getServer().execute(() ->
                                        player.sendSystemMessage(
                                                Component.literal("Seu saldo: " + balance)
                                        )
                                );
                            });

                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("pix")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Saldo: 100"),
                                    false
                            );
                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("cobrar")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Imposto cobrado"),
                                    false
                            );
                            return 1;
                        })
        );
    }

}
