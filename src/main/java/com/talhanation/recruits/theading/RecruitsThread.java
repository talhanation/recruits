package com.talhanation.recruits.theading;

public class RecruitsThread extends Thread {
    private static RecruitsThread instance;
    private boolean running;

    public static synchronized RecruitsThread getInstance() {
        if (instance == null) {
            instance = new RecruitsThread();

        }
        return instance;
    }

    public void start(){
        running = true;
    }

    private RecruitsThread() {
        super("RecruitsThread");
    }

    @Override
    public void run() {
        while (running) {
            try {
                RecruitsAsyncGoal nextGoal = RecruitsAsyncGoalQueue.poll();
                if (nextGoal != null) {
                    nextGoal.execute();
                }
                Thread.sleep(10);  // Kurze Pause
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        running = false;
    }
}




