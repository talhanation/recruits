package com.talhanation.recruits.client.gui.worldmap.pipeline;

import com.talhanation.recruits.client.gui.worldmap.color.MapBlockColorResolver;
import com.talhanation.recruits.client.gui.worldmap.color.MapSample;
import com.talhanation.recruits.client.gui.worldmap.color.MapStateClassifier;
import com.talhanation.recruits.client.gui.worldmap.color.MapTerrainColorResolver;
import com.talhanation.recruits.client.gui.worldmap.debug.WorldMapBuildProfiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** Builds one 16x16 map chunk. */
public final class ChunkImageBuilder {
    private static final int PIXEL_COUNT = 16 * 16;
    private static final int COLOR_SAMPLE_COUNT = PIXEL_COUNT * 2;

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
    private volatile boolean canceled;
    private volatile boolean building;
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

    public static ChunkImageBuilder begin(
            ChunkSamplingContext context, int chunkX, int chunkZ, long chunkKey) {
        return new ChunkImageBuilder(context, chunkX, chunkZ, chunkKey);
    }

    public ChunkBuildResult buildFully() {
        if (canceled) return null;
        building = true;
        WorldMapBuildProfiler.beginChunk(chunkX, chunkZ);
        try {
            long phaseStartNanos = System.nanoTime();
            sampleColumns();
            WorldMapBuildProfiler.recordSamplePhase(System.nanoTime() - phaseStartNanos);
            if (canceled || sampleIndex < ChunkBuildScratch.SAMPLE_COUNT) return null;

            phaseStartNanos = System.nanoTime();
            prepareColors();
            WorldMapBuildProfiler.recordColorPreparePhase(System.nanoTime() - phaseStartNanos);
            if (canceled || colorPrepareIndex < COLOR_SAMPLE_COUNT) return null;

            phaseStartNanos = System.nanoTime();
            renderPixels();
            WorldMapBuildProfiler.recordRenderPhase(System.nanoTime() - phaseStartNanos);
            if (canceled || pixelIndex < PIXEL_COUNT) return null;

            return new ChunkBuildResult(chunkX, chunkZ, chunkKey, pixels);
        } finally {
            building = false;
            activeSampleChunk = null;
            releaseScratch();
            WorldMapBuildProfiler.finishChunk();
        }
    }

    public void cancel() {
        canceled = true;
        if (!building) {
            activeSampleChunk = null;
            releaseScratch();
        }
    }

    private synchronized void releaseScratch() {
        if (scratchReleased) return;
        scratchReleased = true;
        scratch.release();
    }

    private void sampleColumns() {
        MapSample[] surfaces = scratch.surfaceSamples();
        MapSample[] underlays = scratch.underlaySamples();
        // Extra border samples are used for relief and water edges.
        while (sampleIndex < ChunkBuildScratch.SAMPLE_COUNT && !canceled) {
            if (activeSampleChunk == null
                    && !beginSampleColumn(surfaces[sampleIndex], underlays[sampleIndex])) {
                sampleIndex++;
                continue;
            }

            while (activeSampleY >= activeSampleMinY) {
                samplePos.set(activeSampleWorldX, activeSampleY, activeSampleWorldZ);
                BlockState state = activeSampleChunk.getBlockState(samplePos);
                if (sampleStage == SampleStage.SURFACE) {
                    if (MapStateClassifier.isRenderable(state)) {
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
                        underlays[sampleIndex].set(
                                activeSampleWorldX, activeSampleWorldZ, state, activeSampleY);
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
                if (canceled) return;
            }

            if (activeSampleChunk != null && activeSampleY < activeSampleMinY) {
                finishSampleColumn();
            }
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
        if (activeSampleChunk == null) {
            activeSampleWorldX = minBlockX + Math.max(0, Math.min(15, x));
            activeSampleWorldZ = minBlockZ + Math.max(0, Math.min(15, z));
            activeSampleChunk = context.getLoadedChunk(activeSampleWorldX, activeSampleWorldZ);
            if (activeSampleChunk == null) return false;
        }

        int surfaceHeight =
                activeSampleChunk.getHeight(
                                Heightmap.Types.WORLD_SURFACE, activeSampleWorldX & 15, activeSampleWorldZ & 15)
                        - 1;
        activeSampleMinY = context.level().getMinBuildHeight();
        activeSampleY = Math.min(context.level().getMaxBuildHeight() - 1, surfaceHeight + 3);
        if (activeSampleY >= activeSampleMinY) return true;

        activeSampleChunk = null;
        return false;
    }

    private boolean beginUnderlayScan(MapSample surface) {
        // Water and transparent blocks use a lower sample for blending.
        if (surface.isWaterLike()) {
            sampleStage = SampleStage.WATER_UNDERLAY;
            activeUnderlayChecksLeft = 16;
        } else if (MapStateClassifier.isTransparentOverlay(surface.state())) {
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
                    && !MapStateClassifier.isWaterLike(state)
                    && MapStateClassifier.isRenderable(state);
        }
        return !state.isAir()
                && !MapStateClassifier.isTransparentOverlay(state)
                && (MapStateClassifier.isWaterLike(state) || MapStateClassifier.isRenderable(state));
    }

    private void finishSampleColumn() {
        activeSampleChunk = null;
        sampleStage = SampleStage.SURFACE;
        activeUnderlayChecksLeft = 0;
        sampleIndex++;
    }

    private void renderPixels() {
        MapSample[] surfaces = scratch.surfaceSamples();
        MapSample[] underlays = scratch.underlaySamples();
        while (pixelIndex < PIXEL_COUNT && !canceled) {
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
        }
    }

    private void prepareColors() {
        while (colorPrepareIndex < COLOR_SAMPLE_COUNT && !canceled) {
            MapSample sample = colorSample(colorPrepareIndex);
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
        }
    }

    private MapSample colorSample(int index) {
        int pixel = index % PIXEL_COUNT;
        int x = pixel % 16;
        int z = pixel / 16;
        int sampleCenter = (x + 1) * ChunkBuildScratch.SAMPLE_GRID_SIZE + z + 1;
        return index < PIXEL_COUNT
                ? scratch.surfaceSamples()[sampleCenter]
                : scratch.underlaySamples()[sampleCenter];
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
