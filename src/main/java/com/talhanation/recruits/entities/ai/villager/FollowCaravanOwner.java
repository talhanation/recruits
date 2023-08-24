package com.talhanation.recruits.entities.ai.villager;

import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.UUID;

public class FollowCaravanOwner extends Goal {

    public AbstractVillager villager;
    public UUID uuid;
    public RecruitEntity patrolOwner;

    public FollowCaravanOwner(AbstractVillager villager, UUID uuid) {
        this.villager = villager;
        this.uuid = uuid;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        patrolOwner = this.getPatrolOwner();
    }

    @Nullable
    private RecruitEntity getPatrolOwner() {
        return villager.getCommandSenderWorld().getEntitiesOfClass(RecruitEntity.class, villager.getBoundingBox().inflate(16D))
                .stream().filter(recruit -> recruit.getUUID().equals(uuid))
                .findAny().get();
    }

    @Override
    public void tick() {
        super.tick();

        if (patrolOwner != null && villager.distanceTo(patrolOwner) > 8F){
            villager.getNavigation().moveTo(patrolOwner, 1.05D);
        }
    }
}