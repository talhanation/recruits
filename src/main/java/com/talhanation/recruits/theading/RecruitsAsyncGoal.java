package com.talhanation.recruits.theading;

import com.talhanation.recruits.entities.AbstractRecruitEntity;

public abstract class RecruitsAsyncGoal {
    protected final AbstractRecruitEntity recruit;

    public RecruitsAsyncGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }


    public abstract boolean canUse();
    public abstract boolean canContinueToUse();

    // Start-Methode zur Initialisierung
    public void start() {}

    // Neue tick()-Methode zur zeitbasierten Logik
    public void tick() {}

    // Wird einmalig ausgeführt, wenn das Goal abgeschlossen wird
    public void stop() {}
}

