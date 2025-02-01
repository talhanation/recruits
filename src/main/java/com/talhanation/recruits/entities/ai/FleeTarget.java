package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AssassinEntity;
import com.talhanation.recruits.pathfinding.AsyncPathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;

public class FleeTarget extends Goal {

    AsyncPathfinderMob entity;

    public FleeTarget(AsyncPathfinderMob creatureEntity) {
    this.entity = creatureEntity;
    }

    @Override
    public boolean canUse() {
        float currentHealth = entity.getHealth();
        float maxHealth = entity.getMaxHealth();


        return (currentHealth <  maxHealth - maxHealth / 2.25);
    }

    @Override
    public void tick() {
        super.tick();

        AtomicBoolean notEmpty = new AtomicBoolean(false);

        entity.getCommandSenderWorld().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(32D),
                (living) -> living.equals(this.entity.getTarget())
        ).forEach(living -> {
            notEmpty.set(true);
            double fleeDistance = 64.0D;
            Vec3 vecTarget = new Vec3(living.getX(), living.getY(), living.getZ());
            Vec3 vecRec = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            Vec3 fleeDir = vecRec.subtract(vecTarget);
            fleeDir = fleeDir.normalize();
            Vec3 fleePos = new Vec3(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
            entity.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.25D);
            if (entity instanceof AssassinEntity recruit) {
                recruit.setFleeing(true);
            }
        });

        if (!notEmpty.get() && entity instanceof AssassinEntity recruit) {
            recruit.setFleeing(false);
        }
    }
}
