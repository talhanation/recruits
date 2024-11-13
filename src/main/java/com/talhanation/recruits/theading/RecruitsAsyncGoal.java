package com.talhanation.recruits.theading;

import com.talhanation.recruits.entities.AbstractRecruitEntity;

public abstract class RecruitsAsyncGoal {
    protected final AbstractRecruitEntity recruit;

    public RecruitsAsyncGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    public abstract void execute();
}

