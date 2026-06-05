package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.world.level.block.state.BlockState;

final class MapSample {
    private int x;
    private int z;
    private BlockState state;
    private int height;
    private int baseRgb;
    private boolean waterLike;
    private boolean transparentOverlay;

    MapSample() {
    }

    MapSample(int x, int z, BlockState state, int height) {
        set(x, z, state, height);
    }

    MapSample set(int x, int z, BlockState state, int height) {
        this.x = x;
        this.z = z;
        this.state = state;
        this.height = height;
        this.baseRgb = 0;
        this.waterLike = MapStateSampler.isWaterLike(state);
        this.transparentOverlay = false;
        return this;
    }

    void clear() {
        state = null;
        baseRgb = 0;
        waterLike = false;
        transparentOverlay = false;
    }

    boolean isPresent() {
        return state != null;
    }

    int x() {
        return x;
    }

    int z() {
        return z;
    }

    BlockState state() {
        return state;
    }

    int height() {
        return height;
    }

    int baseRgb() {
        return baseRgb;
    }

    void setBaseRgb(int baseRgb) {
        this.baseRgb = baseRgb;
    }

    boolean isWaterLike() {
        return waterLike;
    }

    boolean isTransparentOverlay() {
        return transparentOverlay;
    }

    void setTransparentOverlay(boolean transparentOverlay) {
        this.transparentOverlay = transparentOverlay;
    }
}
