package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinEntity;
import com.talhanation.recruits.pathfinding.AsyncPathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FleeTNT extends Goal {

    private final AsyncPathfinderMob entity;
    private int cooldown = 0;

    public FleeTNT(AsyncPathfinderMob creatureEntity) {
        this.entity = creatureEntity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        List<PrimedTnt> tntEntities = entity.getCommandSenderWorld().getEntitiesOfClass(
                PrimedTnt.class,
                entity.getBoundingBox().inflate(10D)
        );
        if (tntEntities.isEmpty()) {
            setFleeing(false);
            return;
        }

        Vec3 entityPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        Vec3 combinedFleeDir = Vec3.ZERO;

        for (PrimedTnt tnt : tntEntities) {
            Vec3 tntPos = new Vec3(tnt.getX(), tnt.getY(), tnt.getZ());
            Vec3 fleeDir = entityPos.subtract(tntPos);

            double distanceSq = entityPos.distanceToSqr(tntPos);
            int fuse = tnt.getFuse();
            double fuseWeight = 1.0 / (fuse + 1.0); // Shorter fuse = higher weight
            double distanceWeight = 1.0 / (distanceSq + 1.0);
            double totalWeight = fuseWeight * distanceWeight;

            combinedFleeDir = combinedFleeDir.add(fleeDir.scale(totalWeight));
        }

        if (combinedFleeDir.lengthSqr() > 0) {
            combinedFleeDir = combinedFleeDir.normalize();

            // Add a small random offset for natural movement
            double randomAngle = (entity.getRandom().nextDouble() - 0.5) * 0.5;
            combinedFleeDir = combinedFleeDir.yRot((float) randomAngle);

            // Calculate flee position
            double fleeDistance = 10.0D;
            Vec3 fleePos = entityPos.add(combinedFleeDir.scale(fleeDistance));

            // Move to the flee position
            entity.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.25D);
            setFleeing(true);
        } else {
            setFleeing(false);
        }
    }

    private void setFleeing(boolean fleeing) {
        if (entity instanceof AbstractRecruitEntity recruit) {
            recruit.setFleeing(fleeing);
        }
        if (entity instanceof AssassinEntity assassin) {
            assassin.setFleeing(fleeing);
        }
    }
}