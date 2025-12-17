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

public final class SendPix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int run(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {

        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "valor");

        if (amount <= 0) {
            sender.sendSystemMessage(
                    Component.literal("Valor inválido")
            );
            return 0;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(
                    Component.literal("Você não pode enviar Pix para si mesmo")
            );
            return 0;
        }

        DbExecutor.runAsync(() -> {
            try {
                boolean success = AccountService.transfer(
                        sender.getUUID(),
                        target.getUUID(),
                        amount
                );

                ctx.getSource().getServer().execute(() -> {
                    if (!success) {
                        sender.sendSystemMessage(
                                Component.literal("Saldo insuficiente")
                        );
                    } else {
                        sender.sendSystemMessage(
                                Component.literal(
                                        "Pix enviado: " + amount +
                                                " para " + target.getName().getString()
                                )
                        );
                        target.sendSystemMessage(
                                Component.literal(
                                        "Você recebeu um Pix de " + amount +
                                                " de " + sender.getName().getString()
                                )
                        );
                    }
                });
            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        sender.sendSystemMessage(
                                Component.literal("Erro ao processar Pix")
                        )
                );
                LOGGER.error(e.toString());
            }
        });

        return 1;
    }
}
