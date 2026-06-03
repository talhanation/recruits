package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;

final class ChunkImageBuilder {
    private static final int SAMPLE_GRID_SIZE = 17;
    private static final int SAMPLE_COUNT = SAMPLE_GRID_SIZE * SAMPLE_GRID_SIZE;
    private static final int PIXEL_COUNT = 16 * 16;

    private final ClientLevel level;
    private final int chunkX;
    private final int chunkZ;
    private final long chunkKey;
    private final int minBlockX;
    private final int minBlockZ;
    private final MapSample[][] samples = new MapSample[SAMPLE_GRID_SIZE][SAMPLE_GRID_SIZE];
    private NativeImage image = new NativeImage(NativeImage.Format.RGBA, 16, 16, true);
    private int sampleIndex;
    private int pixelIndex;
    private int meaningfulPixels;

    ChunkImageBuilder(ClientLevel level, int chunkX, int chunkZ, long chunkKey) {
        this.level = level;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkKey = chunkKey;
        this.minBlockX = chunkX << 4;
        this.minBlockZ = chunkZ << 4;
        this.image.untrack();
    }

    boolean workUntil(long deadlineNanos) {
        while (System.nanoTime() < deadlineNanos) {
            if (sampleIndex < SAMPLE_COUNT) {
                sampleNextColumn();
                continue;
            }

            if (pixelIndex < PIXEL_COUNT) {
                renderNextPixel();
                continue;
            }

            return true;
        }

        return isFinished();
    }

    boolean isFinished() {
        return sampleIndex >= SAMPLE_COUNT && pixelIndex >= PIXEL_COUNT;
    }

    boolean isMeaningful() {
        return meaningfulPixels >= 25;
    }

    long chunkKey() {
        return chunkKey;
    }

    int chunkX() {
        return chunkX;
    }

    int chunkZ() {
        return chunkZ;
    }

    ChunkImage takeImage() {
        if (!isFinished() || image == null) return null;

        NativeImage completedImage = image;
        image = null;
        return new ChunkImage(completedImage);
    }

    void close() {
        try {
            if (image != null) image.close();
        } catch (Exception ignored) {
        }
        image = null;
    }

    private void sampleNextColumn() {
        int x = sampleIndex / SAMPLE_GRID_SIZE - 1;
        int z = sampleIndex % SAMPLE_GRID_SIZE - 1;
        samples[x + 1][z + 1] = MapStateSampler.findTopMapSample(level, minBlockX + x, minBlockZ + z);
        sampleIndex++;
    }

    private void renderNextPixel() {
        int x = pixelIndex % 16;
        int z = pixelIndex / 16;
        MapSample sample = samples[x + 1][z + 1];
        int color = 0x00000000;

        if (sample != null) {
            int northHeight = ChunkImage.cachedHeight(samples[x + 1][z], sample.height());
            int northWestHeight = ChunkImage.cachedHeight(samples[x][z], sample.height());
            color = ChunkImage.resolveTerrainColor(level, sample, northHeight, northWestHeight);
        }

        image.setPixelRGBA(x, z, color);
        if (isVisible(color)) meaningfulPixels++;
        pixelIndex++;
    }

    private static boolean isVisible(int color) {
        int alpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        return alpha > 0 && rgb != 0;
    }
}
