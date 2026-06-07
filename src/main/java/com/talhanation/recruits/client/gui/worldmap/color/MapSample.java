package com.talhanation.recruits.client.gui.worldmap.color;

import net.minecraft.world.level.block.state.BlockState;

public final class MapSample {
    private int x;
    private int z;
    private BlockState state;
    private int height;
    private int baseRgb;
    private boolean waterLike;
    private boolean transparentOverlay;

    public MapSample() {}

    public MapSample(int x, int z, BlockState state, int height) {
        set(x, z, state, height);
    }

    public MapSample set(int x, int z, BlockState state, int height) {
        this.x = x;
        this.z = z;
        this.state = state;
        this.height = height;
        this.baseRgb = 0;
        this.waterLike = MapStateClassifier.isWaterLike(state);
        this.transparentOverlay = false;
        return this;
    }

    public void clear() {
        state = null;
        baseRgb = 0;
        waterLike = false;
        transparentOverlay = false;
    }

    public boolean isPresent() {
        return state != null;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public BlockState state() {
        return state;
    }

    public int height() {
        return height;
    }

    public int baseRgb() {
        return baseRgb;
    }

    public void setBaseRgb(int baseRgb) {
        this.baseRgb = baseRgb;
    }

    public boolean isWaterLike() {
        return waterLike;
    }

    public boolean isTransparentOverlay() {
        return transparentOverlay;
    }

    public void setTransparentOverlay(boolean transparentOverlay) {
        this.transparentOverlay = transparentOverlay;
    }
}
