package com.talhanation.recruits.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.items.RecruitsSpawnEgg;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.command.EnumArgument;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecruitsAdminCommands {
    private static final List<String> RELATIONS = List.of("Ally", "Neutral", "Enemy");

    private static final SuggestionProvider<CommandSourceStack> RELATION_SUGGESTIONS =
            SuggestionProviders.register(new ResourceLocation("recruits:relations"),
                    (context, builder) -> {
                        for (String relation : RELATIONS) {
                            builder.suggest(relation);
                        }
                        return builder.buildFuture();
                    });
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("recruits").requires((source) -> source.hasPermission(2));
        //TeamCommand
        literalBuilder.then(Commands.literal("admin")
            .then(Commands.literal("tpRecruitsToOwner")
                .then(Commands.argument("Owner", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).executes((conetext) -> {

                    ServerLevel level = conetext.getSource().getLevel();


                    return tpToOwner(level, ScoreHolderArgument.getNamesWithDefaultWildcard(conetext, "Owner"));
            })))
            .then(Commands.literal("unitsManager")
                .then(Commands.literal("getUnitsCount")
                        .then(Commands.argument("Player", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                            .executes((context) -> {
                                String playerName = ScoreHolderArgument.getName(context, "Player");
                                ServerPlayer player = context.getSource().getLevel().getServer().getPlayerList().getPlayerByName(playerName);

                                if(player == null) {
                                    context.getSource().sendFailure(Component.literal("No Player found!").withStyle(ChatFormatting.RED));
                                    return 0;
                                }

                                int unitCount = getUnitsCount(player);
                                context.getSource().sendSuccess(() ->
                                        Component.literal(player.getName().getString() + " has " + unitCount + " from max. " + RecruitsServerConfig.MaxRecruitsForPlayer.get()), false);
                                return 1;
                            }))
                )
                .then(Commands.literal("setUnitsCount")
                        .then(Commands.argument("Player", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                .then(Commands.argument("Amount", IntegerArgumentType.integer(0))
                                        .executes((context) -> {
                                            String playerName = ScoreHolderArgument.getName(context, "Player");
                                            ServerPlayer player = context.getSource().getLevel().getServer().getPlayerList().getPlayerByName(playerName);

                                            if(player == null) {
                                                context.getSource().sendFailure(Component.literal("No Player found!").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            int amount = IntegerArgumentType.getInteger(context, "Amount");

                                            return setUnitsCount(context, player, amount);
                                        })
                                )
                        )
                )
            )
            .then(Commands.literal("factionManager")
                .then(Commands.literal("getNPCCount")
                        .then(Commands.argument("Faction", TeamArgument.team())
                                .executes((context) -> {
                                    PlayerTeam playerTeam = TeamArgument.getTeam(context, "Faction");
                                    RecruitsTeam faction = TeamEvents.recruitsTeamManager.getTeamByStringID(playerTeam.getName());
                                    if(faction == null) {
                                        context.getSource().sendFailure(Component.literal("No Faction found!").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(() ->
                                            Component.literal(faction.getTeamDisplayName() + " has " + faction.npcs + " from max. " + faction.maxNPCs), false);
                                    return 1;
                                }))
                )
                .then(Commands.literal("setNPCCount")
                        .then(Commands.argument("Faction", TeamArgument.team())
                                .then(Commands.argument("Amount", IntegerArgumentType.integer(0))
                                        .executes((context) -> {
                                            PlayerTeam playerTeam = TeamArgument.getTeam(context, "Faction");
                                            RecruitsTeam faction = TeamEvents.recruitsTeamManager.getTeamByStringID(playerTeam.getName());
                                            if(faction == null) {
                                                context.getSource().sendFailure(Component.literal("No Faction found!").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                            int amount = IntegerArgumentType.getInteger(context, "Amount");

                                            return setFactionNPCsCount(context, faction, amount);
                                        })
                                )
                        )
                )
                .then(Commands.literal("getLeader")
                        .then(Commands.argument("Faction", TeamArgument.team())
                                .executes((context) -> {
                                    PlayerTeam playerTeam = TeamArgument.getTeam(context, "Faction");
                                    RecruitsTeam faction = TeamEvents.recruitsTeamManager.getTeamByStringID(playerTeam.getName());
                                    if(faction == null) {
                                        context.getSource().sendFailure(Component.literal("No Faction found!").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("The Leader of " + faction.getTeamDisplayName() + " is " + faction.getTeamLeaderName()), false);
                                    return 1;
                                }))
                )
                .then(Commands.literal("setLeader")
                    .then(Commands.argument("Faction", TeamArgument.team())
                        .then(Commands.argument("Player", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                            .executes((context) -> {
                                PlayerTeam playerTeam = TeamArgument.getTeam(context, "Faction");
                                RecruitsTeam faction = TeamEvents.recruitsTeamManager.getTeamByStringID(playerTeam.getName());

                                String playerName = ScoreHolderArgument.getName(context, "Player");
                                ServerPlayer player = context.getSource().getLevel().getServer().getPlayerList().getPlayerByName(playerName);

                                if(faction == null) {
                                    context.getSource().sendFailure(Component.literal("No Faction found!").withStyle(ChatFormatting.RED));
                                    return 0;
                                }

                                if(player == null) {
                                    context.getSource().sendFailure(Component.literal("No Player found!").withStyle(ChatFormatting.RED));
                                    return 0;
                                }

                                if(TeamEvents.isPlayerAlreadyAFactionLeader(player)){
                                    context.getSource().sendFailure(Component.literal("Player is already a Leader of another Faction!").withStyle(ChatFormatting.RED));
                                    return 0;
                                }

                                if(!playerTeam.getPlayers().contains(playerName)){
                                    TeamEvents.addPlayerToTeam(null, context.getSource().getLevel(), faction.getStringID(), playerName);
                                }

                                faction.setTeamLeaderID(player.getUUID());
                                faction.setTeamLeaderName(player.getName().getString());

                                TeamEvents.modifyTeam(context.getSource().getLevel(), faction.getStringID(), faction, context.getSource().getPlayer(), 0);

                                TeamEvents.recruitsTeamManager.save(context.getSource().getLevel());

                                context.getSource().sendSuccess(() ->
                                        Component.literal("The Leader of " + faction.getTeamDisplayName() + " is now " + faction.getTeamLeaderName()), false);
                                return 1;
                            })))
                )
            )
            .then(Commands.literal("diplomacyManager")
                    /*
                    .then(Commands.literal("getRelations")
                            .then(Commands.argument("Faction", TeamArgument.team())
                                    .executes((context) -> {
                                        PlayerTeam playerTeam = TeamArgument.getTeam(context, "Faction");
                                        return 1;
                                    })
                            )
                    )
                     */

                    .then(Commands.literal("setRelations")
                            .then(Commands.argument("Faction1", TeamArgument.team())
                                    .then(Commands.argument("Faction2", TeamArgument.team())
                                            .then(Commands.argument("Relation", EnumArgument.enumArgument(RecruitsDiplomacyManager.DiplomacyStatus.class))
                                                    .executes((context) -> {
                                                        PlayerTeam playerTeam1 = TeamArgument.getTeam(context, "Faction1");
                                                        PlayerTeam playerTeam2 = TeamArgument.getTeam(context, "Faction2");
                                                        RecruitsDiplomacyManager.DiplomacyStatus status = context.getArgument("Relation", RecruitsDiplomacyManager.DiplomacyStatus.class);

                                                        if(playerTeam1.equals(playerTeam2)){
                                                            context.getSource().sendFailure(Component.literal("Cannot set Diplomacy of same Faction!").withStyle(ChatFormatting.RED));
                                                            return 0;
                                                        }

                                                        if(TeamEvents.recruitsDiplomacyManager == null){
                                                            context.getSource().sendFailure(Component.literal("recruitsDiplomacyManager == null!").withStyle(ChatFormatting.RED));
                                                            return 0;
                                                        }

                                                        TeamEvents.recruitsDiplomacyManager.setRelation(playerTeam1.getName(), playerTeam2.getName(), status, context.getSource().getLevel());
                                                        TeamEvents.recruitsDiplomacyManager.setRelation(playerTeam2.getName(), playerTeam1.getName(), status, context.getSource().getLevel());

                                                        context.getSource().sendSuccess(() ->
                                                                Component.literal(playerTeam1.getName() + " and " + playerTeam2.getName() + " are now " + status), false);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
            )
            .then(Commands.literal("debugManager")
                .then(Commands.literal("spawnFromEgg")
                    .then(Commands.argument("Amount", IntegerArgumentType.integer(0))
                        .executes((context) -> {
                            int amount = IntegerArgumentType.getInteger(context, "Amount");
                            ServerPlayer player = context.getSource().getPlayer();
                            ServerLevel serverLevel = context.getSource().getLevel();
                            if(player == null) return 0;

                            ItemStack handItem = player.getMainHandItem();

                            if(handItem.getItem() instanceof RecruitsSpawnEgg recruitsSpawnEgg){
                                BlockPos pos = player.getOnPos();
                                EntityType<?> entitytype = recruitsSpawnEgg.getType(handItem.getTag());
                                List<AbstractRecruitEntity> recruitEntities = new ArrayList<>();

                                for(int i = 0; i < amount; i++){
                                    Entity entity = entitytype.create(serverLevel);
                                    CompoundTag entityTag = handItem.getTag();

                                    if(entity instanceof AbstractRecruitEntity recruit && entityTag != null) {
                                        RecruitsSpawnEgg.fillRecruit(recruit, entityTag, pos);
                                        recruitEntities.add((AbstractRecruitEntity)entity);
                                    }
                                }

                                //FormationUtils.squareFormation(player, recruitEntities, pos.getCenter());

                                for(Entity entity : recruitEntities){
                                    serverLevel.addFreshEntity(entity);
                                }

                            }
                            else{
                                context.getSource().sendFailure(Component.literal("No Spawn Egg found!").withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            return 1;
                        })
                    )
                )
            )
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

    private static int getUnitsCount(ServerPlayer player) {
        if(RecruitEvents.recruitsPlayerUnitManager != null){
            return RecruitEvents.recruitsPlayerUnitManager.getRecruitCount(player.getUUID());
        }

        return 0;
    }

    private static int setUnitsCount(CommandContext<CommandSourceStack> context, ServerPlayer player, int x) {
        if(RecruitEvents.recruitsPlayerUnitManager != null){
            RecruitEvents.recruitsPlayerUnitManager.setRecruitCount(player.getUUID(), x);


            RecruitEvents.recruitsPlayerUnitManager.save(context.getSource().getLevel());
            context.getSource().sendSuccess(() ->
                    Component.literal("The recruits count of " + player.getName().getString() + " has been set to " + x + "."), false);

            return 1;
        }
        return 0;
    }

    private static int setFactionNPCsCount(CommandContext<CommandSourceStack> context, RecruitsTeam faction, int x) {
        if(TeamEvents.recruitsTeamManager != null){
            faction.setNPCs(x);

            TeamEvents.recruitsTeamManager.save(context.getSource().getLevel());
            context.getSource().sendSuccess(() ->
                    Component.literal("The npc count of " + faction.getStringID() + " has been set to " + x + "."), false);

            return 1;
        }
        return 0;
    }
}
