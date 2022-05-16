package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AssassinEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

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
            Vec3 vecTarget = new Vec3(fleePos.getX(), fleePos.getY(), fleePos.getZ());
            Vec3 vecRec = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            Vec3 fleeDir = vecRec.subtract(vecTarget);
            fleeDir = fleeDir.normalize();
            Vec3 fleePos1 = new Vec3(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
            entity.getNavigation().moveTo(fleePos1.x, fleePos1.y, fleePos1.z, 1.15D);
            fleeTick++;
            if (fleeTick >= 3000) entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
    }
}
