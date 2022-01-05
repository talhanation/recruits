package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AssassinEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class AssassinFleeSuccess extends Goal {

    AssassinEntity entity;
    int fleeTick = 0;
    BlockPos fleePos;

    public AssassinFleeSuccess(AssassinEntity assassin) {
    this.entity = assassin;
    }

    @Override
    public boolean canUse() {
        return this.entity.getIsInOrder() && entity.getTarget() == null;
    }

    @Override
    public void start() {
        super.start();
        fleeTick = 0;
        fleePos = entity.getAssassinOnPos();
    }

    @Override
    public void tick() {
        super.tick();
        if (fleePos != null) {
            double fleeDistance = 32D;
            Vector3d vecTarget = new Vector3d(fleePos.getX(), fleePos.getY(), fleePos.getZ());
            Vector3d vecRec = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
            Vector3d fleeDir = vecRec.subtract(vecTarget);
            fleeDir = fleeDir.normalize();
            Vector3d fleePos1 = new Vector3d(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
            entity.getNavigation().moveTo(fleePos1.x, fleePos1.y, fleePos1.z, 1.15D);
            fleeTick++;
            if (fleeTick >= 3000) entity.remove();
        }
    }
}
