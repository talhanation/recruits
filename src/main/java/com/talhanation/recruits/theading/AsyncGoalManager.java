package com.talhanation.recruits.theading;

import com.talhanation.recruits.entities.AbstractRecruitEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AsyncGoalManager {
    private final Queue<RecruitsAsyncGoal> activeGoals = new ConcurrentLinkedQueue<>();
    private final List<RecruitsAsyncGoal> goals;

    public AsyncGoalManager(AbstractRecruitEntity recruit) {
        this.goals = new ArrayList<>();
    }

    public void registerGoal(RecruitsAsyncGoal goal){
        this.goals.add(goal);
    }


    public void updateGoals() {
        for (RecruitsAsyncGoal goal : goals) {
            if (goal.canUse() && !activeGoals.contains(goal)) {
                activeGoals.add(goal);
                RecruitsThread.enqueueGoal(goal); // Asynchron starten
            }
        }

        // Entfernen der Goals, die nicht mehr weitergeführt werden sollen
        activeGoals.removeIf(goal -> !goal.canContinueToUse());
    }
}

