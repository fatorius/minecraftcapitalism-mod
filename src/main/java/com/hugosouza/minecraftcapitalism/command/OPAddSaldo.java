package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.sql.SQLException;

public class OPAddSaldo {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int delta = IntegerArgumentType.getInteger(ctx, "valor");

        DbExecutor.runAsync(() -> {
            try {
                AccountService.addBalance(target.getUUID(), delta);
                ctx.getSource().getServer().execute(() ->
                        ctx.getSource().sendSuccess(
                                () -> Component.literal(
                                        "Saldo de " + target.getName().getString() +
                                                " alterado em " + delta
                                ),
                                false
                        )
                );
            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        ctx.getSource().sendFailure(
                                Component.literal("Erro ao adicionar saldo")
                        )
                );

                LOGGER.error(e.toString());
            }
        });

        return 1;
    }
}
