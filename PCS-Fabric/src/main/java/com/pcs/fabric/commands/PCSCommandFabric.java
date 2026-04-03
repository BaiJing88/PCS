package com.pcs.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class PCSCommandFabric {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("pcs")
                        .then(CommandManager.literal("status")
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    src.sendFeedback(() -> Text.literal("PCS mod running."), false);
                                    return 1;
                                }))
                        .then(CommandManager.literal("echo")
                                .then(CommandManager.argument("msg", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String msg = StringArgumentType.getString(ctx, "msg");
                                            ctx.getSource().sendFeedback(() -> Text.literal("echo: " + msg), false);
                                            return 1;
                                        })))
        );
    }
}
