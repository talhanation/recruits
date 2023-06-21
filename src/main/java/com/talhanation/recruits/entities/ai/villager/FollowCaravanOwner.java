package com.talhanation.recruits.entities.ai.villager;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FollowCaravanOwner extends Goal {

    public PathfinderMob mob;
    public UUID uuid;
    public LivingEntity patrolOwner;

    public FollowCaravanOwner(PathfinderMob mob, UUID uuid) {
        this.mob = mob;
        this.uuid = uuid;
    }

    @Override
    public boolean canUse() {
        return this.getPatrolOwner() != null;
    }

    @Override
    public void start() {
        super.start();
        patrolOwner = this.getPatrolOwner();
    }

    @Nullable
    private LivingEntity getPatrolOwner() {
        List<LivingEntity> livings = mob.level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16D));
        for (LivingEntity living : livings){
            if(living.isAlive() && living.getUUID().equals(uuid)){
                return living;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();

        if (patrolOwner != null && patrolOwner.isAlive() && mob.distanceTo(patrolOwner) > 12F){
            mob.getNavigation().moveTo(patrolOwner, 1.05D);
        }
    }
}
