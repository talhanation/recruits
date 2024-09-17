package com.talhanation.recruits.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.network.MessageCommandPatrolSpawn;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;


public class PatrolSpawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("recruits").requires((source) -> source.hasPermission(2));

        literalBuilder.then(Commands.literal("spawn")
                        .then(Commands.literal("pillagerPatrol")
                                .then(Commands.literal("tiny").executes( (commandSource) -> {
                                    //Something
                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(0));
                                    return 0;
                                }))
                                .then(Commands.literal("small").executes( (commandSource) -> {
                                    //Something
                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(1));
                                    return 0;
                                }))
                                .then(Commands.literal("medium").executes( (commandSource) -> {
                                    //Something
                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(2));
                                    return 0;
                                }))
                                .then(Commands.literal("large").executes( (commandSource) -> {
                                    //Something
                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(3));
                                    return 0;
                                }))
                        )
                        .then(Commands.literal("recruitPatrol")
                            .then(Commands.literal("tiny").executes( (commandSource) -> {
                                //Something
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(11));
                                return 0;
                            }))
                            .then(Commands.literal("small").executes( (commandSource) -> {
                                //Something
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(12));
                                return 0;
                            }))
                            .then(Commands.literal("medium").executes( (commandSource) -> {
                                //Something
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(13));
                                return 0;
                            }))
                            .then(Commands.literal("large").executes( (commandSource) -> {
                                //Something
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(14));
                                return 0;
                            }))
                            .then(Commands.literal("huge").executes( (commandSource) -> {
                                //Something
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(15));
                                return 0;
                            }))
                            .then(Commands.literal("caravan").executes( (commandSource) -> {
                                //Something
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandPatrolSpawn(10));
                                return 0;
                            }))
                        )
        );

        dispatcher.register(literalBuilder);
    }

}
