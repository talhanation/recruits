package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.client.gui.worldmap.render.tile.WorldMapRenderTileKey;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class WorldMapRegionPixels {
    private static final int CHUNK_PIXEL_COUNT =
            WorldMapRegion.PIXELS_PER_CHUNK * WorldMapRegion.PIXELS_PER_CHUNK;
    private static final int CHUNK_COUNT =
            WorldMapRegion.CHUNKS_PER_REGION * WorldMapRegion.CHUNKS_PER_REGION;
    private static final int[] EMPTY_CHUNK = new int[CHUNK_PIXEL_COUNT];

    private final AtomicReferenceArray<int[]> chunks;

    private WorldMapRegionPixels(AtomicReferenceArray<int[]> chunks) {
        this.chunks = chunks;
    }

    static WorldMapRegionPixels blank() {
        return new WorldMapRegionPixels(new AtomicReferenceArray<>(CHUNK_COUNT));
    }

    static WorldMapRegionPixels fromPackedPixels(int[] packedPixels) {
        int baseSize = WorldMapRegion.REGION_PIXEL_SIZE;
        if (packedPixels == null || packedPixels.length != baseSize * baseSize) return null;

        WorldMapRegionPixels regionPixels = blank();
        for (int chunkZ = 0; chunkZ < WorldMapRegion.CHUNKS_PER_REGION; chunkZ++) {
            for (int chunkX = 0; chunkX < WorldMapRegion.CHUNKS_PER_REGION; chunkX++) {
                if (!hasVisiblePixels(packedPixels, baseSize, chunkX, chunkZ)) continue;

                int[] chunkPixels = new int[CHUNK_PIXEL_COUNT];
                for (int localZ = 0; localZ < WorldMapRegion.PIXELS_PER_CHUNK; localZ++) {
                    int sourceOffset =
                            (chunkZ * WorldMapRegion.PIXELS_PER_CHUNK + localZ) * baseSize
                                    + chunkX * WorldMapRegion.PIXELS_PER_CHUNK;
                    int targetOffset = localZ * WorldMapRegion.PIXELS_PER_CHUNK;
                    System.arraycopy(
                            packedPixels,
                            sourceOffset,
                            chunkPixels,
                            targetOffset,
                            WorldMapRegion.PIXELS_PER_CHUNK);
                }
                regionPixels.chunks.set(chunkIndex(chunkX, chunkZ), chunkPixels);
            }
        }
        return regionPixels;
    }

    static WorldMapRegionPixels fromStorage(IntBuffer source) {
        int baseSize = WorldMapRegion.REGION_PIXEL_SIZE;
        if (source == null || source.remaining() != baseSize * baseSize) return null;

        WorldMapRegionPixels regionPixels = blank();
        int sourceStart = source.position();
        for (int chunkZ = 0; chunkZ < WorldMapRegion.CHUNKS_PER_REGION; chunkZ++) {
            for (int chunkX = 0; chunkX < WorldMapRegion.CHUNKS_PER_REGION; chunkX++) {
                if (!hasVisiblePixels(source, sourceStart, baseSize, chunkX, chunkZ)) continue;

                int[] chunkPixels = new int[CHUNK_PIXEL_COUNT];
                for (int localZ = 0; localZ < WorldMapRegion.PIXELS_PER_CHUNK; localZ++) {
                    int sourceOffset =
                            sourceStart
                                    + (chunkZ * WorldMapRegion.PIXELS_PER_CHUNK + localZ) * baseSize
                                    + chunkX * WorldMapRegion.PIXELS_PER_CHUNK;
                    source.get(
                            sourceOffset,
                            chunkPixels,
                            localZ * WorldMapRegion.PIXELS_PER_CHUNK,
                            WorldMapRegion.PIXELS_PER_CHUNK);
                }
                regionPixels.chunks.set(chunkIndex(chunkX, chunkZ), chunkPixels);
            }
        }
        return regionPixels;
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

    Snapshot snapshot() {
        int[][] chunkSnapshot = new int[CHUNK_COUNT][];
        for (int index = 0; index < CHUNK_COUNT; index++) {
            chunkSnapshot[index] = chunks.get(index);
        }
        return new Snapshot(chunkSnapshot);
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

    private static boolean hasVisiblePixels(int[] source, int sourceWidth, int chunkX, int chunkZ) {
        int startX = chunkX * WorldMapRegion.PIXELS_PER_CHUNK;
        int startZ = chunkZ * WorldMapRegion.PIXELS_PER_CHUNK;
        for (int localZ = 0; localZ < WorldMapRegion.PIXELS_PER_CHUNK; localZ++) {
            int row = (startZ + localZ) * sourceWidth + startX;
            for (int localX = 0; localX < WorldMapRegion.PIXELS_PER_CHUNK; localX++) {
                if (source[row + localX] != 0) return true;
            }
        }
        return false;
    }

    private static boolean hasVisiblePixels(
            IntBuffer source, int sourceStart, int sourceWidth, int chunkX, int chunkZ) {
        int startX = chunkX * WorldMapRegion.PIXELS_PER_CHUNK;
        int startZ = chunkZ * WorldMapRegion.PIXELS_PER_CHUNK;
        for (int localZ = 0; localZ < WorldMapRegion.PIXELS_PER_CHUNK; localZ++) {
            int row = sourceStart + (startZ + localZ) * sourceWidth + startX;
            for (int localX = 0; localX < WorldMapRegion.PIXELS_PER_CHUNK; localX++) {
                if (source.get(row + localX) != 0) return true;
            }
        }
        return false;
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

    public static final class Snapshot {
        private final int[][] chunks;

        private Snapshot(int[][] chunks) {
            this.chunks = chunks;
        }

        public void writeTo(IntBuffer target) {
            for (int pixelZ = 0; pixelZ < WorldMapRegion.REGION_PIXEL_SIZE; pixelZ++) {
                int chunkZ = pixelZ >> 4;
                int localZ = pixelZ & 15;
                int sourceOffset = localZ * WorldMapRegion.PIXELS_PER_CHUNK;
                for (int chunkX = 0; chunkX < WorldMapRegion.CHUNKS_PER_REGION; chunkX++) {
                    int[] chunk = chunks[chunkIndex(chunkX, chunkZ)];
                    target.put(
                            chunk == null ? EMPTY_CHUNK : chunk, sourceOffset, WorldMapRegion.PIXELS_PER_CHUNK);
                }
            }
        }
    }
}
