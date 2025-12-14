package com.hugosouza.minecraftcapitalism;

import com.hugosouza.minecraftcapitalism.command.*;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public final class CommandRegister {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("saldo")
                        .requires(src -> src.hasPermission(0))

                        // /saldo
                        .executes(ConsultSaldo::run)

                        // /saldo set <player> <valor>
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("valor", IntegerArgumentType.integer(0))
                                                .executes(OPSetSaldo::run))
                                )
                        )

                        // /saldo add <player> <valor>
                        .then(Commands.literal("add")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("valor", IntegerArgumentType.integer()))
                                        .executes(OPAddSaldo::run)
                                )
                        )
        );


        // /pix <player> <value>
        dispatcher.register(
                Commands.literal("pix")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("valor", IntegerArgumentType.integer()))
                        .executes(SendPix::run)
                        )
        );

        // /extrato
        dispatcher.register(
                Commands.literal("extrato")
                        // /extrato (página 1 por padrão)
                        .executes(ctx -> ConsultExtrato.run(ctx, 1))

                        // /extrato <page>
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int page = IntegerArgumentType.getInteger(ctx, "page");
                                    return ConsultExtrato.run(ctx, page);
                                })
                        )
        );
    }

}
