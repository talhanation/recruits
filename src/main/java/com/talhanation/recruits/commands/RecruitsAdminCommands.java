package com.talhanation.recruits.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecruitsAdminCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("recruits").requires((source) -> source.hasPermission(2));
        //TeamCommand
        literalBuilder.then(Commands.literal("admin")
                        .then(Commands.literal("tpRecruitsToOwner")
                                .then(Commands.argument("Owner", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).executes((conetext) -> {

                                    ServerLevel level = conetext.getSource().getLevel();


                                    return tpToOwner(level, ScoreHolderArgument.getNamesWithDefaultWildcard(conetext, "Owner"));
                                })))
                        );

        dispatcher.register(literalBuilder);
    }

    private static int tpToOwner(ServerLevel level, Collection<String> names) {
        List<ServerPlayer> players = level.getPlayers(player -> names.contains(player.getScoreboardName()));
        List<Entity> allEntities = new ArrayList<>();
        level.getEntities().getAll().iterator().forEachRemaining(allEntities::add);

        List<AbstractRecruitEntity> recruits = allEntities.stream()
                .filter(entity -> entity instanceof AbstractRecruitEntity recruit && recruit.isOwned())
                .map(entity -> (AbstractRecruitEntity) entity)
                .toList();


        Map<UUID, ServerPlayer> playerMap = players.stream()
                .collect(Collectors.toMap(ServerPlayer::getUUID, Function.identity()));


        for (AbstractRecruitEntity recruit : recruits) {
            ServerPlayer player = playerMap.get(recruit.getOwnerUUID());
            if (player != null) {
                recruit.teleportTo(player.getX(), player.getY(), player.getZ());
            }
        }

        return 1;
    }
}
