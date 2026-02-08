package com.talhanation.recruits.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.items.RecruitsSpawnEgg;
import com.talhanation.recruits.world.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
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
                                    RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(playerTeam.getName());
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
                                            RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(playerTeam.getName());
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
                                    RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(playerTeam.getName());
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
                                RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(playerTeam.getName());

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

                                if(FactionEvents.isPlayerAlreadyAFactionLeader(player)){
                                    context.getSource().sendFailure(Component.literal("Player is already a Leader of another Faction!").withStyle(ChatFormatting.RED));
                                    return 0;
                                }

                                if(!playerTeam.getPlayers().contains(playerName)){
                                    FactionEvents.addPlayerToTeam(null, context.getSource().getLevel(), faction.getStringID(), playerName);
                                }

                                faction.setTeamLeaderID(player.getUUID());
                                faction.setTeamLeaderName(player.getName().getString());

                                FactionEvents.modifyTeam(context.getSource().getLevel(), faction.getStringID(), faction, context.getSource().getPlayer(), 0);

                                FactionEvents.recruitsFactionManager.save(context.getSource().getLevel());

                                context.getSource().sendSuccess(() ->
                                        Component.literal("The Leader of " + faction.getTeamDisplayName() + " is now " + faction.getTeamLeaderName()), false);
                                return 1;
                            })))
                )
                .then(Commands.literal("delete")
                        .then(Commands.argument("Faction", StringArgumentType.greedyString())
                                .executes((context) -> {
                                    String userInput = StringArgumentType.getString(context, "Faction");
                                    RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(userInput);

                                    if(faction == null) {
                                        context.getSource().sendFailure(Component.literal("No Faction found!").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    FactionEvents.recruitsFactionManager.removeTeam(faction.getStringID());
                                    FactionEvents.recruitsFactionManager.save(context.getSource().getLevel());

                                    try{
                                        PlayerTeam team = context.getSource().getLevel().getScoreboard().getPlayerTeam(userInput);
                                        if(team != null) context.getSource().getLevel().getScoreboard().removePlayerTeam(team);
                                    }
                                    catch (Exception ignored){

                                    }

                                    context.getSource().sendSuccess(() ->
                                            Component.literal("Faction (" + userInput + ") was deleted"), false);
                                    return 1;
                                }))
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

                                                        if(FactionEvents.recruitsDiplomacyManager == null){
                                                            context.getSource().sendFailure(Component.literal("recruitsDiplomacyManager == null!").withStyle(ChatFormatting.RED));
                                                            return 0;
                                                        }

                                                        FactionEvents.recruitsDiplomacyManager.setRelation(playerTeam1.getName(), playerTeam2.getName(), status, context.getSource().getLevel());
                                                        FactionEvents.recruitsDiplomacyManager.setRelation(playerTeam2.getName(), playerTeam1.getName(), status, context.getSource().getLevel());

                                                        context.getSource().sendSuccess(() ->
                                                                Component.literal(playerTeam1.getName() + " and " + playerTeam2.getName() + " are now " + status), false);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
            )
            .then(Commands.literal("claimManager")
                .then(Commands.literal("getClaimAtPosition")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            ChunkPos chunkPos = player.chunkPosition();
                            RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClaimEvents.recruitsClaimManager.getAllClaims().stream().toList());

                            if (claim == null) {
                                ctx.getSource().sendFailure(Component.literal("No claim found at your position."));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Claim: [" + claim + "]"), false);

                            return 1;
                        })

                )
                .then(Commands.literal("setAdminChunk")
                        .then(Commands.argument("isAdmin", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean isAdmin = BoolArgumentType.getBool(ctx, "isAdmin");

                                    ChunkPos chunkPos = player.chunkPosition();
                                    RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClaimEvents.recruitsClaimManager.getAllClaims().stream().toList());

                                    if (claim == null) {
                                        ctx.getSource().sendFailure(Component.literal("No claim found at your position."));
                                        return 0;
                                    }

                                    claim.setAdminClaim(isAdmin);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Claim [" + claim + "] is now set to admin = " + isAdmin), false);
                                    ClaimEvents.recruitsClaimManager.broadcastClaimsToAll(ctx.getSource().getLevel());
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("setHealth")
                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                            ChunkPos chunkPos = player.chunkPosition();
                            RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClaimEvents.recruitsClaimManager.getAllClaims().stream().toList());

                            if (claim == null) {
                                ctx.getSource().sendFailure(Component.literal("No claim found at your position."));
                                return 0;
                            }

                            claim.setHealth(amount);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Claim health was set to " + claim.getHealth()), false);
                            ClaimEvents.recruitsClaimManager.broadcastClaimsToAll(ctx.getSource().getLevel());
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("getHealth")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            ChunkPos chunkPos = player.chunkPosition();
                            RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClaimEvents.recruitsClaimManager.getAllClaims().stream().toList());

                            if (claim == null) {
                                ctx.getSource().sendFailure(Component.literal("No claim found at your position."));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal(claim + " has " + claim.getHealth() + "health"), false);

                            return 1;
                        })
                )
                .then(Commands.literal("setSiege")
                        .then(Commands.argument("siege", BoolArgumentType.bool())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                boolean siege = BoolArgumentType.getBool(ctx, "siege");

                                ChunkPos chunkPos = player.chunkPosition();
                                RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClaimEvents.recruitsClaimManager.getAllClaims().stream().toList());

                                if (claim == null) {
                                    ctx.getSource().sendFailure(Component.literal("No claim found at your position."));
                                    return 0;
                                }

                                claim.setAdminClaim(siege);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("Claim [" + claim + "] is setSiege= " + siege), false);
                                ClaimEvents.recruitsClaimManager.broadcastClaimsToAll(ctx.getSource().getLevel());
                                return 1;
                            })
                        )
                )
                .then(Commands.literal("deleteClaim")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            ChunkPos chunkPos = player.chunkPosition();
                            RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClaimEvents.recruitsClaimManager.getAllClaims().stream().toList());

                            if (claim == null) {
                                ctx.getSource().sendFailure(Component.literal("No claim found at your position."));
                                return 0;
                            }

                            ClaimEvents.recruitsClaimManager.removeClaim(claim);

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Claim [" + claim + "] is now deleted."), false);
                            ClaimEvents.recruitsClaimManager.broadcastClaimsToAll(ctx.getSource().getLevel());
                            return 1;
                        })

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
            .then(Commands.literal("nobleVillagerManager")
                .then(Commands.literal("addNobleTrade")
                    .then(Commands.argument("Resource", ResourceLocationArgument.id())
                        .then(Commands.argument("MaxUses", IntegerArgumentType.integer())
                            .then(Commands.argument("VillagerNoble", EntityArgument.entity())
                                .executes((context) -> {
                                    Entity entity = EntityArgument.getEntity(context, "VillagerNoble");
                                    if(entity instanceof VillagerNobleEntity nobleVillager){
                                        ResourceLocation resourceLocation = ResourceLocationArgument.getId(context, "Resource");

                                        RecruitsHireTrade hireTrade = RecruitsHireTradesRegistry.getByResourceLocation(resourceLocation);

                                        if(hireTrade == null) {
                                            context.getSource().sendFailure(Component.literal("No Trade for " + resourceLocation + " found!"));
                                            return 0;
                                        }
                                        if(nobleVillager.hasTrade(resourceLocation)){
                                            nobleVillager.removeTrade(resourceLocation);
                                        }

                                        int maxUses = IntegerArgumentType.getInteger(context, "MaxUses");
                                        hireTrade.maxUses = maxUses;
                                        hireTrade.uses = maxUses;
                                        nobleVillager.addTrade(hireTrade);
                                    }
                                    else{
                                        context.getSource().sendFailure(Component.literal("Not a Noble Villager."));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal("Trade added!"), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                .then(Commands.literal("refreshAllTrades")
                        .then(Commands.argument("VillagerNoble", EntityArgument.entity())
                                .executes((context) -> {
                                    Entity entity = EntityArgument.getEntity(context, "VillagerNoble");
                                    if(entity instanceof VillagerNobleEntity nobleVillager){
                                        List<RecruitsHireTrade> list = nobleVillager.getTrades();

                                        for (RecruitsHireTrade trade : list) {
                                             trade.uses = trade.maxUses;
                                        }

                                        nobleVillager.setTrades(list);
                                    }
                                    else{
                                        context.getSource().sendFailure(Component.literal("Not a Noble Villager."));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal("Trades refreshed!"), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("levelup")
                        .then(Commands.argument("VillagerNoble", EntityArgument.entity())
                                .executes((context) -> {
                                    Entity entity = EntityArgument.getEntity(context, "VillagerNoble");
                                    if(entity instanceof VillagerNobleEntity nobleVillager){
                                        nobleVillager.addTraderProgress(100);
                                    }
                                    else{
                                        context.getSource().sendFailure(Component.literal("Not a Noble Villager."));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal("Leveled up!"), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("removeNobleTrade")
                    .then(Commands.argument("Resource", ResourceLocationArgument.id())
                        .then(Commands.argument("VillagerNoble", EntityArgument.entity())
                            .executes((context) -> {
                                Entity entity = EntityArgument.getEntity(context, "VillagerNoble");

                                if(entity instanceof VillagerNobleEntity nobleVillager){
                                    ResourceLocation resourceLocation = ResourceLocationArgument.getId(context, "Resource");

                                    if(nobleVillager.hasTrade(resourceLocation)){
                                        nobleVillager.removeTrade(resourceLocation);
                                        context.getSource().sendSuccess(() -> Component.literal("Trade was removed!"), false);
                                    }
                                    else {
                                        context.getSource().sendFailure(Component.literal("No Trade for " + resourceLocation + " found!"));
                                        return 0;
                                    }
                                }
                                else{
                                    context.getSource().sendFailure(Component.literal("Not a Noble Villager."));
                                    return 0;
                                }
                                return 1;
                            })
                        )
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
            RecruitEvents.recruitsPlayerUnitManager.setRecruitCount(player, x);


            RecruitEvents.recruitsPlayerUnitManager.save(context.getSource().getLevel());
            context.getSource().sendSuccess(() ->
                    Component.literal("The recruits count of " + player.getName().getString() + " has been set to " + x + "."), false);

            return 1;
        }
        return 0;
    }

    private static int setFactionNPCsCount(CommandContext<CommandSourceStack> context, RecruitsFaction faction, int x) {
        if(FactionEvents.recruitsFactionManager != null){
            faction.setNPCs(x);

            FactionEvents.recruitsFactionManager.save(context.getSource().getLevel());
            context.getSource().sendSuccess(() ->
                    Component.literal("The npc count of " + faction.getStringID() + " has been set to " + x + "."), false);

            return 1;
        }
        return 0;
    }
}
