package com.talhanation.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RecruitEvents {

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        HitResult rayTrace = event.getRayTraceResult();
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile)entity;
            Entity owner = projectile.getOwner();

            if (rayTrace.getType() == HitResult.Type.ENTITY) {
                if (((EntityHitResult) rayTrace).getEntity() instanceof LivingEntity) {
                    LivingEntity impactEntity = (LivingEntity) ((EntityHitResult) rayTrace).getEntity();
                    if (owner instanceof AbstractRecruitEntity) {
                        AbstractRecruitEntity recruit = (AbstractRecruitEntity) owner;

                        if (!AbstractRecruitEntity.canDamageTarget(recruit, impactEntity)) {
                            event.setCanceled(true);
                        }

                        if (recruit.getOwner() == impactEntity) {
                            event.setCanceled(true);
                        } else{
                            recruit.addXp(2);
                            recruit.checkLevel();
                        }


                    }

                    if (owner instanceof AbstractIllager) {
                        AbstractIllager illager = (AbstractIllager) owner;

                        if (illager.isAlliedTo(impactEntity)) {
                            event.setCanceled(true);
                        }
                    }

                }
            }
        }
    }


    @SubscribeEvent
    public static void onPlayerLeaveTeam(){

    }
}
