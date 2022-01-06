package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AssassinEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;

public class FleeTarget extends Goal {

    CreatureEntity entity;

    public FleeTarget(CreatureEntity creatureEntity) {
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
        List<LivingEntity> list = entity.level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(32D));
        if (!list.isEmpty()) {
            for (LivingEntity living : list) {
                if (living.equals(this.entity.getTarget())) {
                    double fleeDistance = 64.0D;
                    Vector3d vecTarget = new Vector3d(living.getX(), living.getY(), living.getZ());
                    Vector3d vecRec = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
                    Vector3d fleeDir = vecRec.subtract(vecTarget);
                    fleeDir = fleeDir.normalize();
                    Vector3d fleePos = new Vector3d(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
                    entity.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.25D);
                    if (entity instanceof AssassinEntity) {
                        AssassinEntity recruit = (AssassinEntity) entity;
                        recruit.setFleeing(true);
                    }
                }
            }
        }
        else
        if (entity instanceof AssassinEntity) {
            AssassinEntity recruit = (AssassinEntity) entity;
            recruit.setFleeing(false);
        }

    }
}
