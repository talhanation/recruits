package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

final class ChunkImageBuilder {
    private static final int PIXEL_COUNT = 16 * 16;
    private static final int COLOR_SAMPLE_COUNT = PIXEL_COUNT * 2;
    private static final int MAX_SAMPLE_COLUMNS_PER_ADVANCE = 16;
    private static final int MAX_COLOR_SAMPLES_PER_ADVANCE = 32;
    private static final int MAX_RENDER_PIXELS_PER_ADVANCE = 32;

    private final ChunkSamplingContext context;
    private final int chunkX;
    private final int chunkZ;
    private final long chunkKey;
    private final int minBlockX;
    private final int minBlockZ;
    private final ChunkBuildScratch scratch = ChunkBuildScratch.acquire();
    private final int[] pixels = new int[PIXEL_COUNT];
    private final BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();

    private int sampleIndex;
    private int colorPrepareIndex;
    private int pixelIndex;
    private LevelChunk activeSampleChunk;
    private int activeSampleWorldX;
    private int activeSampleWorldZ;
    private int activeSampleY;
    private int activeSampleMinY;
    private int activeUnderlayChecksLeft;
    private SampleStage sampleStage = SampleStage.SURFACE;
    private ChunkBuildResult result;
    private boolean canceled;
    private boolean scratchReleased;

    private ChunkImageBuilder(ChunkSamplingContext context, int chunkX, int chunkZ, long chunkKey) {
        this.context = context;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkKey = chunkKey;
        this.minBlockX = chunkX << 4;
        this.minBlockZ = chunkZ << 4;
        this.scratch.prepare(context);
    }

    static ChunkImageBuilder begin(ChunkSamplingContext context, int chunkX, int chunkZ, long chunkKey) {
        return new ChunkImageBuilder(context, chunkX, chunkZ, chunkKey);
    }

    void advance(long deadlineNanos) {
        if (result != null || canceled) return;

        WorldMapBuildProfiler.beginChunk(chunkX, chunkZ);
        try {
            long phaseStartNanos = System.nanoTime();
            sampleColumns(deadlineNanos);
            WorldMapBuildProfiler.recordSamplePhase(System.nanoTime() - phaseStartNanos);

            if (sampleIndex < ChunkBuildScratch.SAMPLE_COUNT || System.nanoTime() >= deadlineNanos) return;

            phaseStartNanos = System.nanoTime();
            prepareColors(deadlineNanos);
            WorldMapBuildProfiler.recordColorPreparePhase(System.nanoTime() - phaseStartNanos);

            if (colorPrepareIndex < COLOR_SAMPLE_COUNT || System.nanoTime() >= deadlineNanos) return;

            phaseStartNanos = System.nanoTime();
            renderPixels(deadlineNanos);
            WorldMapBuildProfiler.recordRenderPhase(System.nanoTime() - phaseStartNanos);

            if (pixelIndex >= PIXEL_COUNT) {
                result = new ChunkBuildResult(chunkX, chunkZ, chunkKey, pixels);
                releaseScratch();
            }
        } finally {
            WorldMapBuildProfiler.finishChunk();
        }
    }

    boolean isDone() {
        return result != null;
    }

    void cancel() {
        canceled = true;
        activeSampleChunk = null;
        releaseScratch();
    }

    ChunkBuildResult result() {
        return result;
    }

    private void releaseScratch() {
        if (scratchReleased) return;
        scratchReleased = true;
        scratch.release();
    }

    private void sampleColumns(long deadlineNanos) {
        MapSample[] surfaces = scratch.surfaceSamples();
        MapSample[] underlays = scratch.underlaySamples();
        int sampledColumns = 0;
        while (sampleIndex < ChunkBuildScratch.SAMPLE_COUNT
                && sampledColumns < MAX_SAMPLE_COLUMNS_PER_ADVANCE) {
            int sampleIndexBefore = sampleIndex;
            if (activeSampleChunk == null && !beginSampleColumn(surfaces[sampleIndex], underlays[sampleIndex])) {
                sampleIndex++;
                sampledColumns++;
                if (System.nanoTime() >= deadlineNanos) return;
                continue;
            }

            while (activeSampleY >= activeSampleMinY) {
                samplePos.set(activeSampleWorldX, activeSampleY, activeSampleWorldZ);
                BlockState state = activeSampleChunk.getBlockState(samplePos);
                if (sampleStage == SampleStage.SURFACE) {
                    if (MapStateSampler.isRenderableMapState(context.level(), samplePos, state)) {
                        MapSample surface = surfaces[sampleIndex];
                        surface.set(activeSampleWorldX, activeSampleWorldZ, state, activeSampleY);
                        if (!beginUnderlayScan(surface)) {
                            finishSampleColumn();
                            break;
                        }
                    } else {
                        activeSampleY--;
                    }
                } else {
                    if (isUnderlayCandidate(state)) {
                        underlays[sampleIndex].set(activeSampleWorldX, activeSampleWorldZ, state, activeSampleY);
                        finishSampleColumn();
                        break;
                    }
                    activeSampleY--;
                    activeUnderlayChecksLeft--;
                    if (activeUnderlayChecksLeft <= 0) {
                        finishSampleColumn();
                        break;
                    }
                }
                if (System.nanoTime() >= deadlineNanos) return;
            }

            if (activeSampleChunk != null && activeSampleY < activeSampleMinY) {
                finishSampleColumn();
            }
            if (sampleIndex > sampleIndexBefore) {
                sampledColumns += sampleIndex - sampleIndexBefore;
            }
            if (System.nanoTime() >= deadlineNanos) return;
        }
    }

    private boolean beginSampleColumn(MapSample surface, MapSample underlay) {
        surface.clear();
        underlay.clear();
        sampleStage = SampleStage.SURFACE;
        activeUnderlayChecksLeft = 0;
        int x = sampleIndex / ChunkBuildScratch.SAMPLE_GRID_SIZE - 1;
        int z = sampleIndex % ChunkBuildScratch.SAMPLE_GRID_SIZE - 1;
        activeSampleWorldX = minBlockX + x;
        activeSampleWorldZ = minBlockZ + z;
        activeSampleChunk = context.getLoadedChunk(activeSampleWorldX, activeSampleWorldZ);
        if (activeSampleChunk == null) return false;

        int surfaceHeight = activeSampleChunk.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                activeSampleWorldX & 15,
                activeSampleWorldZ & 15
        ) - 1;
        activeSampleMinY = context.level().getMinBuildHeight();
        activeSampleY = Math.min(context.level().getMaxBuildHeight() - 1, surfaceHeight + 3);
        if (activeSampleY >= activeSampleMinY) return true;

        activeSampleChunk = null;
        return false;
    }

    private boolean beginUnderlayScan(MapSample surface) {
        if (surface.isWaterLike()) {
            sampleStage = SampleStage.WATER_UNDERLAY;
            activeUnderlayChecksLeft = 16;
        } else if (MapStateSampler.isTransparentOverlay(context.level(), samplePos, surface.state())) {
            surface.setTransparentOverlay(true);
            sampleStage = SampleStage.TRANSPARENT_UNDERLAY;
            activeUnderlayChecksLeft = 32;
        } else {
            return false;
        }

        activeSampleY--;
        return activeSampleY >= activeSampleMinY;
    }

    private boolean isUnderlayCandidate(BlockState state) {
        if (sampleStage == SampleStage.WATER_UNDERLAY) {
            return !state.isAir()
                    && !MapStateSampler.isWaterLike(state)
                    && MapStateSampler.isRenderableMapState(context.level(), samplePos, state);
        }
        return !state.isAir()
                && !MapStateSampler.isTransparentOverlay(context.level(), samplePos, state)
                && (MapStateSampler.isWaterLike(state)
                || MapStateSampler.isRenderableMapState(context.level(), samplePos, state));
    }

    private void finishSampleColumn() {
        activeSampleChunk = null;
        sampleStage = SampleStage.SURFACE;
        activeUnderlayChecksLeft = 0;
        sampleIndex++;
    }

    private void renderPixels(long deadlineNanos) {
        MapSample[] surfaces = scratch.surfaceSamples();
        MapSample[] underlays = scratch.underlaySamples();
        int renderedPixels = 0;
        while (pixelIndex < PIXEL_COUNT && renderedPixels < MAX_RENDER_PIXELS_PER_ADVANCE) {
            int x = pixelIndex % 16;
            int z = pixelIndex / 16;
            int sampleCenter = (x + 1) * ChunkBuildScratch.SAMPLE_GRID_SIZE + z + 1;
            MapSample sample = surfaces[sampleCenter];
            int color = 0x00000000;

            if (sample.isPresent()) {
                int northHeight = MapTerrainColorResolver.cachedHeight(surfaces[sampleCenter - 1], sample.height());
                int northWestHeight = MapTerrainColorResolver.cachedHeight(
                        surfaces[sampleCenter - ChunkBuildScratch.SAMPLE_GRID_SIZE - 1],
                        sample.height()
                );
                int southHeight = MapTerrainColorResolver.cachedHeight(surfaces[sampleCenter + 1], sample.height());
                int westHeight = MapTerrainColorResolver.cachedHeight(
                        surfaces[sampleCenter - ChunkBuildScratch.SAMPLE_GRID_SIZE],
                        sample.height()
                );
                int eastHeight = MapTerrainColorResolver.cachedHeight(
                        surfaces[sampleCenter + ChunkBuildScratch.SAMPLE_GRID_SIZE],
                        sample.height()
                );
                color = MapTerrainColorResolver.resolve(
                        context.level(),
                        sample,
                        underlays[sampleCenter],
                        northHeight,
                        northWestHeight,
                        countWaterNeighbors(surfaces, underlays, sampleCenter),
                        Math.abs(westHeight - eastHeight) + Math.abs(northHeight - southHeight)
                );
            }

            pixels[pixelIndex] = color;
            pixelIndex++;
            renderedPixels++;
            if (System.nanoTime() >= deadlineNanos) return;
        }
    }

    private void prepareColors(long deadlineNanos) {
        MapSample[] surfaces = scratch.surfaceSamples();
        MapSample[] underlays = scratch.underlaySamples();
        int preparedColors = 0;
        while (colorPrepareIndex < COLOR_SAMPLE_COUNT && preparedColors < MAX_COLOR_SAMPLES_PER_ADVANCE) {
            int pixel = colorPrepareIndex % PIXEL_COUNT;
            int x = pixel % 16;
            int z = pixel / 16;
            int sampleCenter = (x + 1) * ChunkBuildScratch.SAMPLE_GRID_SIZE + z + 1;
            MapSample sample = colorPrepareIndex < PIXEL_COUNT
                    ? surfaces[sampleCenter]
                    : underlays[sampleCenter];
            if (sample.isPresent()) {
                samplePos.set(sample.x(), sample.height(), sample.z());
                sample.setBaseRgb(MapBlockColorResolver.resolveBaseRgb(
                        context,
                        samplePos,
                        sample.state(),
                        scratch.tintSampler()
                ));
            }
            colorPrepareIndex++;
            preparedColors++;
            if (System.nanoTime() >= deadlineNanos) return;
        }
    }

    private static int countWaterNeighbors(MapSample[] surfaces, MapSample[] underlays, int center) {
        int count = 0;
        if (isDisplayedWater(surfaces, underlays, center - 1)) count++;
        if (isDisplayedWater(surfaces, underlays, center + 1)) count++;
        if (isDisplayedWater(surfaces, underlays, center - ChunkBuildScratch.SAMPLE_GRID_SIZE)) count++;
        if (isDisplayedWater(surfaces, underlays, center + ChunkBuildScratch.SAMPLE_GRID_SIZE)) count++;
        return count;
    }

    private static boolean isDisplayedWater(MapSample[] surfaces, MapSample[] underlays, int index) {
        MapSample surface = surfaces[index];
        return surface.isWaterLike()
                || surface.isTransparentOverlay() && underlays[index].isWaterLike();
    }

    private enum SampleStage {
        SURFACE,
        WATER_UNDERLAY,
        TRANSPARENT_UNDERLAY
    }
}
