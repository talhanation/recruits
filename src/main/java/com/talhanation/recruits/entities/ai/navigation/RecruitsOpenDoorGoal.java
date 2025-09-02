package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Mob;

public class RecruitsOpenDoorGoal extends RecruitsDoorInteractGoal{
    private final boolean closeDoor;
    private int forgetTime;

    public RecruitsOpenDoorGoal(AbstractRecruitEntity p_25678_, boolean p_25679_) {
        super(p_25678_);
        this.recruit = p_25678_;
        this.closeDoor = p_25679_;
    }

    public boolean canContinueToUse() {
        return this.closeDoor && this.forgetTime > 0 && super.canContinueToUse();
    }

    public void start() {
        this.forgetTime = 25;
        this.setOpen(true);
    }

    public void stop() {
        this.setOpen(false);
    }

    public void tick() {
        --this.forgetTime;
        super.tick();
    }
}