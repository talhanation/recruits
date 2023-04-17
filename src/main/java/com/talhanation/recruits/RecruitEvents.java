package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAddRecruitToTeam;
import com.talhanation.recruits.world.PillagerPatrolSpawn;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class   RecruitEvents {

    public int timestamp;
    public static final TranslatableComponent TEXT_BLOCK_WARN = new TranslatableComponent("chat.recruits.text.block_placing_warn");
    public static final TranslatableComponent TEXT_INTERACT_WARN = new TranslatableComponent("chat.recruits.text.block_interact_warn");

    private static final Map<ServerLevel, RecruitsPatrolSpawn> RECRUIT_PATROL = new HashMap<>();
    private static final Map<ServerLevel, PillagerPatrolSpawn> PILLAGER_PATROL = new HashMap<>();

    @SubscribeEvent
    public void onTeleportEvent(EntityTeleportEvent event) {


        if (event.getEntity() instanceof ServerPlayer player && !(event instanceof EntityTeleportEvent.EnderPearl) && !(event instanceof EntityTeleportEvent.ChorusFruit) && !(event instanceof EntityTeleportEvent.EnderEntity)){

            UUID player_uuid = player.getUUID();
            double targetX = event.getTargetX();
            double targetY = event.getTargetY();
            double targetZ = event.getTargetZ();

            List <AbstractRecruitEntity> recruits = player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
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
    public void onServerTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isClientSide && event.world instanceof ServerLevel serverWorld) {
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

                        if (!this.canDamageTarget(recruit, impactEntity)) {
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
    public void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        if(RecruitsModConfig.AggroRecruitsBlockEvents.get()) {
            Player blockBreaker = event.getPlayer();

            if (blockBreaker != null){
                List<AbstractRecruitEntity> list = blockBreaker.level.getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 1) {
                        recruits.setTarget(blockBreaker);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(blockBreaker, new TextComponent(list.get(0).getName().getString() +": " + TEXT_BLOCK_WARN.getString()), list);
                }
            }
        }

        if(RecruitsModConfig.NeutralRecruitsBlockEvents.get()) {
            Player blockBreaker = event.getPlayer();

            if (blockBreaker != null){
                List<AbstractRecruitEntity> list = blockBreaker.level.getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(blockBreaker);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(blockBreaker, new TextComponent(list.get(0).getName().getString() +": " + TEXT_BLOCK_WARN.getString()), list);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        if(RecruitsModConfig.AggroRecruitsBlockEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(livingBlockPlacer.level.getEntitiesOfClass(AbstractRecruitEntity.class, livingBlockPlacer.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, livingBlockPlacer) && recruits.getState() == 1) {
                        recruits.setTarget(livingBlockPlacer);
                    }
                }

                if(blockPlacer instanceof Player player && list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, new TextComponent(list.get(0).getName().getString() +": " + TEXT_BLOCK_WARN.getString()), list);
                }
            }
        }

        if(RecruitsModConfig.NeutralRecruitsBlockEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(livingBlockPlacer.level.getEntitiesOfClass(AbstractRecruitEntity.class, livingBlockPlacer.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, livingBlockPlacer) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(livingBlockPlacer);
                    }
                }

                if(blockPlacer instanceof Player player && list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, new TextComponent(list.get(0).getName().getString() +": " + TEXT_BLOCK_WARN.getString()), list);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getHitVec().getBlockPos();
        Player player = event.getPlayer();

        BlockState selectedBlock = player.level.getBlockState(pos);
        BlockEntity blockEntity = player.level.getBlockEntity(pos);

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


            if(RecruitsModConfig.AggroRecruitsBlockEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 1) {
                        recruits.setTarget(player);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, new TextComponent(list.get(0).getName().getString() +": " + TEXT_INTERACT_WARN.getString()), list);
                }
            }

            if(RecruitsModConfig.NeutralRecruitsBlockEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(player);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, new TextComponent(list.get(0).getName().getString() +": " + TEXT_INTERACT_WARN.getString()), list);
                }
            }
        }

    }

    public static void onPlayerLeaveTeam(){

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
        else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getEscortUUID() != null && recruitEntityTarget.getEscortUUID() != null && recruit.getEscortUUID().equals(recruitEntityTarget.getEscortUUID())){
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
        else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getEscortUUID() != null && recruitEntityTarget.getEscortUUID() != null && recruit.getEscortUUID().equals(recruitEntityTarget.getEscortUUID())){
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


    public void onRecruitDeath(LivingDeathEvent event) {
        Entity target = event.getEntity();

        if(target instanceof AbstractRecruitEntity recruit){
            if (recruit.getTeam() != null){
                Main.LOGGER.debug("recruit in team died: " + recruit.getTeam().getName());
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAddRecruitToTeam(recruit.getTeam().getName(), -1));
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

    private void warnPlayer(Player player, TextComponent component, List<AbstractRecruitEntity> list){
        saveCurrentWarning(player, (byte) (getSavedWarning(player) + 1));

        if(getSavedWarning(player) >= 0){
            player.sendMessage(component, player.getUUID());
            saveCurrentWarning(player, (byte) -10);
        }
    }
}
