package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.util.ClaimUtil;
import com.talhanation.recruits.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class ClaimEvents {

    public static MinecraftServer server;
    public static RecruitsClaimManager recruitsClaimManager;

    public static int siegeCounter;

    public static int detectionCounter;

    private static final int SIEGE_TICK_INTERVAL = 100;

    private static final int DETECTION_TICK_INTERVAL = 300;

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

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event){
        if(event.getServer().overworld().isClientSide()) return;

        siegeCounter++;
        detectionCounter++;

        ServerLevel level = server.overworld();

        if(siegeCounter >= SIEGE_TICK_INTERVAL){
            siegeCounter = 0;
            tickActiveSieges(level);
        }

        if(detectionCounter >= DETECTION_TICK_INTERVAL){
            detectionCounter = 0;
            tickDetection(level);
        }
    }


    private void tickActiveSieges(ServerLevel level){
        // Kopie, da wir während der Iteration entfernen können
        List<RecruitsClaim> sieges = new ArrayList<>(recruitsClaimManager.getActiveSieges());

        for(RecruitsClaim claim : sieges){
            if(claim == null || claim.getOwnerFaction() == null) continue;

            List<LivingEntity> entities = ClaimUtil.getLivingEntitiesInClaim(level, claim, LivingEntity::isAlive);
            List<LivingEntity> attackers = new ArrayList<>();
            List<LivingEntity> defenders = new ArrayList<>();

            classifyEntities(entities, claim, attackers, defenders);

            int attackerSize = attackers.size();
            int defenderSize = defenders.size();

            updateParties(claim, attackers, defenders);

            if(attackerSize < RecruitsServerConfig.SiegeClaimsRecruitsAmount.get()){
                claim.setUnderSiege(false, level);
                claim.resetHealth();
                claim.setSiegeSpeedPercent(0f);
                claim.attackingParties.clear();
                claim.defendingParties.clear();
                recruitsClaimManager.removeActiveSiege(claim);
                recruitsClaimManager.broadcastClaimsToAll(level);
                siegeOverVillagers(level, claim);
                continue;
            }

            // Siege-Speed prozentual berechnen basierend auf Ratio
            float speedPercent = calculateSiegeSpeedPercent(attackerSize, defenderSize);
            claim.setSiegeSpeedPercent(speedPercent);

            int baseDamage = 3;

            // SiegeEvent.Tick feuern – cancelable, Addons können Damage überschreiben
            com.talhanation.recruits.SiegeEvent.Tick tickEvent = new com.talhanation.recruits.SiegeEvent.Tick(claim, level, attackerSize, defenderSize, baseDamage);
            MinecraftForge.EVENT_BUS.post(tickEvent);

            if(!tickEvent.isCanceled()){
                claim.setHealth(claim.getHealth() - tickEvent.getDamage());
            }

            if(claim.getHealth() <= 0){
                claim.setSiegeSpeedPercent(0f);
                claim.setSiegeSuccess(level);
                recruitsClaimManager.removeActiveSiege(claim);
                recruitsClaimManager.broadcastClaimsToAll(level);
                siegeOverVillagers(level, claim);
                continue;
            }

            // Broadcast an Spieler im Claim
            List<ServerPlayer> players = attackers.stream().filter(e -> e instanceof ServerPlayer).map(e -> (ServerPlayer) e).toList();
            recruitsClaimManager.broadcastClaimUpdateTo(claim, players);
            players = defenders.stream().filter(e -> e instanceof ServerPlayer).map(e -> (ServerPlayer) e).toList();
            recruitsClaimManager.broadcastClaimUpdateTo(claim, players);
        }
    }


    private void tickDetection(ServerLevel level){
        for(RecruitsClaim claim : recruitsClaimManager.getAllClaims()){
            if(claim == null || claim.getOwnerFaction() == null) continue;
            if(claim.isAdmin) continue;

            // Aktive Sieges werden bereits im Siege-Tick behandelt
            if(recruitsClaimManager.isActiveSiege(claim)) continue;

            List<LivingEntity> entities = ClaimUtil.getLivingEntitiesInClaim(level, claim, LivingEntity::isAlive);
            List<LivingEntity> attackers = new ArrayList<>();
            List<LivingEntity> defenders = new ArrayList<>();

            for(LivingEntity livingEntity : entities){
                takeOverVillager(level, claim, livingEntity);
            }

            classifyEntities(entities, claim, attackers, defenders);

            int attackerSize = attackers.size();

            updateParties(claim, attackers, defenders);

            if(attackerSize >= RecruitsServerConfig.SiegeClaimsRecruitsAmount.get()){
                if (RecruitsServerConfig.SiegeRequiresOwnerOnline.get()) {
                    RecruitsPlayerInfo ownerInfo = claim.getPlayerInfo();
                    if (ownerInfo != null) {
                        ServerPlayer onlineOwner = server.getPlayerList().getPlayer(ownerInfo.getUUID());
                        if (onlineOwner == null) {

                            Component msg = Component.translatable(
                                    "chat.recruits.text.siegeBlockedOwnerOffline",
                                    claim.getName(),
                                    ownerInfo.getName()
                            ).withStyle(net.minecraft.ChatFormatting.RED);
                            for (LivingEntity attacker : attackers) {
                                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(msg);
                            }
                            continue;
                        }
                    }
                }
                claim.setUnderSiege(true, level);
                recruitsClaimManager.addActiveSiege(claim);
                recruitsClaimManager.broadcastClaimsToAll(level);
                sendVillagersHome(level, claim);
            }
        }
    }

    private void classifyEntities(List<LivingEntity> entities, RecruitsClaim claim, List<LivingEntity> attackers, List<LivingEntity> defenders){
        for(LivingEntity livingEntity : entities){
            if(!livingEntity.isAlive() || livingEntity.getTeam() == null) continue;

            String teamName = livingEntity.getTeam().getName();
            RecruitsFaction recruitsFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(teamName);

            if(recruitsFaction == null) continue;

            RecruitsDiplomacyManager.DiplomacyStatus relation = FactionEvents.recruitsDiplomacyManager.getRelation(teamName, claim.getOwnerFactionStringID());

            if (recruitsFaction.getStringID().equals(claim.getOwnerFactionStringID()) || relation == RecruitsDiplomacyManager.DiplomacyStatus.ALLY) {
                defenders.add(livingEntity);
            }

            else if (!recruitsFaction.getStringID().equals(claim.getOwnerFactionStringID()) && relation == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY){
                attackers.add(livingEntity);
            }
        }
    }

    /**
     * Fügt die Fraktionen der Angreifer/Verteidiger als Parteien zum Claim hinzu.
     */
    private void updateParties(RecruitsClaim claim, List<LivingEntity> attackers, List<LivingEntity> defenders){
        for(LivingEntity livingEntity : defenders){
            if(livingEntity.getTeam() == null) continue;
            RecruitsFaction recruitsFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(livingEntity.getTeam().getName());
            if(recruitsFaction == null) continue;
            if(!claim.getOwnerFaction().equalsFaction(recruitsFaction)) claim.addParty(claim.defendingParties, recruitsFaction);
        }

        for(LivingEntity livingEntity : attackers){
            if(livingEntity.getTeam() == null) continue;
            RecruitsFaction recruitsFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(livingEntity.getTeam().getName());
            if(recruitsFaction == null) continue;
            claim.addParty(claim.attackingParties, recruitsFaction);
        }
    }

    public static float calculateSiegeSpeedPercent(int attackerCount, int defenderCount) {
        if (attackerCount <= 0) return 0f;
        if (defenderCount <= 0) return 2.0f; // Keine Verteidiger = maximaler Speed
        return (float) attackerCount / (float) defenderCount;
    }

    private void takeOverVillager(ServerLevel level, RecruitsClaim claim, LivingEntity livingEntity) {
        if(!(livingEntity instanceof Villager || livingEntity instanceof VillagerNobleEntity)) return;
        if(!livingEntity.isAlive()) return;

        String teamName = claim.getOwnerFaction().getStringID();
        PlayerTeam team = level.getScoreboard().getPlayerTeam(teamName);
        if(team == null) return;

        if(livingEntity.getTeam() == null || !livingEntity.getTeam().getName().equals(team.getName())) level.getScoreboard().addPlayerToTeam(livingEntity.getStringUUID(), team);

        if(livingEntity instanceof VillagerNobleEntity noble){
            noble.updateTeam();
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
    public void onExplosion(ExplosionEvent event) {
        if(event.getLevel().isClientSide()) return;
        Vec3 vec = event.getExplosion().getPosition();
        BlockPos pos = new BlockPos((int) vec.x, (int) vec.y, (int) vec.z);
        ChunkAccess access = server.overworld().getChunk(pos);
        RecruitsClaim claim = recruitsClaimManager.getClaim(access.getPos());

        Entity entity = event.getExplosion().getDirectSourceEntity();
        if(entity instanceof Player player && player.isCreative() && player.hasPermissions(2)){
            return;
        }

        if(claim != null && RecruitsServerConfig.ExplosionProtectionInClaims.get()){
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public void onBucketInteract(FillBucketEvent event) {
        if(event.getLevel().isClientSide()) return;
        if(event.getTarget() == null) return;

        Vec3 vec = event.getTarget().getLocation();
        BlockPos pos = new BlockPos((int) vec.x, (int) vec.y, (int) vec.z);

        ChunkAccess access = server.overworld().getChunk(pos);
        RecruitsClaim claim = recruitsClaimManager.getClaim(access.getPos());
        if(claim == null) return;

        Player player = event.getEntity();

        if(player.isCreative() && player.hasPermissions(2)){
            return;
        }

        boolean isInTeam = player.getTeam() != null && player.getTeam().getName().equals(claim.getOwnerFactionStringID());
        if(!isInTeam) event.setCanceled(true);
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

        if(claim.isBlockInteractionAllowed()) return;

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
                || (blockEntity instanceof Container)
        )
        {
            {
                boolean isInTeam = player.getTeam() != null && player.getTeam().getName().equals(claim.getOwnerFactionStringID());
                if(!isInTeam) event.setCanceled(true);
            }
        }
    }

    public static void sendVillagersHome(ServerLevel level, RecruitsClaim claim) {
        List<LivingEntity> list = ClaimUtil.getLivingEntitiesInClaim(level, claim, livingEntity -> livingEntity instanceof Villager);

        for (LivingEntity living : list) {
            if(living instanceof Villager villager){
                Brain<?> brain = villager.getBrain();
                brain.setActiveActivityIfPossible(Activity.HIDE);
                brain.setMemory(MemoryModuleType.IS_PANICKING, true);
                brain.setMemory(MemoryModuleType.HEARD_BELL_TIME, level.getGameTime());
            }
        }
    }

    public static void siegeOverVillagers(ServerLevel level, RecruitsClaim claim) {
        List<LivingEntity> list = ClaimUtil.getLivingEntitiesInClaim(level, claim, livingEntity -> livingEntity instanceof Villager);

        for (LivingEntity living : list) {
            if(living instanceof Villager villager){
                Brain<?> brain = villager.getBrain();
                brain.setActiveActivityIfPossible(Activity.MEET);
                brain.eraseMemory(MemoryModuleType.IS_PANICKING);
                brain.eraseMemory(MemoryModuleType.HEARD_BELL_TIME);
            }
        }
    }

}
