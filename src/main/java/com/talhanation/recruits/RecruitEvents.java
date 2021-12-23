package com.talhanation.recruits;


import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.HorseAIRecruitRide;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RecruitEvents {


    /*@SubscribeEvent
    public void abstractHorseAi(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof AbstractHorseEntity){
            AbstractHorseEntity horse = (AbstractHorseEntity) entity;
            horse.goalSelector.addGoal(0, new HorseAIRecruitRide(horse, 1.5D));
        }
    }*/


    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        RayTraceResult rayTrace = event.getRayTraceResult();
        if (entity instanceof ProjectileEntity) {
            ProjectileEntity projectile = (ProjectileEntity)entity;
            Entity owner = projectile.getOwner();

            if (rayTrace.getType() == RayTraceResult.Type.ENTITY) {
                if (((EntityRayTraceResult) rayTrace).getEntity() instanceof LivingEntity) {
                    LivingEntity impactEntity = (LivingEntity) ((EntityRayTraceResult) rayTrace).getEntity();
                    if (owner instanceof AbstractRecruitEntity) {
                        AbstractRecruitEntity recruit = (AbstractRecruitEntity) owner;

                        if (!AbstractRecruitEntity.canDamageTarget(recruit, impactEntity)) {
                            event.setCanceled(true);
                        }
                        if (recruit.getOwner() == impactEntity) {
                            event.setCanceled(true);
                        }
                    }

                }
            }
        }
    }

    public static void onAttackButton(AbstractRecruitEntity recruit, LivingEntity owner, UUID target, int group) {
        if (recruit.getGroup() == group || group == 0) {
            List<LivingEntity> list = recruit.level.getEntitiesOfClass(LivingEntity.class, recruit.getBoundingBox().inflate(64.0D));
            for (LivingEntity potTargets : list) {
                recruit.getOwner().sendMessage(new StringTextComponent("FOR"), recruit.getOwner().getUUID());
                if (potTargets.getUUID() == target) {
                    if (recruit.getOwner() == owner && recruit.wantsToAttack(potTargets, owner))
                        recruit.getOwner().sendMessage(new StringTextComponent("TARGET"), recruit.getOwner().getUUID());
                        recruit.setTarget(potTargets);
                }
            }
        }
    }

    public static void onStopButton(AbstractRecruitEntity recruit, UUID owner, int group) {
        if (recruit.isTame() &&(recruit.getListen()) && Objects.equals(recruit.getOwnerUUID(), owner) && (recruit.getGroup() == group || group == 0)) {
            recruit.setTarget(null);
        }
    }

}
