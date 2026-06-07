package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.client.gui.worldmap.render.tile.WorldMapRenderTileKey;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

public final class WorldMapRegion {
    public static final int CHUNKS_PER_REGION = 32;
    public static final int PIXELS_PER_CHUNK = 16;
    public static final int REGION_PIXEL_SIZE = CHUNKS_PER_REGION * PIXELS_PER_CHUNK;
    private static final int CHUNK_COUNT = CHUNKS_PER_REGION * CHUNKS_PER_REGION;
    private static final int RENDER_PIXEL_COUNT =
            WorldMapRenderTileKey.PIXEL_SIZE * WorldMapRenderTileKey.PIXEL_SIZE;
    private static final int MAX_POOLED_RENDER_BUFFERS = 64;
    private static final AtomicInteger CONTENT_VERSION_SEQUENCE = new AtomicInteger();
    private static final AtomicInteger POOLED_RENDER_BUFFER_COUNT = new AtomicInteger();
    private static final ConcurrentLinkedQueue<int[]> RENDER_PIXEL_POOL =
            new ConcurrentLinkedQueue<>();

    private final int regionX;
    private final int regionZ;
    private final AtomicIntegerArray[] renderVersions =
            new AtomicIntegerArray[WorldMapRenderTileKey.MAX_LEVEL + 1];
    private final AtomicIntegerArray chunkColorEpochs = new AtomicIntegerArray(CHUNK_COUNT);
    private final AtomicInteger mutationEpoch = new AtomicInteger();
    private volatile WorldMapRegionPixels pixels;
    private int saveSnapshotVersion = -1;
    private boolean dirty;
    private int dirtyVersion;
    private long lastDirtyNanos;
    private long lastAccessOrder;

    WorldMapRegion(int regionX, int regionZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        for (int level = 0; level <= WorldMapRenderTileKey.MAX_LEVEL; level++) {
            int side = tilesPerSide(level);
            renderVersions[level] = new AtomicIntegerArray(side * side);
        }
    }

    synchronized void createBlank(int colorEpoch) {
        this.pixels = WorldMapRegionPixels.blank();
        this.saveSnapshotVersion = -1;
        this.dirty = false;
        this.dirtyVersion = 0;
        resetChunkColorEpochs(colorEpoch);
        resetRenderVersions();
        this.lastDirtyNanos = 0L;
    }

    void loadFromPixels(WorldMapRegionPixels loadedPixels, int colorEpoch) {
        if (loadedPixels == null) return;
        synchronized (this) {
            this.pixels = loadedPixels;
            this.saveSnapshotVersion = -1;
            this.dirty = false;
            this.dirtyVersion = 0;
            resetChunkColorEpochs(colorEpoch);
            resetRenderVersions();
            this.lastDirtyNanos = 0L;
        }
    }

    synchronized void updateFromChunkPixels(
            int[] chunkPixels, int chunkXInRegion, int chunkZInRegion, int colorEpoch) {
        if (this.pixels == null
                || chunkPixels == null
                || chunkPixels.length != PIXELS_PER_CHUNK * PIXELS_PER_CHUNK) {
            return;
        }

        int startX = chunkXInRegion * PIXELS_PER_CHUNK;
        int startZ = chunkZInRegion * PIXELS_PER_CHUNK;
        mutationEpoch.incrementAndGet();
        try {
            this.pixels.updateChunk(chunkPixels, chunkXInRegion, chunkZInRegion);
            this.chunkColorEpochs.set(chunkIndex(chunkXInRegion, chunkZInRegion), colorEpoch);
            this.dirty = true;
            this.dirtyVersion++;
            markRenderTileDirty(
                    startX / WorldMapRenderTileKey.PIXEL_SIZE, startZ / WorldMapRenderTileKey.PIXEL_SIZE);
            this.lastDirtyNanos = System.nanoTime();
        } finally {
            mutationEpoch.incrementAndGet();
        }
    }

    synchronized SaveSnapshot beginSaveSnapshot() {
        if (this.pixels == null || !this.dirty || this.saveSnapshotVersion >= 0) return null;

        this.saveSnapshotVersion = this.dirtyVersion;
        return new SaveSnapshot(this.pixels.snapshot(), this.saveSnapshotVersion);
    }

    synchronized void finishSave(int savedVersion, boolean success) {
        if (savedVersion == this.saveSnapshotVersion) {
            this.saveSnapshotVersion = -1;
        }
        if (success && savedVersion >= this.dirtyVersion) {
            this.dirty = false;
            this.lastDirtyNanos = 0L;
        }
    }

    synchronized boolean isReadyForBackgroundSave(long nowNanos, long quietPeriodNanos) {
        return this.dirty && nowNanos - this.lastDirtyNanos >= quietPeriodNanos;
    }

    public int renderVersion(int level, int tileXInRegion, int tileZInRegion) {
        if (!isValidRenderTile(level, tileXInRegion, tileZInRegion)) return -1;
        return renderVersions[level].get(renderVersionIndex(level, tileXInRegion, tileZInRegion));
    }

    boolean hasVisiblePixel(int x, int z) {
        WorldMapRegionPixels regionPixels = pixels;
        return regionPixels != null && regionPixels.hasVisiblePixel(x, z);
    }

    boolean hasChunkPixels(int chunkXInRegion, int chunkZInRegion) {
        WorldMapRegionPixels regionPixels = pixels;
        return regionPixels != null && regionPixels.hasChunkPixels(chunkXInRegion, chunkZInRegion);
    }

    boolean hasChunkPixelsForColorEpoch(
            int chunkXInRegion, int chunkZInRegion, int colorEpoch) {
        return hasChunkPixels(chunkXInRegion, chunkZInRegion)
                && chunkColorEpochs.get(chunkIndex(chunkXInRegion, chunkZInRegion)) >= colorEpoch;
    }

    public RenderSnapshot createRenderSnapshot(
            int level, int tileXInRegion, int tileZInRegion, int expectedVersion) {
        if (!isValidRenderTile(level, tileXInRegion, tileZInRegion)) return null;

        int sourceScale = 1 << level;
        int sourceSize = WorldMapRenderTileKey.PIXEL_SIZE * sourceScale;
        int sourceStartX = tileXInRegion * sourceSize;
        int sourceStartZ = tileZInRegion * sourceSize;
        int[] renderPixels = acquireRenderPixels();
        WorldMapRegionPixels sourceRegion = pixels;
        int sourceEpoch = mutationEpoch.get();
        try {
            // Retry if pixels changed while copying the snapshot.
            if (sourceRegion == null
                    || (sourceEpoch & 1) != 0
                    || renderVersion(level, tileXInRegion, tileZInRegion) != expectedVersion) {
                releaseRenderPixels(renderPixels);
                return null;
            }
            if (level == 0) {
                sourceRegion.copyArea(sourceStartX, sourceStartZ, sourceSize, renderPixels);
            } else {
                sourceRegion.copyDownsampledArea(sourceStartX, sourceStartZ, sourceScale, renderPixels);
            }

            if (pixels != sourceRegion
                    || mutationEpoch.get() != sourceEpoch
                    || renderVersion(level, tileXInRegion, tileZInRegion) != expectedVersion) {
                releaseRenderPixels(renderPixels);
                return null;
            }
            return new RenderSnapshot(renderPixels, expectedVersion);
        } catch (RuntimeException | Error exception) {
            releaseRenderPixels(renderPixels);
            throw exception;
        }
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    void markAccessed(long accessOrder) {
        this.lastAccessOrder = accessOrder;
    }

    long getLastAccessOrder() {
        return lastAccessOrder;
    }

    synchronized boolean isDirty() {
        return dirty;
    }

    synchronized void close() {
        pixels = null;
        saveSnapshotVersion = -1;
    }

    public static int chunkToRegionCoord(int chunkCoord) {
        return Math.floorDiv(chunkCoord, CHUNKS_PER_REGION);
    }

    public static int chunkLocalCoord(int chunkCoord) {
        return Math.floorMod(chunkCoord, CHUNKS_PER_REGION);
    }

    private static int nextContentVersion() {
        return CONTENT_VERSION_SEQUENCE.updateAndGet(
                version -> version == Integer.MAX_VALUE ? 1 : version + 1);
    }

    private static int[] acquireRenderPixels() {
        int[] pixels = RENDER_PIXEL_POOL.poll();
        if (pixels != null) {
            POOLED_RENDER_BUFFER_COUNT.decrementAndGet();
            return pixels;
        }
        return new int[RENDER_PIXEL_COUNT];
    }

    public static void releaseRenderPixels(int[] pixels) {
        if (pixels == null || pixels.length != RENDER_PIXEL_COUNT) return;

        int pooled = POOLED_RENDER_BUFFER_COUNT.incrementAndGet();
        if (pooled <= MAX_POOLED_RENDER_BUFFERS) {
            RENDER_PIXEL_POOL.offer(pixels);
        } else {
            POOLED_RENDER_BUFFER_COUNT.decrementAndGet();
        }
    }

    public static void prepareRenderPixelPool() {
        while (POOLED_RENDER_BUFFER_COUNT.get() < MAX_POOLED_RENDER_BUFFERS) {
            releaseRenderPixels(new int[RENDER_PIXEL_COUNT]);
        }
    }

    private void resetRenderVersions() {
        int version = nextContentVersion();
        for (AtomicIntegerArray levelVersions : renderVersions) {
            for (int index = 0; index < levelVersions.length(); index++) {
                levelVersions.set(index, version);
            }
        }
    }

    private void resetChunkColorEpochs(int colorEpoch) {
        for (int index = 0; index < CHUNK_COUNT; index++) {
            chunkColorEpochs.set(index, colorEpoch);
        }
    }

    private void markRenderTileDirty(int baseTileX, int baseTileZ) {
        int version = nextContentVersion();
        for (int level = 0; level <= WorldMapRenderTileKey.MAX_LEVEL; level++) {
            int tileX = baseTileX >> level;
            int tileZ = baseTileZ >> level;
            renderVersions[level].set(renderVersionIndex(level, tileX, tileZ), version);
        }
    }

    private static int renderVersionIndex(int level, int tileX, int tileZ) {
        return tileZ * tilesPerSide(level) + tileX;
    }

    private static int chunkIndex(int chunkX, int chunkZ) {
        return chunkZ * CHUNKS_PER_REGION + chunkX;
    }

    private static boolean isValidRenderTile(int level, int tileX, int tileZ) {
        if (level < 0 || level > WorldMapRenderTileKey.MAX_LEVEL) return false;
        int side = tilesPerSide(level);
        return tileX >= 0 && tileX < side && tileZ >= 0 && tileZ < side;
    }

    private static int tilesPerSide(int level) {
        return REGION_PIXEL_SIZE / (WorldMapRenderTileKey.PIXEL_SIZE << level);
    }

    public record SaveSnapshot(WorldMapRegionPixels.Snapshot pixels, int dirtyVersion) {}

    public static final class RenderSnapshot {
        private final AtomicReference<int[]> pixels;
        private final int sourceVersion;

        private RenderSnapshot(int[] pixels, int sourceVersion) {
            this.pixels = new AtomicReference<>(pixels);
            this.sourceVersion = sourceVersion;
        }

        public int[] pixels() {
            return pixels.get();
        }

        public int[] takePixels() {
            return pixels.getAndSet(null);
        }

        public int sourceVersion() {
            return sourceVersion;
        }

        public void release() {
            releaseRenderPixels(pixels.getAndSet(null));
        }
    }
}
