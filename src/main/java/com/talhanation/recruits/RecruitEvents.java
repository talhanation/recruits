package com.talhanation.recruits;

import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.horse.HorseRiddenByRecruitGoal;
import com.talhanation.recruits.world.PillagerPatrolSpawn;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class   RecruitEvents {
    private static final Map<ServerLevel, RecruitsPatrolSpawn> RECRUIT_PATROL = new HashMap<>();
    private static final Map<ServerLevel, PillagerPatrolSpawn> PILLAGER_PATROL = new HashMap<>();

    public static MinecraftServer server;
    public boolean needsUpdateHungerDay = true;
    public boolean needsUpdateHungerNight = true;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    public void onTeleportEvent(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(event instanceof EntityTeleportEvent.EnderPearl) && !(event instanceof EntityTeleportEvent.ChorusFruit) && !(event instanceof EntityTeleportEvent.EnderEntity)){

            UUID player_uuid = player.getUUID();
            double targetX = event.getTargetX();
            double targetY = event.getTargetY();
            double targetZ = event.getTargetZ();

            List <AbstractRecruitEntity> recruits = player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                    .inflate(64, 32, 64), AbstractRecruitEntity::isAlive)
                    .stream()
                    .filter(recruit -> recruit.getFollowState() == 1)
                    .filter(recruit -> recruit.getOwnerUUID().equals(player_uuid))
                    .toList();

            recruits.forEach(recruit -> recruit.teleportTo(targetX, targetY, targetZ));

            //wip
        }

    }
    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.level instanceof ServerLevel serverWorld) {
            if (RecruitsModConfig.ShouldRecruitPatrolsSpawn.get()) {
                RECRUIT_PATROL.computeIfAbsent(serverWorld,
                    serverLevel -> new RecruitsPatrolSpawn(serverWorld));
                    RecruitsPatrolSpawn spawner = RECRUIT_PATROL.get(serverWorld);
                    spawner.tick();
            }

            if (RecruitsModConfig.ShouldPillagerPatrolsSpawn.get()) {
                PILLAGER_PATROL.computeIfAbsent(serverWorld,
                    serverLevel -> new PillagerPatrolSpawn(serverWorld));
                    PillagerPatrolSpawn pillagerSpawner = PILLAGER_PATROL.get(serverWorld);
                    pillagerSpawner.tick();
            }
        }


        //HUNGER
        if (!event.level.isClientSide && event.level instanceof ServerLevel serverWorld) {
            if(serverWorld.getLevel().isDay() && this.needsUpdateHungerDay){
                serverSideUpdateRecruitHunger(serverWorld);
                this.needsUpdateHungerNight = true;
                this.needsUpdateHungerDay = false;
            }
            else if (serverWorld.getLevel().isNight() && this.needsUpdateHungerNight){

                serverSideUpdateRecruitHunger(serverWorld);
                this.needsUpdateHungerNight = false;
                this.needsUpdateHungerDay = true;
            }
        }

    }

    public void serverSideUpdateRecruitHunger(ServerLevel level){
        List<AbstractRecruitEntity> recruitList = new ArrayList<>();
        for(Entity entity : level.getEntities().getAll()){
            if(entity instanceof AbstractRecruitEntity recruit)
                recruitList.add(recruit);
        }
        for(AbstractRecruitEntity recruit : recruitList){
            recruit.updateHunger();
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        HitResult rayTrace = event.getRayTraceResult();
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();

            if (rayTrace.getType() == HitResult.Type.ENTITY) {
                if (((EntityHitResult) rayTrace).getEntity() instanceof LivingEntity impactEntity) {

                    if (owner instanceof AbstractRecruitEntity recruit) {

                        if (!canDamageTarget(recruit, impactEntity)) {
                            event.setCanceled(true);
                        }
                        else {
                            recruit.addXp(2);
                            recruit.checkLevel();
                        }
                    }

                    if (owner instanceof AbstractIllager illager && !RecruitsModConfig.PillagerFriendlyFire.get()) {

                        if (illager.isAlliedTo(impactEntity)) {
                            event.setCanceled(true);
                        }
                    }

                    if (owner instanceof Player player) {
                        if (!canHarmTeam(player, impactEntity)) {
                            event.setCanceled(true);
                        }
                    }

                }
            }
        }
    }
    @SubscribeEvent
    public void onPlayerInteractWithCaravan(PlayerInteractEvent.EntityInteract entityInteract){
        Player player = entityInteract.getEntity();
        Entity interacting = entityInteract.getTarget();

        if(interacting instanceof AbstractChestedHorse chestedHorse){
            CompoundTag nbt = chestedHorse.getPersistentData();
            if(nbt.contains("Caravan") && chestedHorse.hasChest()){
                List<AbstractRecruitEntity> recruits = player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(64F));
                for(AbstractRecruitEntity recruit : recruits){
                    if(!recruit.isOwned() && (recruit.getName().getString().equals("Caravan Leader") || recruit.getName().getString().equals("Caravan Guard"))){
                        recruit.setTarget(player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if(Main.isMusketModLoaded) {
            Entity sourceEntity = event.getSource().getEntity();
            if(sourceEntity instanceof AbstractRecruitEntity owner && IWeapon.isMusketModWeapon(owner.getMainHandItem())){
                Entity target = event.getEntity();
                if (target instanceof LivingEntity impactEntity) {

                    if (!canDamageTarget(owner, impactEntity)) {
                        event.setCanceled(true);
                    }
                    else {
                        owner.addXp(2);
                        owner.checkLevel();
                    }

                }
            }
        }
    }
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        Entity target = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();

        if(target instanceof AbstractRecruitEntity recruit && sourceEntity instanceof Player player){

            if (!canHarmTeam(player, recruit)){
                event.setCanceled(true);
            }

        }
    }

    @SubscribeEvent
    public void onHorseJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof AbstractHorse horse) {
            horse.goalSelector.addGoal(0, new HorseRiddenByRecruitGoal(horse));
        }
    }

    @SubscribeEvent
    public void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        if(RecruitsModConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();

            if (blockBreaker != null){
                List<AbstractRecruitEntity> list = blockBreaker.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 1) {
                        recruits.setTarget(blockBreaker);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(blockBreaker, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }

        if(RecruitsModConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();

            if (blockBreaker != null){
                List<AbstractRecruitEntity> list = blockBreaker.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(blockBreaker);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(blockBreaker, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        if(RecruitsModConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(livingBlockPlacer.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, livingBlockPlacer.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, livingBlockPlacer) && recruits.getState() == 1) {
                        recruits.setTarget(livingBlockPlacer);
                    }
                }

                if(blockPlacer instanceof Player player && list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }

        if(RecruitsModConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(livingBlockPlacer.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, livingBlockPlacer.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, livingBlockPlacer) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(livingBlockPlacer);
                    }
                }

                if(blockPlacer instanceof Player player && list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getHitVec().getBlockPos();
        Player player = event.getEntity();

        BlockState selectedBlock = player.getCommandSenderWorld().getBlockState(pos);
        BlockEntity blockEntity = player.getCommandSenderWorld().getBlockEntity(pos);

        if (selectedBlock.is(BlockTags.BUTTONS) ||
            selectedBlock.is(BlockTags.DOORS) ||
            selectedBlock.is(BlockTags.WOODEN_TRAPDOORS) ||
            selectedBlock.is(BlockTags.WOODEN_BUTTONS) ||
            selectedBlock.is(BlockTags.WOODEN_DOORS) ||
            selectedBlock.is(BlockTags.SHULKER_BOXES) ||
            selectedBlock.is(BlockTags.FENCE_GATES) ||
            selectedBlock.is(BlockTags.ANVIL) ||
            (blockEntity instanceof Container)
        ) {

            if(RecruitsModConfig.AggroRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 1) {
                        recruits.setTarget(player);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }


            if(RecruitsModConfig.NeutralRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(player);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }
        }

    }

    public boolean canDamageTargetBlockEvent(AbstractRecruitEntity recruit, LivingEntity target) {
        if (recruit.isOwned() && target instanceof AbstractRecruitEntity recruitEntityTarget) {
            if (recruit.getOwnerUUID().equals(recruitEntityTarget.getOwnerUUID())){
                return false;
            }
            //extra for safety
            else if (recruit.getTeam() != null && recruitEntityTarget.getTeam() != null && recruit.getTeam().equals(recruitEntityTarget.getTeam())){
                return false;
            }
        }
        else if (recruit.isOwned() && target instanceof Player player) {
            if (recruit.getOwnerUUID().equals(player.getUUID())){
                return false;
            }
        }
        else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getProtectUUID() != null && recruitEntityTarget.getProtectUUID() != null && recruit.getProtectUUID().equals(recruitEntityTarget.getProtectUUID())){
            return false;
        }
        return RecruitEvents.canHarmTeamNoFriendlyFire(recruit, target);
    }

    public static boolean canDamageTarget(AbstractRecruitEntity recruit, LivingEntity target) {
        if (recruit.isOwned() && target instanceof AbstractRecruitEntity recruitEntityTarget) {
            if (recruit.getOwnerUUID().equals(recruitEntityTarget.getOwnerUUID())){
                return false;
            }
            //extra for safety
            else if (recruit.getTeam() != null && recruitEntityTarget.getTeam() != null && recruit.getTeam().equals(recruitEntityTarget.getTeam())){
                return false;
            }
        }
        else if (recruit.isOwned() && target instanceof Player player) {
            if (recruit.getOwnerUUID().equals(player.getUUID())){
                return false;
            }
        }
        else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getProtectUUID() != null && recruitEntityTarget.getProtectUUID() != null && recruit.getProtectUUID().equals(recruitEntityTarget.getProtectUUID())){
            return false;
        }
        return RecruitEvents.canHarmTeam(recruit, target);
    }

    public static boolean canHarmTeam(LivingEntity attacker, LivingEntity target) {
        Team team = attacker.getTeam();
        Team team1 = target.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team1) || team.isAllowFriendlyFire();
            //attacker can Harm target when attacker has no team
            //or attacker and target are not allied
            //or team friendly is true
        }
    }

    public static boolean canHarmTeamNoFriendlyFire(LivingEntity attacker, LivingEntity target) {
        Team team = attacker.getTeam();
        Team team1 = target.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team1);
            //attacker can Harm target when attacker has no team
            //or attacker and target are not allied
            //or team friendly is true
        }
    }

    @SubscribeEvent
    public void onRecruitDeath(LivingDeathEvent event) {
        Entity target = event.getEntity();

        if(target instanceof AbstractRecruitEntity recruit){
            if (recruit.getTeam() != null){
                TeamEvents.addNPCToData(server.overworld(), recruit.getTeam().getName(), -1);
            }

            //Morale loss when recruits friend die
            if(recruit.getIsOwned() && !server.overworld().isClientSide()){
                UUID owner = recruit.getOwnerUUID();
                List<AbstractRecruitEntity> recruits = recruit.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, recruit.getBoundingBox().inflate(64.0D));
    
                for (AbstractRecruitEntity recruit2 : recruits) {
                    if(recruit2.getOwnerUUID() != null && recruit2.getOwnerUUID().equals(owner)){
                        float currentMoral = recruit2.getMoral();
                        float newMorale = currentMoral - 0.2F;
                        if(newMorale > 0) recruit2.setMoral(newMorale);
                        else recruit2.setMoral(0F);

                        //add to target list
                    }
                } 
            }
        }
    }

    public byte getSavedWarning(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getByte("RecruitWarnings");
    }

    public void saveCurrentWarning(Player player, byte x) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putByte("RecruitWarnings", x);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    private void warnPlayer(Player player, Component component){
        saveCurrentWarning(player, (byte) (getSavedWarning(player) + 1));

        if(getSavedWarning(player) >= 0){
            player.sendSystemMessage(component);
            saveCurrentWarning(player, (byte) -10);
        }
    }

    public static MutableComponent TEXT_BLOCK_WARN(String name) {
        return Component.translatable("chat.recruits.text.block_placing_warn", name);
    }

    public static MutableComponent TEXT_INTERACT_WARN(String name) {
        return Component.translatable("chat.recruits.text.block_interact_warn", name);
    }



}
