package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.CaptainEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class CaptainControlBoatAI extends Goal {
    public CaptainControlBoatAI(CaptainEntity captainEntity) {
    }

    @Override
    public boolean canUse() {
        return false;
    }
}
