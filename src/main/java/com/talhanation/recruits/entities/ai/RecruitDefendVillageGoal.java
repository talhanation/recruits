package com.talhanation.recruits.entities.ai;

import java.util.EnumSet;
import java.util.List;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;

public class RecruitDefendVillageGoal extends TargetGoal {
    private final AbstractRecruitEntity entity;
    private LivingEntity potentialTarget;
    private final EntityPredicate attackTargeting = (new EntityPredicate()).range(64.0D);

    public RecruitDefendVillageGoal(AbstractRecruitEntity entity) {
        super(entity, false, true);
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        AxisAlignedBB axisalignedbb = this.entity.getBoundingBox().inflate(40.0D, 8.0D, 40.0D);
        List<LivingEntity> list = this.entity.level.getNearbyEntities(VillagerEntity.class, this.attackTargeting, this.entity, axisalignedbb);
        List<PlayerEntity> list1 = this.entity.level.getNearbyPlayers(this.attackTargeting, this.entity, axisalignedbb);

        for(LivingEntity livingentity : list) {
            VillagerEntity villagerentity = (VillagerEntity) livingentity;

            for (PlayerEntity playerentity : list1) {
                int i = villagerentity.getPlayerReputation(playerentity);
                if ((playerentity.getUUID() != entity.getOwnerUUID())) {
                    if (i <= -100) {
                        this.potentialTarget = playerentity;
                    }
                }
            }
        }

        if (this.potentialTarget == null) {
            return false;
        } else {
            return (!(this.potentialTarget instanceof PlayerEntity) ||
                    !this.potentialTarget.isSpectator() && !((PlayerEntity)this.potentialTarget).isCreative())
                    && entity.getState() != 3;
        }
    }

    public void start() {
        this.entity.setTarget(this.potentialTarget);
        super.start();
    }
}