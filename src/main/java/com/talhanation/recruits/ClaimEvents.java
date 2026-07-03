package com.talhanation.recruits;
import de.maxhenkel.corelib.net.NetUtils;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.network.MessageToClientWorldMapIdentity;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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

        if(event.getEntity() instanceof ServerPlayer player){
            ServerLevel overworld = player.getServer().overworld();
            NetUtils.sendTo(player, new MessageToClientWorldMapIdentity(RecruitsWorldSaveData.get(overworld).getWorldId()));
            recruitsClaimManager.sendClaimsTo(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event){
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

            if(attackerSize < RecruitsServerConfig.SiegeClaimsRecruitsAmount.get()){
                // Siege aborts: notify the parties that were besieging BEFORE clearing them.
                // (setUnderSiege(false) -> notifyAttackersSiegeFailed reads the still-populated list.)
                claim.setUnderSiege(false, level);
                claim.resetHealth();
                claim.setSiegeSpeedPercent(0f);
                claim.attackingParties.clear();
                claim.defendingParties.clear();
                recruitsClaimManager.removeActiveSiege(claim);
                recruitsClaimManager.broadcastClaimUpdateToAll(level, claim);
                siegeOverVillagers(level, claim);
                continue;
            }

            // Siege continues: rebuild parties from who is currently present (clears stale entries
            // and orders attackers by present unit count, so get(0) is the dominant attacker).
            updateParties(claim, attackers, defenders);

            // Siege-Speed prozentual berechnen basierend auf Ratio
            float speedPercent = calculateSiegeSpeedPercent(attackerSize, defenderSize);
            claim.setSiegeSpeedPercent(speedPercent);

            int baseDamage = 3;

            // SiegeEvent.Tick feuern – cancelable, Addons können Damage überschreiben
            com.talhanation.recruits.SiegeEvent.Tick tickEvent = new com.talhanation.recruits.SiegeEvent.Tick(claim, level, attackerSize, defenderSize, baseDamage);
            NeoForge.EVENT_BUS.post(tickEvent);

            if(!tickEvent.isCanceled()){
                claim.setHealth(claim.getHealth() - tickEvent.getDamage());
            }

            if(claim.getHealth() <= 0){
                claim.setSiegeSpeedPercent(0f);
                claim.setSiegeSuccess(level);
                recruitsClaimManager.removeActiveSiege(claim);
                recruitsClaimManager.broadcastClaimUpdateToAll(level, claim);
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
                // (2) Build parties from who is present RIGHT NOW, immediately before the siege
                // starts, so the bossbar/overlay is correct on the very first entry. Doing this
                // only here (instead of every tick for every claim) also avoids the stale buildup
                // that previously let a passing third faction end up as attackingParties.get(0).
                updateParties(claim, attackers, defenders);

                claim.setUnderSiege(true, level);
                recruitsClaimManager.addActiveSiege(claim);
                recruitsClaimManager.broadcastClaimUpdateToAll(level, claim);
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
     * Rebuilds the attacking/defending party lists for the claim from the entities that are
     * CURRENTLY inside it.
     *
     * <p>Behaviour:
     * <ul>
     *   <li>Both lists are cleared first, so factions that merely passed through earlier (or a
     *       previous siege left behind) can no longer linger as stale entries.</li>
     *   <li>The MAIN attacker ({@code attackingParties.get(0)} - what the bossbar shows and what
     *       conquers on success) is <b>stable</b>: the faction that holds it keeps it as long as it
     *       is still present and still at/above the siege threshold. A faction that joins later
     *       with more units does NOT take over the siege ("no siege stealing").</li>
     *   <li>The main attacker only changes when the current holder drops below the threshold (or
     *       leaves): then the strongest faction that is itself at/above the threshold takes over;
     *       if none qualifies, the strongest present attacker is used.</li>
     *   <li>Remaining attacker factions are listed after the main one, ordered by present unit
     *       count (for the secondary banner row).</li>
     * </ul>
     */
    private void updateParties(RecruitsClaim claim, List<LivingEntity> attackers, List<LivingEntity> defenders){
        int threshold = RecruitsServerConfig.SiegeClaimsRecruitsAmount.get();

        // remember who was the main attacker BEFORE we clear, so we can keep them if still valid
        String previousMainId = (claim.attackingParties != null && !claim.attackingParties.isEmpty()
                && claim.attackingParties.get(0) != null)
                ? claim.attackingParties.get(0).getStringID()
                : null;

        claim.attackingParties.clear();
        claim.defendingParties.clear();

        // defenders: presence is enough, order is irrelevant for them
        for(LivingEntity livingEntity : defenders){
            if(livingEntity.getTeam() == null) continue;
            RecruitsFaction recruitsFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(livingEntity.getTeam().getName());
            if(recruitsFaction == null) continue;
            if(!claim.getOwnerFaction().equalsFaction(recruitsFaction)) claim.addParty(claim.defendingParties, recruitsFaction);
        }

        // count present units per attacking faction (keyed by stringID; RecruitsFaction has no
        // equals/hashCode so the object itself is not a safe map key)
        java.util.Map<String, Integer> attackerCounts = new java.util.LinkedHashMap<>();
        java.util.Map<String, RecruitsFaction> attackerFactions = new java.util.LinkedHashMap<>();
        for(LivingEntity livingEntity : attackers){
            if(livingEntity.getTeam() == null) continue;
            RecruitsFaction recruitsFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(livingEntity.getTeam().getName());
            if(recruitsFaction == null) continue;
            String id = recruitsFaction.getStringID();
            attackerCounts.merge(id, 1, Integer::sum);
            attackerFactions.putIfAbsent(id, recruitsFaction);
        }

        if(attackerCounts.isEmpty()) return;

        // factions ordered by present unit count, strongest first
        List<String> orderedIds = attackerCounts.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());

        // decide the main attacker (strict anti-steal interpretation)
        String mainId;
        boolean previousStillPresent = previousMainId != null && attackerCounts.containsKey(previousMainId);

        if(previousStillPresent && attackerCounts.get(previousMainId) >= threshold){
            // holder still present AND still above threshold -> keep it (no stealing)
            mainId = previousMainId;
        } else {
            // holder dropped below threshold or is gone. The title only moves to a faction that is
            // ITSELF at/above the threshold ("...only if faction B is above the threshold at that
            // time"). If no one qualifies:
            //   - keep the previous holder if it is at least still present (it merely shrank), so a
            //     weaker late-joiner can't steal a siege just because the initiator thinned out;
            //   - only if the previous holder is entirely gone do we fall back to the strongest
            //     present attacker (otherwise there would be no main attacker at all).
            String strongestAboveThreshold = orderedIds.stream()
                    .filter(id -> attackerCounts.get(id) >= threshold)
                    .findFirst()
                    .orElse(null);

            if(strongestAboveThreshold != null){
                mainId = strongestAboveThreshold;
            } else if(previousStillPresent){
                mainId = previousMainId;
            } else {
                mainId = orderedIds.get(0);
            }
        }

        // add main attacker first, then the rest in strength order
        claim.addParty(claim.attackingParties, attackerFactions.get(mainId));
        for(String id : orderedIds){
            if(id.equals(mainId)) continue;
            claim.addParty(claim.attackingParties, attackerFactions.get(id));
        }
    }

    public static float calculateSiegeSpeedPercent(int attackerCount, int defenderCount) {
        if (attackerCount <= 0) return 0f;
        if (defenderCount <= 0) return 2.0f; // Keine Verteidiger = maximaler Speed
        return (float) attackerCount / (float) defenderCount;
    }

    public static void onRelationChanged(ServerLevel level, String factionA, String factionB) {
        if (level == null || recruitsClaimManager == null) return;

        ServerLevel claimLevel = (server != null) ? server.overworld() : level;

        ClaimEvents instance = new ClaimEvents();

        for (RecruitsClaim claim : recruitsClaimManager.getAllClaims()) {
            if (claim == null || claim.getOwnerFaction() == null) continue;

            String owner = claim.getOwnerFactionStringID();

            boolean ownerAffected = owner.equals(factionA) || owner.equals(factionB);
            boolean participantAffected = involvesFaction(claim, factionA) || involvesFaction(claim, factionB);
            if (!ownerAffected && !participantAffected) continue;

            List<LivingEntity> entities = ClaimUtil.getLivingEntitiesInClaim(claimLevel, claim, LivingEntity::isAlive);
            List<LivingEntity> attackers = new ArrayList<>();
            List<LivingEntity> defenders = new ArrayList<>();

            instance.classifyEntities(entities, claim, attackers, defenders);
            instance.updateParties(claim, attackers, defenders);

            recruitsClaimManager.broadcastClaimUpdateToAll(claimLevel, claim);
        }
    }

    private static boolean involvesFaction(RecruitsClaim claim, String factionId) {
        if (factionId == null) return false;
        for (RecruitsFaction f : claim.attackingParties) {
            if (f != null && factionId.equals(f.getStringID())) return true;
        }
        for (RecruitsFaction f : claim.defendingParties) {
            if (f != null && factionId.equals(f.getStringID())) return true;
        }
        return false;
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
    public void onExplosion(ExplosionEvent.Start event) {
        if(event.getLevel().isClientSide()) return;
        Vec3 vec = event.getExplosion().center();
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