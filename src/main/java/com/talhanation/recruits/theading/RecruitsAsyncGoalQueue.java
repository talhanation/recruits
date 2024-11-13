package com.talhanation.recruits.theading;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RecruitsAsyncGoalQueue {
    private static final ConcurrentLinkedQueue<RecruitsAsyncGoal> queue = new ConcurrentLinkedQueue<>();

    public static void addGoal(RecruitsAsyncGoal goal) {
        queue.add(goal);
    }

    public static RecruitsAsyncGoal poll() {
        return queue.poll();
    }
}
