package com.talhanation.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
                        } else
                            recruit.addXp(2);
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

    public static void onAttackButton(AbstractRecruitEntity recruit, LivingEntity owner, UUID target, int group) {
        if (recruit.getGroup() == group || group == 0) {
            List<LivingEntity> list = recruit.level.getEntitiesOfClass(LivingEntity.class, recruit.getBoundingBox().inflate(64.0D));
            for (LivingEntity potTargets : list) {
                recruit.getOwner().sendMessage(new TextComponent("FOR"), recruit.getOwner().getUUID());
                if (potTargets.getUUID() == target) {
                    if (recruit.getOwner() == owner && recruit.wantsToAttack(potTargets, owner))
                        recruit.getOwner().sendMessage(new TextComponent("TARGET"), recruit.getOwner().getUUID());
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
