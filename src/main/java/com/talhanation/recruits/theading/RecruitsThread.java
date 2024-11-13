package com.talhanation.recruits.theading;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RecruitsThread implements Runnable {
    private static final BlockingQueue<RecruitsAsyncGoal> goalQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public static void enqueueGoal(RecruitsAsyncGoal goal) {
        goalQueue.add(goal);
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                RecruitsAsyncGoal goal = goalQueue.poll();
                if (goal != null) {
                    if (goal.canContinueToUse()) {
                        goal.tick();
                    } else {
                        goalQueue.remove(goal);
                    }
                }

                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
