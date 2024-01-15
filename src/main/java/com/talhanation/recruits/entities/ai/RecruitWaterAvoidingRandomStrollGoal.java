package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class RecruitWaterAvoidingRandomStrollGoal extends RandomStrollGoal {
    protected final float probability;
    private AbstractRecruitEntity recruit;

    public RecruitWaterAvoidingRandomStrollGoal(AbstractRecruitEntity recruit, double speed, float probability) {
        super(recruit, speed);
        this.probability = probability;
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return !recruit.getShouldProtect() && !recruit.getShouldFollow() && !recruit.getFleeing() && !recruit.needsToGetFood() && !recruit.getShouldMount() && super.canUse();
    }

    @Nullable
    protected Vec3 getPosition() {
        if (this.mob.isInWaterOrBubble()) {
            Vec3 vec3 = LandRandomPos.getPos(this.mob, 15, 7);
            return vec3 == null ? super.getPosition() : vec3;
        } else {
            return this.mob.getRandom().nextFloat() >= this.probability ? LandRandomPos.getPos(this.mob, 10, 7) : super.getPosition();
        }
    }
}
