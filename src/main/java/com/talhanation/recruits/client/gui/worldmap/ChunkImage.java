package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkImage {
    private final NativeImage image;

    public ChunkImage(ClientLevel level, ChunkPos pos) {
        this.image = generateChunkImage(level, pos);
    }

    public static int sampleMapColor(ClientLevel level, int worldX, int worldZ) {
        MapSample sample = MapStateSampler.findTopMapSample(level, worldX, worldZ);
        if (sample == null) return 0x00000000;

        int northHeight = sampleHeight(level, worldX, worldZ - 1, sample.height());
        int northWestHeight = sampleHeight(level, worldX - 1, worldZ - 1, sample.height());
        return resolveTerrainColor(level, sample, northHeight, northWestHeight);
    }

    private NativeImage generateChunkImage(ClientLevel level, ChunkPos pos) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, 16, 16, true);
        MapSample[][] samples = loadSampleGrid(level, pos);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                MapSample sample = samples[x + 1][z + 1];
                if (sample == null) {
                    img.setPixelRGBA(x, z, 0x00000000);
                    continue;
                }

                int northHeight = cachedHeight(samples[x + 1][z], sample.height());
                int northWestHeight = cachedHeight(samples[x][z], sample.height());
                img.setPixelRGBA(x, z, resolveTerrainColor(level, sample, northHeight, northWestHeight));
            }
        }
        img.untrack();
        return img;
    }

    private static MapSample[][] loadSampleGrid(ClientLevel level, ChunkPos pos) {
        MapSample[][] samples = new MapSample[17][17];
        int minBlockX = pos.getMinBlockX();
        int minBlockZ = pos.getMinBlockZ();

        for (int x = -1; x < 16; x++) {
            for (int z = -1; z < 16; z++) {
                samples[x + 1][z + 1] = MapStateSampler.findTopMapSample(level, minBlockX + x, minBlockZ + z);
            }
        }

        return samples;
    }

    private static int sampleHeight(ClientLevel level, int worldX, int worldZ, int fallback) {
        MapSample sample = MapStateSampler.findTopMapSample(level, worldX, worldZ);
        return cachedHeight(sample, fallback);
    }

    private static int cachedHeight(MapSample sample, int fallback) {
        return sample != null ? sample.height() : fallback;
    }

    private static int resolveTerrainColor(ClientLevel level, MapSample sample, int northHeight, int northWestHeight) {
        BlockPos pos = sample.pos();
        BlockState state = sample.state();
        if (MapStateSampler.isTransparentOverlay(level, pos, state)) {
            return resolveTransparentOverlayColor(level, sample);
        }

        boolean water = MapStateSampler.isWaterLike(state);
        int rgb = water
                ? MapBlockColorResolver.resolveWaterRgb(level, pos, state)
                : MapBlockColorResolver.resolveBaseRgb(level, pos, state);
        if ((rgb & 0x00FFFFFF) == 0) return 0x00000000;

        if (water) {
            return MapBlockColorResolver.applyBrightnessToNativeColor(
                    rgb,
                    MapReliefShading.computeWaterBrightness(level, pos)
            );
        }

        return MapBlockColorResolver.applyBrightnessToNativeColor(
                rgb,
                MapReliefShading.computeLandBrightness(level, sample, northHeight, northWestHeight)
        );
    }

    public NativeImage getNativeImage() {
        return this.image;
    }

    public boolean isMeaningful() {
        if (this.image == null) return false;
        int meaningful = 0;
        for (int i = 0; i < 256; i++) {
            int pixel = this.image.getPixelRGBA(i % 16, i / 16);
            int alpha = (pixel >> 24) & 0xFF;
            int rgb = pixel & 0x00FFFFFF;
            if (alpha > 0 && rgb != 0) meaningful++;
        }
        return meaningful >= 25; // ~10% von 256
    }

    private static int resolveTransparentOverlayColor(ClientLevel level, MapSample overlaySample) {
        int overlayRgb = MapBlockColorResolver.resolveBaseRgb(level, overlaySample.pos(), overlaySample.state());
        if ((overlayRgb & 0x00FFFFFF) == 0) return 0x00000000;

        MapSample baseSample = MapStateSampler.findUnderOverlaySample(level, overlaySample.pos());
        if (baseSample == null) {
            return MapBlockColorResolver.applyBrightnessToNativeColor(overlayRgb, 1.0f);
        }

        int northHeight = sampleHeight(level, baseSample.pos().getX(), baseSample.pos().getZ() - 1, baseSample.height());
        int northWestHeight = sampleHeight(level, baseSample.pos().getX() - 1, baseSample.pos().getZ() - 1, baseSample.height());
        int baseNative = resolveTerrainColor(level, baseSample, northHeight, northWestHeight);
        if (((baseNative >> 24) & 0xFF) == 0) {
            return MapBlockColorResolver.applyBrightnessToNativeColor(overlayRgb, 1.0f);
        }

        int baseRgb = nativeToRgb(baseNative);
        int blendedRgb = MapBlockColorResolver.blendRgb(
                baseRgb,
                overlayRgb,
                MapStateSampler.getTransparentOverlayAlpha(overlaySample.state())
        );
        return MapBlockColorResolver.applyBrightnessToNativeColor(blendedRgb, 1.0f);
    }

    private static int nativeToRgb(int nativeColor) {
        int red = nativeColor & 0xFF;
        int green = (nativeColor >> 8) & 0xFF;
        int blue = (nativeColor >> 16) & 0xFF;
        return (red << 16) | (green << 8) | blue;
    }

    public void close() {
        try { if (image != null) image.close(); } catch (Exception ignored) {}
    }
}
