package com.talhanation.recruits.client.gui.worldmap;

final class PanInertia {
    private static final double DECAY = 0.9;
    private static final double STOP_THRESHOLD = 0.01;
    private static final double MIN_DRAG_DISTANCE = 3.0;
    private static final double MAX_SPEED_PIXELS = 500.0;
    private static final long DRAG_SAMPLE_INTERVAL_NANOS = 30_000_000L;
    private static final double NANOS_PER_60FPS_FRAME = 16_666_666.666666668;

    private boolean active;
    private long animationStartMillis;
    private double fromOffsetX;
    private double fromOffsetZ;
    private double targetOffsetX;
    private double targetOffsetZ;
    private double previousDragSampleX;
    private double previousDragSampleY;
    private long previousDragSampleNanos;
    private double lastDragSampleX;
    private double lastDragSampleY;
    private long lastDragSampleNanos;
    private double draggedDistance;

    void begin(double mouseX, double mouseY) {
        reset();
        long now = System.nanoTime();
        previousDragSampleX = mouseX;
        previousDragSampleY = mouseY;
        previousDragSampleNanos = now;
        lastDragSampleX = mouseX;
        lastDragSampleY = mouseY;
        lastDragSampleNanos = now;
    }

    void recordDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        draggedDistance += Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        long now = System.nanoTime();
        if (lastDragSampleNanos == 0L || now - lastDragSampleNanos >= DRAG_SAMPLE_INTERVAL_NANOS) {
            previousDragSampleX = lastDragSampleX;
            previousDragSampleY = lastDragSampleY;
            previousDragSampleNanos = lastDragSampleNanos;
            lastDragSampleX = mouseX;
            lastDragSampleY = mouseY;
            lastDragSampleNanos = now;
        }
    }

    boolean finish(double mouseX, double mouseY, double scale, double minScale, double currentOffsetX, double currentOffsetZ) {
        if (lastDragSampleNanos == 0L || draggedDistance < MIN_DRAG_DISTANCE || scale <= 0.0) {
            reset();
            return false;
        }

        long now = System.nanoTime();
        long sampleTime = previousDragSampleNanos != 0L ? previousDragSampleNanos : lastDragSampleNanos;
        double sampleX = previousDragSampleNanos != 0L ? previousDragSampleX : lastDragSampleX;
        double sampleY = previousDragSampleNanos != 0L ? previousDragSampleY : lastDragSampleY;
        double elapsedFrames = Math.max(0.25, (now - sampleTime) / NANOS_PER_60FPS_FRAME);
        double draggedX = mouseX - sampleX;
        double draggedY = mouseY - sampleY;
        double speedX = -draggedX / scale / elapsedFrames;
        double speedZ = -draggedY / scale / elapsedFrames;
        double speed = Math.sqrt(speedX * speedX + speedZ * speedZ);
        if (speed <= 0.0) {
            reset();
            return false;
        }

        double maxSpeed = MAX_SPEED_PIXELS / Math.max(scale, minScale);
        if (speed > maxSpeed) {
            double speedScale = maxSpeed / speed;
            speedX *= speedScale;
            speedZ *= speedScale;
            speed = maxSpeed;
        }

        double moveDistance = -speed / Math.log(DECAY);
        double moveWorldX = speedX / speed * moveDistance;
        double moveWorldZ = speedZ / speed * moveDistance;

        fromOffsetX = currentOffsetX;
        fromOffsetZ = currentOffsetZ;
        targetOffsetX = currentOffsetX - moveWorldX * scale;
        targetOffsetZ = currentOffsetZ - moveWorldZ * scale;
        animationStartMillis = System.currentTimeMillis();
        active = true;
        lastDragSampleNanos = 0L;
        draggedDistance = 0.0;
        return true;
    }

    Offset currentOffset() {
        return new Offset(
                currentAnimatedValue(fromOffsetX, targetOffsetX, animationStartMillis),
                currentAnimatedValue(fromOffsetZ, targetOffsetZ, animationStartMillis)
        );
    }

    boolean isAtTarget(Offset offset) {
        return offset.x() == targetOffsetX && offset.z() == targetOffsetZ;
    }

    boolean isActive() {
        return active;
    }

    double targetOffsetX() {
        return targetOffsetX;
    }

    double targetOffsetZ() {
        return targetOffsetZ;
    }

    void reset() {
        active = false;
        animationStartMillis = 0L;
        previousDragSampleNanos = 0L;
        lastDragSampleNanos = 0L;
        draggedDistance = 0.0;
    }

    private static double currentAnimatedValue(double from, double target, long startMillis) {
        double offset = target - from;
        double times = (System.currentTimeMillis() - startMillis) / 16.666666666666668;
        double currentOffset = offset * Math.pow(DECAY, times);
        return Math.abs(currentOffset) <= STOP_THRESHOLD ? target : target - currentOffset;
    }

    record Offset(double x, double z) {
    }
}
