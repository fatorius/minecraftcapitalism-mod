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
            Commands.literal("saldo")
                .requires(src -> src.hasPermission(0))

                // /saldo
                .executes(ConsultSaldo::run)

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
        );


        // /pix <player> <value>
        dispatcher.register(
            Commands.literal("pix")
                .then(
                    Commands.argument("player", EntityArgument.player())
                    .then(
                        Commands.argument("valor", IntegerArgumentType.integer(0))
                        .executes(SendPix::run)
                    )
                )
        );

        // /extrato
        dispatcher.register(
            Commands.literal("extrato")

                // /extrato (página 1 por padrão)
                .executes(ctx -> ConsultExtrato.run(ctx, 1))

                // /extrato <page>
                .then(
                    Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int page = IntegerArgumentType.getInteger(ctx, "page");
                        return ConsultExtrato.run(ctx, page);
                    })
                )
        );

        // /cobrar <player> <value>
        dispatcher.register(
            Commands.literal("cobrar")
                .then(
                    Commands.argument("player", EntityArgument.player())
                    .then(
                        Commands.argument("valor", IntegerArgumentType.integer(1))
                        .executes(Cobrar::criar)
                    )
                )
        );

        // /pagarcobranca
        dispatcher.register(
            Commands.literal("pagarcobranca")
                .then(
                    Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(Cobrar::pagar)
                )
        );

        // /recusarcobranca
        dispatcher.register(
            Commands.literal("recusarcobranca")
                .then(
                    Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(Cobrar::recusar)
                )
        );

        // /tigrinho
        dispatcher.register(
            Commands.literal("tigrinho")
                .then(
                    Commands.argument("value", IntegerArgumentType.integer(1))
                    .executes(Tigrinho::apostar)
                )
        );

        // /sell <item> <quantidade> <valor>
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
        );

        // /mercado
        dispatcher.register(
            Commands.literal("mercado")
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
        );
    }
}

//@formatter:on
