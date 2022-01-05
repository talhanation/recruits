package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractOrderAbleEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;

public class FleeTNT extends Goal {

    CreatureEntity entity;

    public FleeTNT(CreatureEntity creatureEntity) {
    this.entity = creatureEntity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        List<TNTEntity> tntEntities = entity.level.getEntitiesOfClass(TNTEntity.class, entity.getBoundingBox().inflate(10D));
        if (!tntEntities.isEmpty()) {
            for (TNTEntity tnt : tntEntities) {
                double fleeDistance = 10.0D;
                Vector3d vecTarget = new Vector3d(tnt.getX(), tnt.getY(), tnt.getZ());
                Vector3d vecRec = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
                Vector3d fleeDir = vecRec.subtract(vecTarget);
                fleeDir = fleeDir.normalize();
                Vector3d fleePos = new Vector3d(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
                entity.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.25D);
                if (entity instanceof AbstractRecruitEntity) {
                    AbstractRecruitEntity recruit = (AbstractRecruitEntity) entity;
                    recruit.setFleeing(true);
                }
                if (entity instanceof AssassinEntity) {
                    AssassinEntity recruit = (AssassinEntity) entity;
                    recruit.setFleeing(true);
                }
            }
        }
        else
        if (entity instanceof AbstractRecruitEntity) {
            AbstractRecruitEntity recruit = (AbstractRecruitEntity) entity;
            recruit.setFleeing(false);
        }
        if (entity instanceof AssassinEntity) {
            AssassinEntity recruit = (AssassinEntity) entity;
            recruit.setFleeing(false);
        }

    }
}
