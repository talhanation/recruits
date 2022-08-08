package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FleeTNT extends Goal {

    PathfinderMob entity;

    public FleeTNT(PathfinderMob creatureEntity) {
        this.entity = creatureEntity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        List<PrimedTnt> tntEntities = entity.level.getEntitiesOfClass(PrimedTnt.class, entity.getBoundingBox().inflate(10D));
        if (!tntEntities.isEmpty()) {
            for (PrimedTnt tnt : tntEntities) {
                double fleeDistance = 10.0D;
                Vec3 vecTarget = new Vec3(tnt.getX(), tnt.getY(), tnt.getZ());
                Vec3 vecRec = new Vec3(entity.getX(), entity.getY(), entity.getZ());
                Vec3 fleeDir = vecRec.subtract(vecTarget);
                fleeDir = fleeDir.normalize();
                Vec3 fleePos = new Vec3(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);

                entity.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.25D);

                if (entity instanceof AbstractRecruitEntity recruit) {
                    recruit.setFleeing(true);
                }
                if (entity instanceof AssassinEntity assassin) {
                    assassin.setFleeing(true);
                }
            }
        }
        else {
            if (entity instanceof AbstractRecruitEntity recruit) {
                recruit.setFleeing(false);
            }
            if (entity instanceof AssassinEntity assassin) {
                assassin.setFleeing(false);
            }
        }

    }
}
