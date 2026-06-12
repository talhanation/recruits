package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.client.gui.worldmap.color.MapBlockColorResolver;
import com.talhanation.recruits.client.gui.worldmap.color.MapSample;
import com.talhanation.recruits.client.gui.worldmap.color.MapSourceTintSampler;
import com.talhanation.recruits.client.gui.worldmap.color.MapStateClassifier;
import com.talhanation.recruits.client.gui.worldmap.color.MapTerrainColorResolver;
import com.talhanation.recruits.client.gui.worldmap.pipeline.ChunkBuildScratch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Cached samples for one 32x32 chunk region.
 * Resource reload uses this; normal rendering uses WorldMapRegionPixels.
 */
public final class WorldMapSourceRegion {
    private static final int CHUNK_COUNT =
            WorldMapRegion.CHUNKS_PER_REGION * WorldMapRegion.CHUNKS_PER_REGION;
    private static final int PIXEL_COUNT =
            WorldMapRegion.PIXELS_PER_CHUNK * WorldMapRegion.PIXELS_PER_CHUNK;

    private final AtomicReferenceArray<WorldMapSourceChunk> chunks;

    private WorldMapSourceRegion(AtomicReferenceArray<WorldMapSourceChunk> chunks) {
        this.chunks = chunks;
    }

    static WorldMapSourceRegion blank() {
        return new WorldMapSourceRegion(new AtomicReferenceArray<>(CHUNK_COUNT));
    }

    void updateChunk(WorldMapSourceChunk sourceChunk, int chunkXInRegion, int chunkZInRegion) {
        if (sourceChunk == null || !isValidChunk(chunkXInRegion, chunkZInRegion)) return;

        chunks.set(chunkIndex(chunkXInRegion, chunkZInRegion), sourceChunk);
    }

    boolean hasChunkSource(int chunkXInRegion, int chunkZInRegion) {
        return isValidChunk(chunkXInRegion, chunkZInRegion)
                && chunks.get(chunkIndex(chunkXInRegion, chunkZInRegion)) != null;
    }

    WorldMapSourceChunk chunkSource(int chunkXInRegion, int chunkZInRegion) {
        return isValidChunk(chunkXInRegion, chunkZInRegion)
                ? chunks.get(chunkIndex(chunkXInRegion, chunkZInRegion))
                : null;
    }

    boolean hasAnySource() {
        for (int index = 0; index < CHUNK_COUNT; index++) {
            if (chunks.get(index) != null) return true;
        }
        return false;
    }

    WorldMapRegionPixels rebuildPixels(ClientLevel level, int regionX, int regionZ) {
        if (level == null) return null;

        WorldMapRegionPixels pixels = WorldMapRegionPixels.blank();
        for (int chunkZ = 0; chunkZ < WorldMapRegion.CHUNKS_PER_REGION; chunkZ++) {
            for (int chunkX = 0; chunkX < WorldMapRegion.CHUNKS_PER_REGION; chunkX++) {
                if (!hasChunkSource(chunkX, chunkZ)) continue;

                int[] chunkPixels = renderChunk(level, regionX, regionZ, chunkX, chunkZ);
                if (chunkPixels != null) {
                    pixels.updateChunk(chunkPixels, chunkX, chunkZ);
                }
            }
        }
        return pixels;
    }

    Snapshot snapshot() {
        WorldMapSourceChunk[] snapshot = new WorldMapSourceChunk[CHUNK_COUNT];
        for (int index = 0; index < CHUNK_COUNT; index++) {
            snapshot[index] = chunks.get(index);
        }
        return new Snapshot(snapshot);
    }

    private int[] renderChunk(
            ClientLevel level, int regionX, int regionZ, int chunkXInRegion, int chunkZInRegion) {
        MapSample[] surfaces = newSamples();
        MapSample[] underlays = newSamples();
        int chunkWorldX = regionX * WorldMapRegion.CHUNKS_PER_REGION + chunkXInRegion;
        int chunkWorldZ = regionZ * WorldMapRegion.CHUNKS_PER_REGION + chunkZInRegion;
        int minBlockX = chunkWorldX << 4;
        int minBlockZ = chunkWorldZ << 4;

        for (int sampleX = -1; sampleX <= WorldMapRegion.PIXELS_PER_CHUNK; sampleX++) {
            for (int sampleZ = -1; sampleZ <= WorldMapRegion.PIXELS_PER_CHUNK; sampleZ++) {
                int sampleIndex =
                        (sampleX + 1) * ChunkBuildScratch.SAMPLE_GRID_SIZE + sampleZ + 1;
                int regionPixelX = chunkXInRegion * WorldMapRegion.PIXELS_PER_CHUNK + sampleX;
                int regionPixelZ = chunkZInRegion * WorldMapRegion.PIXELS_PER_CHUNK + sampleZ;
                SourcePixel sourcePixel = sourcePixel(regionPixelX, regionPixelZ);
                if (sourcePixel == null) {
                    // No neighbor region here; clamp instead of creating a transparent fringe.
                    sourcePixel = sourcePixel(
                            chunkXInRegion * WorldMapRegion.PIXELS_PER_CHUNK + clampToChunk(sampleX),
                            chunkZInRegion * WorldMapRegion.PIXELS_PER_CHUNK + clampToChunk(sampleZ));
                }
                if (sourcePixel == null) continue;

                fillSample(
                        surfaces[sampleIndex],
                        sourcePixel.surface(),
                        minBlockX + sampleX,
                        minBlockZ + sampleZ);
                fillSample(
                        underlays[sampleIndex],
                        sourcePixel.underlay(),
                        minBlockX + sampleX,
                        minBlockZ + sampleZ);
            }
        }

        MapSourceTintSampler tintSampler =
                new MapSourceTintSampler(
                        level,
                        (x, z) -> biomeIdAtWorld(
                                regionX,
                                regionZ,
                                x,
                                z,
                                chunkXInRegion,
                                chunkZInRegion));
        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
        prepareColors(surfaces, underlays, tintSampler, samplePos);

        int[] pixels = new int[PIXEL_COUNT];
        for (int pixelIndex = 0; pixelIndex < PIXEL_COUNT; pixelIndex++) {
            int x = pixelIndex % WorldMapRegion.PIXELS_PER_CHUNK;
            int z = pixelIndex / WorldMapRegion.PIXELS_PER_CHUNK;
            int sampleCenter = (x + 1) * ChunkBuildScratch.SAMPLE_GRID_SIZE + z + 1;
            MapSample sample = surfaces[sampleCenter];
            int color = 0x00000000;

            if (sample.isPresent()) {
                int northHeight = MapTerrainColorResolver.cachedHeight(surfaces[sampleCenter - 1], sample.height());
                int northWestHeight = MapTerrainColorResolver.cachedHeight(
                        surfaces[sampleCenter - ChunkBuildScratch.SAMPLE_GRID_SIZE - 1],
                        sample.height());
                int southHeight = MapTerrainColorResolver.cachedHeight(surfaces[sampleCenter + 1], sample.height());
                int westHeight = MapTerrainColorResolver.cachedHeight(
                        surfaces[sampleCenter - ChunkBuildScratch.SAMPLE_GRID_SIZE],
                        sample.height());
                int eastHeight = MapTerrainColorResolver.cachedHeight(
                        surfaces[sampleCenter + ChunkBuildScratch.SAMPLE_GRID_SIZE],
                        sample.height());
                color = MapTerrainColorResolver.resolve(
                        level,
                        sample,
                        underlays[sampleCenter],
                        northHeight,
                        northWestHeight,
                        countWaterNeighbors(surfaces, underlays, sampleCenter),
                        Math.abs(westHeight - eastHeight) + Math.abs(northHeight - southHeight));
            }

            pixels[pixelIndex] = color;
        }
        return pixels;
    }

    private void prepareColors(
            MapSample[] surfaces,
            MapSample[] underlays,
            MapSourceTintSampler tintSampler,
            BlockPos.MutableBlockPos samplePos) {
        for (int pixelIndex = 0; pixelIndex < PIXEL_COUNT; pixelIndex++) {
            int x = pixelIndex % WorldMapRegion.PIXELS_PER_CHUNK;
            int z = pixelIndex / WorldMapRegion.PIXELS_PER_CHUNK;
            int sampleCenter = (x + 1) * ChunkBuildScratch.SAMPLE_GRID_SIZE + z + 1;
            prepareColor(surfaces[sampleCenter], tintSampler, samplePos);
            prepareColor(underlays[sampleCenter], tintSampler, samplePos);
        }
    }

    private static void prepareColor(
            MapSample sample, MapSourceTintSampler tintSampler, BlockPos.MutableBlockPos samplePos) {
        if (!sample.isPresent()) return;

        samplePos.set(sample.x(), sample.height(), sample.z());
        sample.setBaseRgb(MapBlockColorResolver.resolveBaseRgb(tintSampler, samplePos, sample.state()));
    }

    private SourcePixel sourcePixel(int regionPixelX, int regionPixelZ) {
        if (regionPixelX < 0
                || regionPixelZ < 0
                || regionPixelX >= WorldMapRegion.REGION_PIXEL_SIZE
                || regionPixelZ >= WorldMapRegion.REGION_PIXEL_SIZE) {
            return null;
        }

        int chunkX = regionPixelX >> 4;
        int chunkZ = regionPixelZ >> 4;
        WorldMapSourceChunk chunk = chunks.get(chunkIndex(chunkX, chunkZ));
        if (chunk == null) return null;

        int pixelIndex = (regionPixelZ & 15) * WorldMapRegion.PIXELS_PER_CHUNK + (regionPixelX & 15);
        return new SourcePixel(chunk.surface(pixelIndex), chunk.underlay(pixelIndex));
    }

    private int biomeIdAtWorld(
            int regionX,
            int regionZ,
            int worldX,
            int worldZ,
            int fallbackChunkXInRegion,
            int fallbackChunkZInRegion) {
        int regionMinBlockX = regionX * WorldMapRegion.CHUNKS_PER_REGION * WorldMapRegion.PIXELS_PER_CHUNK;
        int regionMinBlockZ = regionZ * WorldMapRegion.CHUNKS_PER_REGION * WorldMapRegion.PIXELS_PER_CHUNK;
        SourcePixel pixel = sourcePixel(worldX - regionMinBlockX, worldZ - regionMinBlockZ);
        if (pixel == null) {
            int localX = fallbackChunkXInRegion * WorldMapRegion.PIXELS_PER_CHUNK + clampToChunk(worldX & 15);
            int localZ = fallbackChunkZInRegion * WorldMapRegion.PIXELS_PER_CHUNK + clampToChunk(worldZ & 15);
            pixel = sourcePixel(localX, localZ);
        }
        if (pixel == null) return WorldMapSourceChunk.MISSING;
        if (pixel.surface().isPresent() && pixel.surface().biomeId() >= 0) {
            return pixel.surface().biomeId();
        }
        return pixel.underlay().isPresent() ? pixel.underlay().biomeId() : WorldMapSourceChunk.MISSING;
    }

    private static void fillSample(
            MapSample target, WorldMapSourceChunk.SourceSample source, int worldX, int worldZ) {
        if (!source.isPresent()) return;

        BlockState state = Block.stateById(source.stateId());
        if (state == null || !MapStateClassifier.isRenderable(state)) return;

        target.set(worldX, worldZ, state, source.height());
        target.setTransparentOverlay(MapStateClassifier.isTransparentOverlay(state));
    }

    private static MapSample[] newSamples() {
        MapSample[] samples = new MapSample[ChunkBuildScratch.SAMPLE_COUNT];
        for (int index = 0; index < samples.length; index++) {
            samples[index] = new MapSample();
        }
        return samples;
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

    private static int clampToChunk(int value) {
        return Math.max(0, Math.min(WorldMapRegion.PIXELS_PER_CHUNK - 1, value));
    }

    private static boolean isValidChunk(int chunkX, int chunkZ) {
        return chunkX >= 0
                && chunkX < WorldMapRegion.CHUNKS_PER_REGION
                && chunkZ >= 0
                && chunkZ < WorldMapRegion.CHUNKS_PER_REGION;
    }

    private static int chunkIndex(int chunkX, int chunkZ) {
        return chunkZ * WorldMapRegion.CHUNKS_PER_REGION + chunkX;
    }

    public record Snapshot(WorldMapSourceChunk[] chunks) {}

    private record SourcePixel(
            WorldMapSourceChunk.SourceSample surface,
            WorldMapSourceChunk.SourceSample underlay) {}
}
