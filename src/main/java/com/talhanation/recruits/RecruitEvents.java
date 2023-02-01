package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAddRecruitToTeam;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class   RecruitEvents {

    public int timestamp;
    public static final TranslatableContents TEXT_BLOCK_WARN = new TranslatableContents("chat.recruits.text.block_placing_warn");
    private static final Map<ServerLevel, RecruitsPatrolSpawn> RECRUIT_PATROL = new HashMap<>();

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
    public void onServerTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.level instanceof ServerLevel serverWorld) {
            RECRUIT_PATROL.computeIfAbsent(serverWorld,
                    k -> new RecruitsPatrolSpawn(serverWorld));
            RecruitsPatrolSpawn spawner = RECRUIT_PATROL.get(serverWorld);
            spawner.tick();
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

                    if (owner instanceof AbstractIllager illager) {

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
            timestamp = 0;
            List<AbstractRecruitEntity> list = Objects.requireNonNull(blockBreaker.level.getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D)));
            for (AbstractRecruitEntity recruits : list) {
                if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 1) {
                    recruits.setTarget(blockBreaker);
                        blockBreaker.sendSystemMessage(Component.literal(list.get(0).getName().getString() + ": " + TEXT_BLOCK_WARN));
                }
            }
        }

        if(RecruitsModConfig.NeutralRecruitsBlockEvents.get()) {
            Player blockBreaker = event.getPlayer();

            List<AbstractRecruitEntity> list = Objects.requireNonNull(blockBreaker.level.getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D)));
            for (AbstractRecruitEntity recruits : list) {
                if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 0 && recruits.isOwned()) {
                    recruits.setTarget(blockBreaker);
                        blockBreaker.sendSystemMessage(Component.literal(list.get(0).getName().getString() + ": " + TEXT_BLOCK_WARN));
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
                            livingBlockPlacer.sendSystemMessage(Component.literal(list.get(0).getName().getString() + ": " + TEXT_BLOCK_WARN));
                    }
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
                        livingBlockPlacer.sendSystemMessage(Component.literal(list.get(0).getName().getString() + ": " + TEXT_BLOCK_WARN));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(BlockEvent.BlockToolModificationEvent event) {
        if(RecruitsModConfig.AggroRecruitsBlockEvents.get()) {
            Player interacter = event.getPlayer();

            List<AbstractRecruitEntity> list = Objects.requireNonNull(interacter.level.getEntitiesOfClass(AbstractRecruitEntity.class, interacter.getBoundingBox().inflate(32.0D)));
            for (AbstractRecruitEntity recruits : list) {
                if (canDamageTargetBlockEvent(recruits, interacter) && recruits.getState() == 1) {
                    recruits.setTarget(interacter);
                    interacter.sendSystemMessage(Component.literal(list.get(0).getName().getString() + ": " + TEXT_BLOCK_WARN));
                }
            }
        }
        if(RecruitsModConfig.NeutralRecruitsBlockEvents.get()) {
            Player interacter = event.getPlayer();

            List<AbstractRecruitEntity> list = Objects.requireNonNull(interacter.level.getEntitiesOfClass(AbstractRecruitEntity.class, interacter.getBoundingBox().inflate(32.0D)));
            for (AbstractRecruitEntity recruits : list) {
                if (canDamageTargetBlockEvent(recruits, interacter) && recruits.getState() == 0 && recruits.isOwned()) {
                    recruits.setTarget(interacter);
                    interacter.sendSystemMessage(Component.literal(list.get(0).getName().getString() + ": " + TEXT_BLOCK_WARN));
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

    public boolean canDamageTarget(AbstractRecruitEntity recruit, LivingEntity target) {
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
}
