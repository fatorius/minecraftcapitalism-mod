package com.hugosouza.minecraftcapitalism;

import com.hugosouza.minecraftcapitalism.command.*;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.arguments.item.ItemArgument;

//@formatter:off

public final class CommandRegister {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandContext) {
        dispatcher.register(
            Commands.literal("capitalismo")

            // /saldo
            .then(Commands.literal("saldo")
                .requires(src -> src.hasPermission(0))

                .executes(Saldo::run)

                // /saldo set <player> <valor>
                .then(Commands.literal("set")
                    .requires(src -> src.hasPermission(2))
                    .then(
                        Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("valor", IntegerArgumentType.integer(0))
                            .executes(OPSetSaldo::run)
                        )
                    )
                )

                // /saldo add <player> <valor>
                .then(Commands.literal("add")
                    .requires(src -> src.hasPermission(2))
                    .then(
                        Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("valor", IntegerArgumentType.integer(0))
                            .executes(OPAddSaldo::run)
                        )
                    )
                )
            )

            // /pix <player> <value>
            .then(
                dispatcher.register(
                    Commands.literal("pix")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("valor", IntegerArgumentType.integer(0))
                            .executes(SendPix::run)
                        )
                    )
                )
            )

            // /extrato
            .then(
                dispatcher.register(
                    Commands.literal("extrato")

                    // página 1 por padrão
                    .executes(ctx -> Extrato.run(ctx, 1))
                    
                    .then(
                        // /extrato <page>
                        Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int page = IntegerArgumentType.getInteger(ctx, "page");
                            return Extrato.run(ctx, page);
                        })
                    )
                )
            )

            // /cobrar <player> <value>
            .then(
                dispatcher.register(
                    Commands.literal("cobrar")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("valor", IntegerArgumentType.integer(1))
                            .executes(Cobrar::criar)
                        )
                    )
                )
            )

            // /pagarcobranca
            .then(
                dispatcher.register(
                    Commands.literal("pagarcobranca")
                    .then(
                        Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(Cobrar::pagar)
                    )
                )
            )
            
            // /recusarcobranca
            .then(
                dispatcher.register(
                    Commands.literal("recusarcobranca")
                    .then(
                        Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(Cobrar::recusar)
                    )
                )
            )
            
            // /tigrinho
            .then(
                dispatcher.register(
                    Commands.literal("tigrinho")
                    .then(
                        Commands.argument("value", IntegerArgumentType.integer(1))
                        .executes(Tigrinho::apostar)
                    )
                )
            )
            
            // /sell <item> <quantidade> <valor>
            .then(
                dispatcher.register(
                    Commands.literal("sell")
                    .then(
                        Commands.argument("item", ItemArgument.item(commandContext))
                        .then(
                            Commands.argument("quantidade", IntegerArgumentType.integer(1))
                            .then(
                                Commands.argument("valor", IntegerArgumentType.integer(1))
                                .executes(Mercado::sell)
                            )
                        )
                    )
                )
            )
            
            // /mercado
            .then(
                dispatcher.register(
                    Commands.literal("mercado")

                    // página 1 por padrão
                    .executes(ctx -> Mercado.list(ctx, 1))

                    // /extrato <page>
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int page = IntegerArgumentType.getInteger(ctx, "page");
                            return Mercado.list(ctx, page);
                        })
                    )

                    // /mercado buy <id>
                    .then(
                        Commands.literal("buy")
                        .then(
                            Commands.argument("id", IntegerArgumentType.integer(1))
                            .executes(Mercado::buy)
                        )
                    )

                    // /mercado cancel <id>
                    .then(
                        Commands.literal("cancel")
                        .then(
                            Commands.argument("id", IntegerArgumentType.integer(1))
                            .executes(Mercado::cancel)
                        )
                    )

                    // /mercado meusanuncios
                    .then(
                        Commands.literal("meusanuncios")

                        // página 1 por padrão
                        .executes(ctx -> Mercado.myAds(ctx, 1))

                        // /mercado meusanuncios <page>
                        .then(
                            Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                int page = IntegerArgumentType.getInteger(ctx, "page");
                                return Mercado.myAds(ctx, page);
                            })
                        )
                    )
                )
            )
        );
    }
}

//@formatter:on
