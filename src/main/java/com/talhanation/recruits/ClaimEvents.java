package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.util.ClaimUtil;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsClaimManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.TickEvent;
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
        /*
        RecruitsClaim recruitsClaim = new RecruitsClaim("RedLands", TeamEvents.recruitsTeamManager.getTeamByStringID("reds"));
        recruitsClaim.setBlockBreakingAllowed(false);
        recruitsClaim.setBlockInteractionAllowed(false);
        recruitsClaim.setBlockPlacementAllowed(false);
        recruitsClaim.addChunk(new ChunkPos(10, 10));
        recruitsClaim.addChunk(new ChunkPos(10, 11));
        recruitsClaim.addChunk(new ChunkPos(10, -11));
        recruitsClaim.addChunk(new ChunkPos(11, 10));
        recruitsClaim.addChunk(new ChunkPos(11, 11));
        recruitsClaim.addChunk(new ChunkPos(11, -11));
        recruitsClaim.addChunk(new ChunkPos(-11, 10));
        recruitsClaim.addChunk(new ChunkPos(-11, 11));
        recruitsClaim.addChunk(new ChunkPos(-11, -11));

        recruitsClaimManager.addOrUpdateClaim(recruitsClaim);

        RecruitsClaim recruitsClaim1 = new RecruitsClaim("BlueLands", TeamEvents.recruitsTeamManager.getTeamByStringID("blues"));
        recruitsClaim1.setBlockBreakingAllowed(false);
        recruitsClaim1.setBlockInteractionAllowed(false);
        recruitsClaim1.setBlockPlacementAllowed(false);
        recruitsClaim1.addChunk(new ChunkPos(0, 0));
        recruitsClaim1.addChunk(new ChunkPos(0, 1));
        recruitsClaim1.addChunk(new ChunkPos(0, -1));
        recruitsClaim1.addChunk(new ChunkPos(1, 0));
        recruitsClaim1.addChunk(new ChunkPos(1, 1));
        recruitsClaim1.addChunk(new ChunkPos(1, -1));
        recruitsClaim1.addChunk(new ChunkPos(-1, 0));
        recruitsClaim1.addChunk(new ChunkPos(-1, 1));
        recruitsClaim1.addChunk(new ChunkPos(-1, -1));

        recruitsClaimManager.addOrUpdateClaim(recruitsClaim1);
        */

    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recruitsClaimManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event){
        recruitsClaimManager.save(server.overworld());
    }
    public static int counter;
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event){
        if(counter++ < 100) return;

        counter = 0;
        for(RecruitsClaim claim : recruitsClaimManager.getAllClaims()){
            ServerLevel level = server.overworld();
            if (claim == null) return;
            if(claim.isAdmin) return;

            List<LivingEntity> attackers = ClaimUtil.getLivingEntitiesInClaim(level, claim,
                    livingEntity -> livingEntity.isAlive() && livingEntity.getTeam() != null
                            && TeamEvents.recruitsDiplomacyManager.getRelation(livingEntity.getTeam().getName(), claim.getOwnerFactionStringID()) == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY);
            List<LivingEntity> defenders = ClaimUtil.getLivingEntitiesInClaim(level, claim,
                    livingEntity -> livingEntity.isAlive() && livingEntity.getTeam() != null
                            && TeamEvents.recruitsDiplomacyManager.getRelation(livingEntity.getTeam().getName(), claim.getOwnerFactionStringID()) == RecruitsDiplomacyManager.DiplomacyStatus.ALLY);

            int attackerSize = attackers.size();
            int defendersSize = defenders.size();

            for(LivingEntity livingEntity : defenders){
                if(livingEntity.getTeam() == null) continue;
                RecruitsTeam recruitsTeam = TeamEvents.recruitsTeamManager.getTeamByStringID(livingEntity.getTeam().getName());
                if(!claim.defendingParties.contains(recruitsTeam) && claim.getOwnerFaction() != recruitsTeam) claim.defendingParties.add(recruitsTeam);
            }

            for(LivingEntity livingEntity : attackers){
                if(livingEntity.getTeam() == null) continue;
                RecruitsTeam recruitsTeam = TeamEvents.recruitsTeamManager.getTeamByStringID(livingEntity.getTeam().getName());
                if(!claim.attackingParties.contains(recruitsTeam)) claim.attackingParties.add(recruitsTeam);
            }

            if(claim.isUnderSiege){
                if(attackerSize < RecruitsServerConfig.SiegingClaimsRecruitsAmount.get()){//Siege FAIL
                    claim.setUnderSiege(false, level);
                    claim.resetHealth();
                    claim.attackingParties.clear();
                    claim.defendingParties.clear();
                    return;
                }
                else{
                    claim.setHealth(claim.getHealth() - 3);

                    if(claim.getHealth() <= 0){//Siege SUCCESS
                        claim.setSiegeSuccsess(level);
                        return;
                    }
                }
            }
            //initial
            else if(defendersSize < attackerSize && attackerSize >= RecruitsServerConfig.SiegingClaimsRecruitsAmount.get()){
                claim.setUnderSiege( true, level);
            }
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

        if(claim == null) return;
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

        if(claim == null) return;
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

        BlockState selectedBlock = player.getCommandSenderWorld().getBlockState(pos);
        BlockEntity blockEntity = player.getCommandSenderWorld().getBlockEntity(pos);

        if (selectedBlock.is(BlockTags.BUTTONS) || selectedBlock.is(BlockTags.DOORS) || selectedBlock.is(BlockTags.WOODEN_TRAPDOORS) || selectedBlock.is(BlockTags.WOODEN_BUTTONS) ||
                selectedBlock.is(BlockTags.WOODEN_DOORS) || selectedBlock.is(BlockTags.SHULKER_BOXES) || selectedBlock.is(BlockTags.FENCE_GATES) || selectedBlock.is(BlockTags.ANVIL) ||
                (blockEntity instanceof Container))
        {
            if(!claim.isBlockInteractionAllowed()){
                boolean isInTeam = player.getTeam() != null && player.getTeam().getName().equals(claim.getOwnerFactionStringID());
                if(!isInTeam) event.setCanceled(true);
            }
        }
    }

}
