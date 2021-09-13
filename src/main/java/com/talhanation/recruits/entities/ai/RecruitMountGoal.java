package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class RecruitMountGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private final UUID mount;
    private final double speedModifier;
    private final double within;

    public RecruitMountGoal(AbstractRecruitEntity recruit, double v, double within) {
        this.recruit = recruit;
        this.speedModifier = v;
        this.within = within;
        this.mount = recruit.getMount();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }
    public boolean canUse() {
        if (this.recruit.getMount() == null && recruit.getMove()) {
            return false;
        }
        else if (this.recruit.getMovePos().closerThan(recruit.position(), within))
            return true;
        else
            return false;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void tick() {
        List<Entity> list = recruit.level.getEntitiesOfClass(Entity.class, recruit.getBoundingBox().inflate(40.0D));
        for (Entity mount : list) {
            if (mount != null) {
                this.recruit.getNavigation().moveTo(mount.getX(), mount.getY(), mount.getZ(), this.speedModifier);

                if (mount.closerThan(this.recruit, 2)) {
                    recruit.setShouldFollow(false);

                    recruit.startRiding(mount);
                }

            }
        }


    }
}

