package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.sql.SQLException;

public class OPSetSaldo {
    public static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int value = IntegerArgumentType.getInteger(ctx, "valor");

        DbExecutor.runAsync(() -> {
            try {
                AccountService.setBalance(target.getUUID(), value);
                ctx.getSource().getServer().execute(() ->
                        ctx.getSource().sendSuccess(
                                () -> Component.literal(
                                        "Saldo de " + target.getName().getString() +
                                                " definido para " + value
                                ),
                                false
                        )
                );
            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        ctx.getSource().sendFailure(
                                Component.literal("Erro ao definir saldo")
                        )
                );
            }
        });
        return 1;
    }
}
