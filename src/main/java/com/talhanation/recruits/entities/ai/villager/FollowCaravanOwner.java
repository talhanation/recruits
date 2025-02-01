package com.talhanation.recruits.entities.ai.villager;

import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.List;
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
        List<RecruitEntity> recruits = villager.getCommandSenderWorld().getEntitiesOfClass(
                RecruitEntity.class,
                villager.getBoundingBox().inflate(16D),
                (recruit) -> recruit.getUUID().equals(uuid)
        );

        return recruits.isEmpty() ? null : recruits.get(0);
    }

    @Override
    public void tick() {
        super.tick();

        if (patrolOwner != null && villager.distanceTo(patrolOwner) > 8F) {
            villager.getNavigation().moveTo(patrolOwner, 1.05D);
        }
    }
}