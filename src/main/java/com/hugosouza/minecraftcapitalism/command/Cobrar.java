package com.hugosouza.minecraftcapitalism.command;

import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import com.hugosouza.minecraftcapitalism.interfaces.Invoice;
import com.hugosouza.minecraftcapitalism.service.AccountService;
import com.hugosouza.minecraftcapitalism.service.InvoiceService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.sql.SQLException;

public class Cobrar {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int criar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "valor");

        if (amount <= 0) {
            sender.sendSystemMessage(Component.literal("Valor inválido"));
            return 0;
        }

        DbExecutor.runAsync(() -> {
            try {
                int invoiceId = InvoiceService.createInvoice(sender.getUUID(), target.getUUID(), amount);

                ctx.getSource().getServer().execute(() -> {
                    sender.sendSystemMessage(Component.literal(
                            "Cobrança enviada para " + target.getName().getString() + ": " + amount));

                    Component message = Component.literal(
                                    sender.getName().getString() + " te cobrou " + amount + ". "
                            )
                            .append(Component.literal("[Pagar]")
                                    .withStyle(style -> style
                                            .withColor(TextColor.fromRgb(0x00FF00))
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pagarcobranca " + invoiceId))
                                    ))
                            .append(Component.literal(" "))
                            .append(Component.literal("[Recusar]")
                                    .withStyle(style -> style
                                            .withColor(TextColor.fromRgb(0xFF0000))
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/recusarcobranca " + invoiceId))
                                    ));

                    target.sendSystemMessage(message);
                });

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        sender.sendSystemMessage(Component.literal("Erro ao criar cobrança")));

                LOGGER.error(e.toString());
            }
        });

        return 1;
    }

    public static int pagar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer payer = ctx.getSource().getPlayerOrException();
        int invoiceId = IntegerArgumentType.getInteger(ctx, "id");

        DbExecutor.runAsync(() -> {
            try {
                Invoice invoice = InvoiceService.getInvoice(invoiceId);

                if (invoice == null || !"PENDING".equals(invoice.status()) || !invoice.toUuid().equals(payer.getUUID())) {
                    ctx.getSource().getServer().execute(() ->
                            payer.sendSystemMessage(Component.literal("Cobrança inválida ou já paga"))
                    );
                    return;
                }

                boolean success = AccountService.transfer(payer.getUUID(), invoice.fromUuid(), invoice.amount());

                if (success) {
                    InvoiceService.markAsPaid(invoiceId);

                    ctx.getSource().getServer().execute(() -> {
                        payer.sendSystemMessage(Component.literal("Cobrança paga com sucesso!"));

                        ServerPlayer senderPlayer = ctx.getSource().getServer().getPlayerList().getPlayer(invoice.fromUuid());

                        if (senderPlayer != null) {
                            senderPlayer.sendSystemMessage(Component.literal(
                                    payer.getName().getString() + " pagou sua cobrança!"
                            ));
                        }
                    });

                } else {
                    ctx.getSource().getServer().execute(() ->
                            payer.sendSystemMessage(Component.literal("Saldo insuficiente para pagar a cobrança"))
                    );
                }

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        payer.sendSystemMessage(Component.literal("Erro ao processar pagamento da cobrança"))
                );

                LOGGER.error(e.toString());
            }
        });

        return 1;
    }


    public static int recusar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer payer = ctx.getSource().getPlayerOrException();
        int invoiceId = IntegerArgumentType.getInteger(ctx, "id");

        DbExecutor.runAsync(() -> {
            try {
                Invoice invoice = InvoiceService.getInvoice(invoiceId);

                if (invoice == null || !"PENDING".equals(invoice.status()) || !invoice.toUuid().equals(payer.getUUID())) {
                    ctx.getSource().getServer().execute(() ->
                            payer.sendSystemMessage(Component.literal("Cobrança inválida ou já processada"))
                    );
                    return;
                }

                InvoiceService.markAsCancelled(invoiceId);

                ctx.getSource().getServer().execute(() -> {
                    payer.sendSystemMessage(Component.literal("Cobrança recusada!"));

                    ServerPlayer senderPlayer = ctx.getSource().getServer().getPlayerList().getPlayer(invoice.fromUuid());
                    if (senderPlayer != null) {
                        senderPlayer.sendSystemMessage(Component.literal(
                                payer.getName().getString() + " recusou sua cobrança de " + invoice.amount()
                        ));
                    }
                });

            } catch (SQLException e) {
                ctx.getSource().getServer().execute(() ->
                        payer.sendSystemMessage(Component.literal("Erro ao processar recusa da cobrança"))
                );

                LOGGER.error(e.toString());
            }
        });

        return 1;
    }

}
