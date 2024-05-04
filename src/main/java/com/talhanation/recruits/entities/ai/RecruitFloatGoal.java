package com.talhanation.recruits.entities.ai;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RecruitFloatGoal extends Goal {
    private final Mob mob;
    private long lastCanUseCheck;

    private long lastCanUseCheck;

    public RecruitFloatGoal(Mob p_25230_) {
        this.mob = p_25230_;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP));
        p_25230_.getNavigation().setCanFloat(true);
    }

    @Override
    public void start() {
        super.start();
        mob.setSprinting(true);
    }

    @Override
    public void stop() {
        super.start();
        mob.setSprinting(false);
    }

    public boolean canUse() {
        long i = this.mob.getCommandSenderWorld().getGameTime();
        if (i - this.lastCanUseCheck >= 20L) {
            this.lastCanUseCheck = i;
            return this.mob.isInWater() && this.mob.getFluidHeight(FluidTags.WATER) > this.mob.getFluidJumpThreshold() || this.mob.isInLava() || this.mob.isInFluidType((fluidType, height) -> this.mob.canSwimInFluidType(fluidType) && height > this.mob.getFluidJumpThreshold());
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isInWater();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.mob.getRandom().nextFloat() < 0.8F) {
            this.mob.getJumpControl().jump();
        }
    }
}
