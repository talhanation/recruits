package com.talhanation.recruits.entities.ai;

import java.util.EnumSet;
import java.util.List;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class RecruitDefendVillageGoal extends TargetGoal {
    private final AbstractRecruitEntity entity;
    private LivingEntity potentialTarget;
    private final TargetingConditions attackTargeting = (new TargetingConditions()).range(64.0D);

    public RecruitDefendVillageGoal(AbstractRecruitEntity entity) {
        super(entity, false, true);
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        AABB axisalignedbb = this.entity.getBoundingBox().inflate(40.0D, 8.0D, 40.0D);
        List<LivingEntity> list = this.entity.level.getNearbyEntities(Villager.class, this.attackTargeting, this.entity, axisalignedbb);
        List<Player> list1 = this.entity.level.getNearbyPlayers(this.attackTargeting, this.entity, axisalignedbb);

        for(LivingEntity livingentity : list) {
            Villager villagerentity = (Villager) livingentity;

            for (Player playerentity : list1) {
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
            return (!(this.potentialTarget instanceof Player) ||
                    !this.potentialTarget.isSpectator() && !((Player)this.potentialTarget).isCreative())
                    && entity.getState() != 3;
        }
    }

    public void start() {
        this.entity.setTarget(this.potentialTarget);
        super.start();
    }
}