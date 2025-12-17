package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.UUID;

public class Saldo {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID uuid = player.getUUID();

        DbExecutor.runAsync(() -> {
            try {
                int balance = AccountService.getBalance(uuid);
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

                LOGGER.error(e.toString());
            }
        });
        return 1;
    }
}
