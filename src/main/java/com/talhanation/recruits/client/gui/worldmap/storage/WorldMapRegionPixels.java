package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.client.gui.worldmap.render.tile.WorldMapRenderTileKey;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class WorldMapRegionPixels {
    private static final int CHUNK_PIXEL_COUNT =
            WorldMapRegion.PIXELS_PER_CHUNK * WorldMapRegion.PIXELS_PER_CHUNK;
    private static final int CHUNK_COUNT =
            WorldMapRegion.CHUNKS_PER_REGION * WorldMapRegion.CHUNKS_PER_REGION;

    private final AtomicReferenceArray<int[]> chunks;

    private WorldMapRegionPixels(AtomicReferenceArray<int[]> chunks) {
        this.chunks = chunks;
    }

    static WorldMapRegionPixels blank() {
        return new WorldMapRegionPixels(new AtomicReferenceArray<>(CHUNK_COUNT));
    }

    void updateChunk(int[] chunkPixels, int chunkXInRegion, int chunkZInRegion) {
        if (chunkPixels == null
                || chunkPixels.length != CHUNK_PIXEL_COUNT
                || !isValidChunk(chunkXInRegion, chunkZInRegion)) {
            return;
        }
        chunks.set(
                chunkIndex(chunkXInRegion, chunkZInRegion), Arrays.copyOf(chunkPixels, CHUNK_PIXEL_COUNT));
    }

    boolean hasVisiblePixel(int x, int z) {
        if (x < 0
                || x >= WorldMapRegion.REGION_PIXEL_SIZE
                || z < 0
                || z >= WorldMapRegion.REGION_PIXEL_SIZE) {
            return false;
        }
        int[] chunk = chunks.get(chunkIndex(x >> 4, z >> 4));
        return chunk != null && ((chunk[pixelIndex(x, z)] >>> 24) & 0xFF) > 0;
    }

    boolean hasChunkPixels(int chunkX, int chunkZ) {
        return isValidChunk(chunkX, chunkZ) && chunks.get(chunkIndex(chunkX, chunkZ)) != null;
    }

    boolean hasVisibleChunkPixels(int chunkX, int chunkZ) {
        if (!isValidChunk(chunkX, chunkZ)) return false;

        int[] chunk = chunks.get(chunkIndex(chunkX, chunkZ));
        if (chunk == null) return false;
        for (int color : chunk) {
            if ((color >>> 24) != 0) return true;
        }
        return false;
    }

    int[] copyChunkPixels(int chunkX, int chunkZ) {
        if (!isValidChunk(chunkX, chunkZ)) return null;

        int[] chunk = chunks.get(chunkIndex(chunkX, chunkZ));
        return chunk == null ? null : Arrays.copyOf(chunk, CHUNK_PIXEL_COUNT);
    }

    void copyArea(int startX, int startZ, int areaSize, int[] target) {
        if (target == null || target.length < areaSize * areaSize) {
            throw new IllegalArgumentException("Target buffer is too small");
        }

        for (int targetZ = 0; targetZ < areaSize; targetZ++) {
            int sourceZ = startZ + targetZ;
            int chunkZ = sourceZ >> 4;
            int localZ = sourceZ & 15;
            int sourceX = startX;
            int targetOffset = targetZ * areaSize;
            int remaining = areaSize;

            while (remaining > 0) {
                int chunkX = sourceX >> 4;
                int localX = sourceX & 15;
                int copyLength = Math.min(WorldMapRegion.PIXELS_PER_CHUNK - localX, remaining);
                int[] chunk = chunks.get(chunkIndex(chunkX, chunkZ));
                if (chunk == null) {
                    Arrays.fill(target, targetOffset, targetOffset + copyLength, 0);
                } else {
                    System.arraycopy(
                            chunk,
                            localZ * WorldMapRegion.PIXELS_PER_CHUNK + localX,
                            target,
                            targetOffset,
                            copyLength);
                }
                sourceX += copyLength;
                targetOffset += copyLength;
                remaining -= copyLength;
            }
        }
    }

    void copyDownsampledArea(int startX, int startZ, int scale, int[] target) {
        int targetSize = WorldMapRenderTileKey.PIXEL_SIZE;
        if (scale <= 1 || target == null || target.length < targetSize * targetSize) {
            throw new IllegalArgumentException("Invalid downsample target");
        }

        for (int targetZ = 0; targetZ < targetSize; targetZ++) {
            int sourceZ = startZ + targetZ * scale;
            int chunkZ = sourceZ >> 4;
            int localZ = sourceZ & 15;
            int targetRow = targetZ * targetSize;
            for (int targetX = 0; targetX < targetSize; targetX++) {
                int sourceX = startX + targetX * scale;
                int[] chunk = chunks.get(chunkIndex(sourceX >> 4, chunkZ));
                target[targetRow + targetX] =
                        chunk == null ? 0 : averageArea(chunk, sourceX & 15, localZ, scale);
            }
        }
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

    private static int pixelIndex(int x, int z) {
        return (z & 15) * WorldMapRegion.PIXELS_PER_CHUNK + (x & 15);
    }

    private static int averageArea(int[] source, int startX, int startZ, int areaSize) {
        int alphaSum = 0;
        long redSum = 0L;
        long greenSum = 0L;
        long blueSum = 0L;
        for (int z = 0; z < areaSize; z++) {
            int row = (startZ + z) * WorldMapRegion.PIXELS_PER_CHUNK + startX;
            for (int x = 0; x < areaSize; x++) {
                int color = source[row + x];
                int alpha = color >>> 24;
                alphaSum += alpha;
                redSum += (color & 0xFFL) * alpha;
                greenSum += ((color >>> 8) & 0xFFL) * alpha;
                blueSum += ((color >>> 16) & 0xFFL) * alpha;
            }
        }

        if (alphaSum == 0) return 0;
        int sampleCount = areaSize * areaSize;
        int alpha = (alphaSum + sampleCount / 2) / sampleCount;
        int red = (int) ((redSum + alphaSum / 2L) / alphaSum);
        int green = (int) ((greenSum + alphaSum / 2L) / alphaSum);
        int blue = (int) ((blueSum + alphaSum / 2L) / alphaSum);
        return alpha << 24 | blue << 16 | green << 8 | red;
    }
}
