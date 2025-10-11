package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.util.ClaimUtil;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsClaimManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class ClaimEvents {

    public static MinecraftServer server;
    public static RecruitsClaimManager recruitsClaimManager;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        ServerLevel level = server.overworld();

        recruitsClaimManager = new RecruitsClaimManager();
        recruitsClaimManager.load(level);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recruitsClaimManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event){
        recruitsClaimManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide()) return;

        if(event.getEntity() instanceof Player){
            recruitsClaimManager.broadcastClaimsToAll(server.overworld());
        }
    }

    public static int counter;
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event){
        if(event.getServer().overworld().isClientSide()) return;
        if(counter++ < 100) return;

        counter = 0;
        for(RecruitsClaim claim : recruitsClaimManager.getAllClaims()){
            ServerLevel level = server.overworld();
            takeOverVillager(level, claim);
            if (claim == null) return;
            if (claim.isAdmin) return;

            List<LivingEntity> attackers = ClaimUtil.getLivingEntitiesInClaim(level, claim,
                    livingEntity -> livingEntity.isAlive() && livingEntity.getTeam() != null
                            && FactionEvents.recruitsDiplomacyManager.getRelation(livingEntity.getTeam().getName(), claim.getOwnerFactionStringID()) == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY);
            List<LivingEntity> defenders = ClaimUtil.getLivingEntitiesInClaim(level, claim,
                    livingEntity -> livingEntity.isAlive() && livingEntity.getTeam() != null
                            && (livingEntity.getTeam().getName().equals(claim.getOwnerFactionStringID()) || FactionEvents.recruitsDiplomacyManager.getRelation(livingEntity.getTeam().getName(), claim.getOwnerFactionStringID()) == RecruitsDiplomacyManager.DiplomacyStatus.ALLY));

            int attackerSize = attackers.size();
            int defendersSize = defenders.size();

            for(LivingEntity livingEntity : defenders){
                if(livingEntity.getTeam() == null) continue;
                RecruitsFaction recruitsFaction = FactionEvents.recruitsFactionManager.getTeamByStringID(livingEntity.getTeam().getName());
                if(!claim.defendingParties.contains(recruitsFaction) && claim.getOwnerFaction() != recruitsFaction) claim.defendingParties.add(recruitsFaction);
            }

            for(LivingEntity livingEntity : attackers){
                if(livingEntity.getTeam() == null) continue;
                RecruitsFaction recruitsFaction = FactionEvents.recruitsFactionManager.getTeamByStringID(livingEntity.getTeam().getName());
                if(!claim.attackingParties.contains(recruitsFaction)) claim.attackingParties.add(recruitsFaction);
            }

            if(claim.isUnderSiege){
                if(attackerSize < RecruitsServerConfig.SiegeClaimsRecruitsAmount.get()){//Siege FAIL
                    claim.setUnderSiege(false, level);
                    claim.resetHealth();
                    claim.attackingParties.clear();
                    claim.defendingParties.clear();
                    recruitsClaimManager.broadcastClaimsToAll(level);
                    return;
                }
                else{
                    claim.setHealth(claim.getHealth() - 3);

                    if(claim.getHealth() <= 0){//Siege SUCCESS
                        claim.setSiegeSuccess(level);
                        recruitsClaimManager.broadcastClaimsToAll(level);
                        return;
                    }
                    List<ServerPlayer> players = attackers.stream().filter(livingEntity -> livingEntity instanceof ServerPlayer).map(e -> (ServerPlayer) e).toList();
                    recruitsClaimManager.broadcastClaimUpdateTo(claim, players);
                    players = defenders.stream().filter(livingEntity -> livingEntity instanceof ServerPlayer).map(e -> (ServerPlayer) e).toList();
                    recruitsClaimManager.broadcastClaimUpdateTo(claim, players);
                }
            }
            //initial
            else if(attackerSize >= RecruitsServerConfig.SiegeClaimsRecruitsAmount.get()){
                claim.setUnderSiege( true, level);
                recruitsClaimManager.broadcastClaimsToAll(level);
            }
        }
    }

    private void takeOverVillager(ServerLevel level, RecruitsClaim claim) {
        List<LivingEntity> list = ClaimUtil.getLivingEntitiesInClaim(level, claim,
                livingEntity -> (livingEntity instanceof Villager || livingEntity instanceof VillagerNobleEntity) && livingEntity.isAlive());
        if(list.isEmpty()) return;

        String teamName = claim.getOwnerFaction().getStringID();
        PlayerTeam team = level.getScoreboard().getPlayerTeam(teamName);
        if(team == null) return;

        for(LivingEntity living : list){
            if(living.getTeam() == null || !living.getTeam().getName().equals(team.getName())) level.getScoreboard().addPlayerToTeam(living.getStringUUID(), team);
        }
    }

    public static List<AbstractRecruitEntity> getRecruitsOfTeamInRange(Level level, Player attackingPlayer, double radius, String teamId) {

        return level.getEntitiesOfClass(AbstractRecruitEntity.class, attackingPlayer.getBoundingBox().inflate(radius)).stream()
                .filter(recruit -> recruit.isAlive() && recruit.getTeam() != null && teamId.equals(recruit.getTeam().getName()))
                .toList();
    }
    @SubscribeEvent
    public void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        if(event.getLevel().isClientSide()) return;

        ChunkAccess access = server.overworld().getChunk(event.getPos());
        RecruitsClaim claim = recruitsClaimManager.getClaim(access.getPos());
        Player player = event.getPlayer();

        if(player.isCreative() && player.hasPermissions(2)){
            return;
        }

        if(claim == null) {
            if(RecruitsServerConfig.BlockPlacingBreakingOnlyWhenClaimed.get()){
                event.setCanceled(true);
            }
            return;
        }
        if(!claim.isBlockBreakingAllowed()){
            boolean isInTeam = player.getTeam() != null && player.getTeam().getName().equals(claim.getOwnerFactionStringID());
            if(!isInTeam) event.setCanceled(true);
        }

    }

    @SubscribeEvent
    public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        if(event.getLevel().isClientSide()) return;

        ChunkAccess access = server.overworld().getChunk(event.getPos());
        RecruitsClaim claim = recruitsClaimManager.getClaim(access.getPos());
        Entity entity = event.getEntity();

        if(entity instanceof Player player && player.isCreative() && player.hasPermissions(2)){
            return;
        }

        if(claim == null) {
            if(RecruitsServerConfig.BlockPlacingBreakingOnlyWhenClaimed.get()){
                event.setCanceled(true);
            }
            return;
        }
        if(!claim.isBlockPlacementAllowed()){
            boolean isInTeam = entity instanceof LivingEntity livingEntity && livingEntity.getTeam() != null && livingEntity.getTeam().getName().equals(claim.getOwnerFactionStringID());
            if(!isInTeam) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if(event.getLevel().isClientSide()) return;
        ChunkAccess access = server.overworld().getChunk(event.getPos());
        RecruitsClaim claim = recruitsClaimManager.getClaim(access.getPos());
        if(claim == null) return;

        BlockPos pos = event.getHitVec().getBlockPos();
        Player player = event.getEntity();

        if(player.isCreative() && player.hasPermissions(2)){
            return;
        }

        BlockState selectedBlock = player.getCommandSenderWorld().getBlockState(pos);
        BlockEntity blockEntity = player.getCommandSenderWorld().getBlockEntity(pos);

        if (selectedBlock.is(BlockTags.BUTTONS)
                || selectedBlock.is(BlockTags.DOORS)
                || selectedBlock.is(BlockTags.WOODEN_TRAPDOORS)
                || selectedBlock.is(BlockTags.WOODEN_BUTTONS)
                || selectedBlock.is(BlockTags.WOODEN_DOORS)
                || selectedBlock.is(BlockTags.SHULKER_BOXES)
                || selectedBlock.is(BlockTags.FENCE_GATES)
                || selectedBlock.is(BlockTags.ANVIL)
                || (selectedBlock.getBlock() instanceof LeverBlock)
                || (selectedBlock.getBlock() instanceof DiodeBlock)
                || (selectedBlock.getBlock() instanceof DaylightDetectorBlock)
                || (blockEntity instanceof Container))
        {
            if(!claim.isBlockInteractionAllowed()){
                boolean isInTeam = player.getTeam() != null && player.getTeam().getName().equals(claim.getOwnerFactionStringID());
                if(!isInTeam) event.setCanceled(true);
            }
        }
    }

}
