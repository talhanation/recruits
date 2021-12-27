package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;

public class RecruitFleeTNT extends Goal {

    AbstractRecruitEntity recruit;

    public RecruitFleeTNT(AbstractRecruitEntity recruit) {
    this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        List<TNTEntity> tntEntities = recruit.level.getEntitiesOfClass(TNTEntity.class, recruit.getBoundingBox().inflate(10D));
        if (!tntEntities.isEmpty()) {
            for (TNTEntity tnt : tntEntities) {
                double fleeDistance = 10.0D;
                Vector3d vecTarget = new Vector3d(tnt.getX(), tnt.getY(), tnt.getZ());
                Vector3d vecRec = new Vector3d(recruit.getX(), recruit.getY(), recruit.getZ());
                Vector3d fleeDir = vecRec.subtract(vecTarget);
                fleeDir = fleeDir.normalize();
                Vector3d fleePos = new Vector3d(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
                recruit.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.25D);
                recruit.setFleeing(true);
            }
        }
        else
            recruit.setFleeing(false);

    }
}
