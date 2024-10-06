package com.talhanation.recruits.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.talhanation.recruits.world.PillagerPatrolSpawn;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.monster.Pillager;


public class PatrolSpawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("recruits").requires((source) -> source.hasPermission(2));

        literalBuilder.then(Commands.literal("spawn")
                        .then(Commands.literal("pillagerPatrol")
                                .then(Commands.literal("tiny").executes( (commandSource) -> {
                                    PillagerPatrolSpawn.spawnPillagerPatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getEntity().getOnPos(), commandSource.getSource().getLevel());
                                    return 0;
                                }))
                                .then(Commands.literal("small").executes( (commandSource) -> {
                                    PillagerPatrolSpawn.spawnSmallPillagerPatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getEntity().getOnPos(), commandSource.getSource().getLevel());
                                    return 0;
                                }))
                                .then(Commands.literal("medium").executes( (commandSource) -> {
                                    PillagerPatrolSpawn.spawnMediumPillagerPatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getEntity().getOnPos(), commandSource.getSource().getLevel());

                                    return 0;
                                }))
                                .then(Commands.literal("large").executes( (commandSource) -> {
                                    PillagerPatrolSpawn.spawnLargePillagerPatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getEntity().getOnPos(), commandSource.getSource().getLevel());

                                    return 0;
                                }))
                        )
                        .then(Commands.literal("recruitPatrol")
                            .then(Commands.literal("tiny").executes( (commandSource) -> {

                                RecruitsPatrolSpawn.spawnTinyPatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getLevel());
                                return 0;
                            }))
                            .then(Commands.literal("small").executes( (commandSource) -> {

                                RecruitsPatrolSpawn.spawnSmallPatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getLevel());
                                return 0;
                            }))
                            .then(Commands.literal("medium").executes( (commandSource) -> {
                                RecruitsPatrolSpawn.spawnMediumPatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getLevel());
                                return 0;
                            }))
                            .then(Commands.literal("large").executes( (commandSource) -> {
                                RecruitsPatrolSpawn.spawnLargePatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getLevel());
                                return 0;
                            }))
                            .then(Commands.literal("huge").executes( (commandSource) -> {
                                RecruitsPatrolSpawn.spawnHugePatrol(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getLevel());
                                return 0;
                            }))
                            .then(Commands.literal("caravan").executes( (commandSource) -> {
                                RecruitsPatrolSpawn.spawnCaravan(commandSource.getSource().getEntity().getOnPos().above(), commandSource.getSource().getLevel());
                                return 0;
                            }))
                        )
        );

        dispatcher.register(literalBuilder);
    }
/*
case 0 -> PillagerPatrolSpawn.spawnSmallPillagerPatrol(pos, pos, context.getSender().getLevel());
            case 1 -> PillagerPatrolSpawn.spawnPillagerPatrol(pos, pos, context.getSender().getLevel());
            case 2 -> PillagerPatrolSpawn.spawnMediumPillagerPatrol(pos, pos, context.getSender().getLevel());
            case 3 -> PillagerPatrolSpawn.spawnLargePillagerPatrol(pos, pos, context.getSender().getLevel());
            case 10 -> RecruitsPatrolSpawn.spawnCaravan(pos, context.getSender().getLevel());
            case 11 -> RecruitsPatrolSpawn.spawnTinyPatrol(pos, context.getSender().getLevel());
            case 12 -> RecruitsPatrolSpawn.spawnSmallPatrol(pos, context.getSender().getLevel());
            case 13 -> RecruitsPatrolSpawn.spawnMediumPatrol(pos, context.getSender().getLevel());
            case 14 -> RecruitsPatrolSpawn.spawnLargePatrol(pos, context.getSender().getLevel());
            case 15 -> RecruitsPatrolSpawn.spawnHugePatrol(pos, context.getSender().getLevel());
 */
}
