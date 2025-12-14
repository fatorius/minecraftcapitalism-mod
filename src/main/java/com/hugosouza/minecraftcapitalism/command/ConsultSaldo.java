package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.sql.SQLException;
import java.util.UUID;

public class ConsultSaldo {
    public static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID uuid = player.getUUID();

        DbExecutor.runAsync(() -> {
            try {
                int balance = DatabaseService.getBalance(uuid);
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(
                                Component.literal("Seu saldo: " + balance)
                        )
                );
            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        player.sendSystemMessage(
                                Component.literal("Erro ao consultar saldo")
                        )
                );
            }
        });
        return 1;
    }
}
