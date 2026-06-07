package com.talhanation.recruits.client.gui.worldmap.debug;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.worldmap.render.MapFramebufferPass;
import com.talhanation.recruits.client.gui.worldmap.render.tile.WorldMapRenderTileKey;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegion;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldMapDebugProfiler {
    private static final long RENDER_SPIKE_NANOS = 40_000_000L;
    private static final long TILE_RENDER_SPIKE_NANOS = 24_000_000L;
    private static final long FRAMEBUFFER_SPIKE_NANOS = 8_000_000L;
    private static final long TEXTURE_UPLOAD_SPIKE_NANOS = 8_000_000L;
    private static final long TILE_UPDATE_SPIKE_NANOS = 16_000_000L;
    private static final long SINGLE_TILE_DRAW_SPIKE_NANOS = 8_000_000L;
    private static final long ASYNC_RUN_SPIKE_NANOS = 40_000_000L;
    private static final long ASYNC_QUEUE_SPIKE_NANOS = 80_000_000L;
    private static final long DETAIL_SAMPLE_INTERVAL_NANOS = 1_000_000_000L;
    private static final long TILE_UPDATE_ALLOCATION_SPIKE_BYTES = 8L * 1024L * 1024L;
    private static final long RENDER_ALLOCATION_SPIKE_BYTES = 4L * 1024L * 1024L;
    private static final long LOG_COOLDOWN_NANOS = 2_000_000_000L;
    private static final long COVERAGE_LOG_COOLDOWN_NANOS = 10_000_000_000L;
    private static final double MEMORY_PRESSURE_RATIO = 0.85;
    private static final int HIGH_PENDING_REGION_LOADS = 24;
    private static final int HIGH_PENDING_LOD_TILES = 64;
    private static final int HIGH_PENDING_REGION_SAVES = 8;
    private static final int HIGH_MISSING_TILE_COUNT = 48;
    private static final long HIGH_ESTIMATED_TILE_IMAGE_MB = 512L;

    private static final Metrics CURRENT = new Metrics();
    private static volatile HudStats lastHudStats = HudStats.EMPTY;
    private static final Map<String, Long> LAST_ASYNC_LOG_NANOS = new ConcurrentHashMap<>();
    private static final List<GarbageCollectorMXBean> GC_COLLECTORS =
            ManagementFactory.getGarbageCollectorMXBeans();
    private static final com.sun.management.ThreadMXBean THREAD_ALLOCATION_BEAN =
            createThreadAllocationBean();

    private static long lastRenderLogNanos;
    private static long lastCoverageLogNanos;
    private static long lastTileUpdateLogNanos;
    private static long nextRenderDetailSampleNanos;
    private static long nextTileUpdateDetailSampleNanos;
    private static long lastTileUpdateNanos;
    private static long lastConsumeLoadsNanos;
    private static long lastEnqueueNanos;
    private static long lastProcessChunksNanos;
    private static long lastConsumeChunksNanos;
    private static long lastScheduleChunksNanos;
    private static long lastSaveNanos;
    private static long lastTrimNanos;
    private static int lastChunkUpdates;
    private static int lastChunkQueueSize;
    private static int lastQueuedChunkCount;
    private static long lastChunkWorkNanos;
    private static long lastChunkFinalizeNanos;
    private static long lastChunkRegionWriteNanos;
    private static long lastChunkLodInvalidateNanos;
    private static long tileUpdateChunkWorkNanos;
    private static long tileUpdateChunkFinalizeNanos;
    private static long tileUpdateChunkRegionWriteNanos;
    private static long tileUpdateChunkLodInvalidateNanos;
    private static long lastTileUpdateGcCount;
    private static long lastTileUpdateGcMillis;
    private static long lastTileUpdateAllocatedBytes;
    private static GcSnapshot tileUpdateGcStart;
    private static long tileUpdateAllocationStart = -1L;

    private WorldMapDebugProfiler() {}

    public static void beginTileUpdate() {
        long now = System.nanoTime();
        boolean detailedSample = now >= nextTileUpdateDetailSampleNanos;
        if (detailedSample) {
            nextTileUpdateDetailSampleNanos = now + DETAIL_SAMPLE_INTERVAL_NANOS;
            tileUpdateGcStart = GcSnapshot.capture();
            tileUpdateAllocationStart = currentThreadAllocatedBytes();
        } else {
            tileUpdateGcStart = null;
            tileUpdateAllocationStart = -1L;
        }
        tileUpdateChunkWorkNanos = 0L;
        tileUpdateChunkFinalizeNanos = 0L;
        tileUpdateChunkRegionWriteNanos = 0L;
        tileUpdateChunkLodInvalidateNanos = 0L;
    }

    public static void beginMapRender(int screenWidth, int screenHeight, double scale) {
        long now = System.nanoTime();
        CURRENT.reset();
        CURRENT.frameStartNanos = now;
        CURRENT.detailedSample = now >= nextRenderDetailSampleNanos;
        if (CURRENT.detailedSample) {
            nextRenderDetailSampleNanos = now + DETAIL_SAMPLE_INTERVAL_NANOS;
            CURRENT.gcStart = GcSnapshot.capture();
            CURRENT.renderAllocationStart = currentThreadAllocatedBytes();
        }
        CURRENT.screenWidth = screenWidth;
        CURRENT.screenHeight = screenHeight;
        CURRENT.scale = scale;
        CURRENT.tileUpdateNanos = lastTileUpdateNanos;
        CURRENT.consumeLoadsNanos = lastConsumeLoadsNanos;
        CURRENT.enqueueNanos = lastEnqueueNanos;
        CURRENT.processChunksNanos = lastProcessChunksNanos;
        CURRENT.consumeChunksNanos = lastConsumeChunksNanos;
        CURRENT.scheduleChunksNanos = lastScheduleChunksNanos;
        CURRENT.saveNanos = lastSaveNanos;
        CURRENT.trimNanos = lastTrimNanos;
        CURRENT.chunkUpdates = lastChunkUpdates;
        CURRENT.chunkQueueSize = lastChunkQueueSize;
        CURRENT.queuedChunkCount = lastQueuedChunkCount;
        CURRENT.chunkWorkNanos = lastChunkWorkNanos;
        CURRENT.chunkFinalizeNanos = lastChunkFinalizeNanos;
        CURRENT.chunkRegionWriteNanos = lastChunkRegionWriteNanos;
        CURRENT.chunkLodInvalidateNanos = lastChunkLodInvalidateNanos;
        CURRENT.tileUpdateGcCount = lastTileUpdateGcCount;
        CURRENT.tileUpdateGcMillis = lastTileUpdateGcMillis;
        CURRENT.tileUpdateAllocatedBytes = lastTileUpdateAllocatedBytes;
    }

    public static boolean measureRenderDetails() {
        return CURRENT.detailedSample;
    }

    public static void recordFramebuffer(MapFramebufferPass.Frame frame) {
        CURRENT.fboScale = frame.fboScale();
        CURRENT.secondaryScale = frame.secondaryScale();
        CURRENT.leftWorld = frame.leftWorld();
        CURRENT.topWorld = frame.topWorld();
        CURRENT.rightWorld = frame.rightWorld();
        CURRENT.bottomWorld = frame.bottomWorld();
    }

    public static void recordFramebufferBegin(long nanos) {
        CURRENT.framebufferBeginNanos = nanos;
    }

    public static void recordTileRender(long nanos) {
        CURRENT.tileRenderNanos = nanos;
    }

    public static void recordRegionPrefetch(long nanos) {
        CURRENT.regionPrefetchNanos += nanos;
    }

    public static void recordWorldTileDraw(long nanos) {
        CURRENT.worldTileDrawNanos += nanos;
        CURRENT.maxWorldTileDrawNanos = Math.max(CURRENT.maxWorldTileDrawNanos, nanos);
    }

    public static void recordLodTrim(long nanos) {
        CURRENT.lodTrimNanos += nanos;
    }

    public static void recordFramebufferBlit(long nanos) {
        CURRENT.framebufferBlitNanos = nanos;
    }

    public static void finishMapRender() {
        long now = System.nanoTime();
        CURRENT.totalRenderNanos = now - CURRENT.frameStartNanos;
        if (CURRENT.detailedSample) {
            CURRENT.recordRenderGc(GcSnapshot.capture());
            CURRENT.renderAllocatedBytes = allocatedSince(CURRENT.renderAllocationStart);
        }
        List<String> reasons = CURRENT.renderProblemReasons();
        if (!reasons.isEmpty() && now - lastRenderLogNanos >= LOG_COOLDOWN_NANOS) {
            lastRenderLogNanos = now;
            Main.LOGGER.warn(
                    "[WorldMapPerf] render spike: {} | {}", String.join(", ", reasons), CURRENT.toLogLine());
        }

        List<String> coverageReasons = CURRENT.coverageProblemReasons();
        if (!coverageReasons.isEmpty() && now - lastCoverageLogNanos >= COVERAGE_LOG_COOLDOWN_NANOS) {
            lastCoverageLogNanos = now;
            Main.LOGGER.info(
                    "[WorldMapPerf] map coverage gap: {} | {}",
                    String.join(", ", coverageReasons),
                    CURRENT.toLogLine());
        }
        lastHudStats = HudStats.from(CURRENT);
    }

    public static HudStats hudStats() {
        return lastHudStats;
    }

    public static void recordRootLevel(int rootLevel, int visibleTiles) {
        CURRENT.rootLevel = rootLevel;
        CURRENT.visibleTiles = visibleTiles;
    }

    public static void recordTileVisit() {
        CURRENT.tileVisits++;
    }

    public static void recordTileDraw() {
        CURRENT.tileDraws++;
    }

    public static void recordDrawBudgetExhausted() {
        CURRENT.drawBudgetExhausted++;
    }

    public static void recordUploadBudgetExhausted() {
        CURRENT.uploadBudgetExhausted++;
    }

    public static void recordTextureUpload(long nanos, boolean publish) {
        CURRENT.textureUploads++;
        CURRENT.textureUploadNanos += nanos;
        CURRENT.maxTextureUploadNanos = Math.max(CURRENT.maxTextureUploadNanos, nanos);
        if (publish) {
            CURRENT.texturePublishes++;
        } else {
            CURRENT.textureUpdates++;
        }
    }

    public static void recordMissingTile() {
        CURRENT.missingTiles++;
    }

    public static void recordMissingFallback() {
        CURRENT.missingFallbacks++;
    }

    public static void recordChildSubstitution() {
        CURRENT.childSubstitutions++;
    }

    public static void recordLodSchedule() {
        CURRENT.lodSchedules++;
    }

    public static void recordLodState(int lodTileCount, int pendingLodTiles) {
        CURRENT.lodTileCount = lodTileCount;
        CURRENT.pendingLodTiles = pendingLodTiles;
    }

    public static void recordMapCacheState(
            int loadedRegions,
            int pendingRegionLoads,
            int failedRegionLoads,
            int pendingRegionSaves,
            int chunkQueueSize,
            int queuedChunkCount,
            int lodTileCount,
            int pendingLodTiles) {
        CURRENT.loadedRegions = loadedRegions;
        CURRENT.pendingRegionLoads = pendingRegionLoads;
        CURRENT.failedRegionLoads = failedRegionLoads;
        CURRENT.pendingRegionSaves = pendingRegionSaves;
        CURRENT.chunkQueueSize = chunkQueueSize;
        CURRENT.queuedChunkCount = queuedChunkCount;
        CURRENT.lodTileCount = lodTileCount;
        CURRENT.pendingLodTiles = pendingLodTiles;
    }

    public static void recordRegionLoadScheduled(boolean frameBudgeted) {
        CURRENT.regionSchedules++;
        if (frameBudgeted) CURRENT.frameBudgetedRegionSchedules++;
    }

    public static void recordRegionLoadCompleted(boolean success) {
        if (success) {
            CURRENT.completedRegionLoads++;
        } else {
            CURRENT.failedRegionLoadCompletions++;
        }
    }

    public static void recordAsyncTask(
            String type, String detail, long queueWaitNanos, long runNanos, boolean success) {
        if (success && queueWaitNanos < ASYNC_QUEUE_SPIKE_NANOS && runNanos < ASYNC_RUN_SPIKE_NANOS)
            return;

        long now = System.nanoTime();
        Long lastLog = LAST_ASYNC_LOG_NANOS.get(type);
        if (lastLog != null && now - lastLog < LOG_COOLDOWN_NANOS) return;
        LAST_ASYNC_LOG_NANOS.put(type, now);
        Main.LOGGER.warn(
                "[WorldMapPerf] async {} {}: queue {} run {} success {} | mem {}",
                type,
                detail,
                ms(queueWaitNanos),
                ms(runNanos),
                success,
                memoryLine());
    }

    public static void recordChunkWork(long nanos) {
        tileUpdateChunkWorkNanos += nanos;
    }

    public static void recordChunkFinalize(
            long totalNanos, long regionWriteNanos, long lodInvalidateNanos) {
        tileUpdateChunkFinalizeNanos += totalNanos;
        tileUpdateChunkRegionWriteNanos += regionWriteNanos;
        tileUpdateChunkLodInvalidateNanos += lodInvalidateNanos;
    }

    public static void recordTileUpdate(
            long nanos,
            int chunkUpdates,
            int chunkQueueSize,
            int queuedChunkCount,
            long consumeLoadsNanos,
            long enqueueNanos,
            long processChunksNanos,
            long consumeChunksNanos,
            long scheduleChunksNanos,
            long saveNanos,
            long trimNanos) {
        GcDelta gcDelta =
                tileUpdateGcStart == null
                        ? new GcDelta(0L, 0L)
                        : GcSnapshot.capture().deltaFrom(tileUpdateGcStart);
        long allocatedBytes = allocatedSince(tileUpdateAllocationStart);
        lastTileUpdateNanos = nanos;
        lastConsumeLoadsNanos = consumeLoadsNanos;
        lastEnqueueNanos = enqueueNanos;
        lastProcessChunksNanos = processChunksNanos;
        lastConsumeChunksNanos = consumeChunksNanos;
        lastScheduleChunksNanos = scheduleChunksNanos;
        lastSaveNanos = saveNanos;
        lastTrimNanos = trimNanos;
        lastChunkUpdates = chunkUpdates;
        lastChunkQueueSize = chunkQueueSize;
        lastQueuedChunkCount = queuedChunkCount;
        lastChunkWorkNanos = tileUpdateChunkWorkNanos;
        lastChunkFinalizeNanos = tileUpdateChunkFinalizeNanos;
        lastChunkRegionWriteNanos = tileUpdateChunkRegionWriteNanos;
        lastChunkLodInvalidateNanos = tileUpdateChunkLodInvalidateNanos;
        lastTileUpdateGcCount = gcDelta.count();
        lastTileUpdateGcMillis = gcDelta.millis();
        lastTileUpdateAllocatedBytes = allocatedBytes;
        CURRENT.tileUpdateNanos = nanos;
        CURRENT.consumeLoadsNanos = consumeLoadsNanos;
        CURRENT.enqueueNanos = enqueueNanos;
        CURRENT.processChunksNanos = processChunksNanos;
        CURRENT.consumeChunksNanos = consumeChunksNanos;
        CURRENT.scheduleChunksNanos = scheduleChunksNanos;
        CURRENT.saveNanos = saveNanos;
        CURRENT.trimNanos = trimNanos;
        CURRENT.chunkUpdates = chunkUpdates;
        CURRENT.chunkQueueSize = chunkQueueSize;
        CURRENT.queuedChunkCount = queuedChunkCount;
        CURRENT.chunkWorkNanos = tileUpdateChunkWorkNanos;
        CURRENT.chunkFinalizeNanos = tileUpdateChunkFinalizeNanos;
        CURRENT.chunkRegionWriteNanos = tileUpdateChunkRegionWriteNanos;
        CURRENT.chunkLodInvalidateNanos = tileUpdateChunkLodInvalidateNanos;
        CURRENT.tileUpdateGcCount = gcDelta.count();
        CURRENT.tileUpdateGcMillis = gcDelta.millis();
        CURRENT.tileUpdateAllocatedBytes = allocatedBytes;

        long now = System.nanoTime();
        if ((nanos >= TILE_UPDATE_SPIKE_NANOS || allocatedBytes >= TILE_UPDATE_ALLOCATION_SPIKE_BYTES)
                && now - lastTileUpdateLogNanos >= LOG_COOLDOWN_NANOS) {
            lastTileUpdateLogNanos = now;
            Main.LOGGER.warn(
                    "[WorldMapPerf] tile update spike: update {} phases load {} enqueue {} chunks {} save {}"
                            + " trim {} | chunkPipeline consume {} schedule {} | chunkParts work {} finalize {}"
                            + " write {} lodInvalidate {} | chunks {} queue {}/{} | regions {} pending {} saves"
                            + " {} failed {} | lod {} pending {} | gc {}/{}ms alloc {} | mem {} tileImages~{}",
                    ms(nanos),
                    ms(consumeLoadsNanos),
                    ms(enqueueNanos),
                    ms(processChunksNanos),
                    ms(saveNanos),
                    ms(trimNanos),
                    ms(consumeChunksNanos),
                    ms(scheduleChunksNanos),
                    ms(tileUpdateChunkWorkNanos),
                    ms(tileUpdateChunkFinalizeNanos),
                    ms(tileUpdateChunkRegionWriteNanos),
                    ms(tileUpdateChunkLodInvalidateNanos),
                    chunkUpdates,
                    chunkQueueSize,
                    queuedChunkCount,
                    CURRENT.loadedRegions,
                    CURRENT.pendingRegionLoads,
                    CURRENT.pendingRegionSaves,
                    CURRENT.failedRegionLoads,
                    CURRENT.lodTileCount,
                    CURRENT.pendingLodTiles,
                    gcDelta.count(),
                    gcDelta.millis(),
                    allocatedMb(allocatedBytes),
                    memoryLine(),
                    tileImageMemoryLine(CURRENT));
        }
    }

    private static boolean hasRenderTimingSpike(Metrics metrics) {
        return metrics.totalRenderNanos >= RENDER_SPIKE_NANOS
                || metrics.tileRenderNanos >= TILE_RENDER_SPIKE_NANOS
                || metrics.framebufferBeginNanos >= FRAMEBUFFER_SPIKE_NANOS
                || metrics.framebufferBlitNanos >= FRAMEBUFFER_SPIKE_NANOS
                || metrics.textureUploadNanos >= TEXTURE_UPLOAD_SPIKE_NANOS
                || metrics.maxWorldTileDrawNanos >= SINGLE_TILE_DRAW_SPIKE_NANOS
                || metrics.tileUpdateNanos >= TILE_UPDATE_SPIKE_NANOS;
    }

    private static boolean hasPipelineBackpressure(Metrics metrics) {
        return metrics.pendingRegionLoads >= HIGH_PENDING_REGION_LOADS
                || metrics.pendingLodTiles >= HIGH_PENDING_LOD_TILES
                || metrics.pendingRegionSaves >= HIGH_PENDING_REGION_SAVES
                || metrics.drawBudgetExhausted > 0
                || metrics.failedRegionLoads > 0
                || metrics.failedRegionLoadCompletions > 0
                || metrics.memoryPressure()
                || metrics.estimatedTileImageMb() >= HIGH_ESTIMATED_TILE_IMAGE_MB;
    }

    private static boolean hasCoverageGap(Metrics metrics) {
        return metrics.missingTiles >= HIGH_MISSING_TILE_COUNT
                && metrics.pendingRegionLoads == 0
                && metrics.pendingLodTiles == 0
                && metrics.regionSchedules == 0
                && metrics.lodSchedules == 0;
    }

    private static String coverageLine(Metrics metrics) {
        return "missingTiles="
                + metrics.missingTiles
                + " fallback="
                + metrics.missingFallbacks
                + " root=L"
                + metrics.rootLevel
                + " scale="
                + String.format(Locale.ROOT, "%.3f", metrics.scale);
    }

    private static String phaseLine(Metrics metrics) {
        return "load "
                + ms(metrics.consumeLoadsNanos)
                + " enqueue "
                + ms(metrics.enqueueNanos)
                + " chunks "
                + ms(metrics.processChunksNanos)
                + " consume "
                + ms(metrics.consumeChunksNanos)
                + " schedule "
                + ms(metrics.scheduleChunksNanos)
                + " save "
                + ms(metrics.saveNanos)
                + " trim "
                + ms(metrics.trimNanos);
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.2fms", nanos / 1_000_000.0);
    }

    private static String memoryLine() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L;
        long maxMb = runtime.maxMemory() / 1_048_576L;
        return usedMb + "/" + maxMb + " MB";
    }

    private static String tileImageMemoryLine(Metrics metrics) {
        return metrics.estimatedTileImageMb() + " MB";
    }

    private static com.sun.management.ThreadMXBean createThreadAllocationBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean allocationBean)
                || !allocationBean.isThreadAllocatedMemorySupported()) {
            return null;
        }
        try {
            if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
                allocationBean.setThreadAllocatedMemoryEnabled(true);
            }
            return allocationBean;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static long currentThreadAllocatedBytes() {
        if (THREAD_ALLOCATION_BEAN == null) return -1L;
        long allocated = THREAD_ALLOCATION_BEAN.getThreadAllocatedBytes(Thread.currentThread().getId());
        return Math.max(-1L, allocated);
    }

    public static long allocatedSince(long startBytes) {
        if (startBytes < 0L) return -1L;
        long endBytes = currentThreadAllocatedBytes();
        return endBytes < startBytes ? -1L : endBytes - startBytes;
    }

    private static String allocatedMb(long bytes) {
        if (bytes < 0L) return "n/a";
        return String.format(Locale.ROOT, "%.2f MB", bytes / 1_048_576.0);
    }

    private record GcSnapshot(long count, long millis) {
        private static GcSnapshot capture() {
            long count = 0L;
            long millis = 0L;
            for (GarbageCollectorMXBean collector : GC_COLLECTORS) {
                count += Math.max(0L, collector.getCollectionCount());
                millis += Math.max(0L, collector.getCollectionTime());
            }
            return new GcSnapshot(count, millis);
        }

        private GcDelta deltaFrom(GcSnapshot start) {
            if (start == null) return new GcDelta(0L, 0L);
            return new GcDelta(Math.max(0L, count - start.count), Math.max(0L, millis - start.millis));
        }
    }

    private record GcDelta(long count, long millis) {}

    public record HudStats(
            long mapRenderNanos,
            long tileRenderNanos,
            long tileUpdateNanos,
            long consumeChunksNanos,
            long scheduleChunksNanos,
            long textureUploadNanos,
            int rootLevel,
            int visibleTiles,
            int tileDraws,
            int missingTiles,
            int chunkQueueSize,
            int queuedChunkCount,
            int lodTileCount,
            int pendingLodTiles) {
        private static final HudStats EMPTY =
                new HudStats(0L, 0L, 0L, 0L, 0L, 0L, 0, 0, 0, 0, 0, 0, 0, 0);

        private static HudStats from(Metrics metrics) {
            return new HudStats(
                    metrics.totalRenderNanos,
                    metrics.tileRenderNanos,
                    metrics.tileUpdateNanos,
                    metrics.consumeChunksNanos,
                    metrics.scheduleChunksNanos,
                    metrics.textureUploadNanos,
                    metrics.rootLevel,
                    metrics.visibleTiles,
                    metrics.tileDraws,
                    metrics.missingTiles,
                    metrics.chunkQueueSize,
                    metrics.queuedChunkCount,
                    metrics.lodTileCount,
                    metrics.pendingLodTiles);
        }
    }

    private static final class Metrics {
        private long frameStartNanos;
        private long totalRenderNanos;
        private long framebufferBeginNanos;
        private long tileRenderNanos;
        private long framebufferBlitNanos;
        private long regionPrefetchNanos;
        private long worldTileDrawNanos;
        private long maxWorldTileDrawNanos;
        private long lodTrimNanos;
        private long textureUploadNanos;
        private long maxTextureUploadNanos;
        private long tileUpdateNanos;
        private long consumeLoadsNanos;
        private long enqueueNanos;
        private long processChunksNanos;
        private long consumeChunksNanos;
        private long scheduleChunksNanos;
        private long saveNanos;
        private long trimNanos;
        private double scale;
        private double fboScale;
        private double secondaryScale;
        private double leftWorld;
        private double topWorld;
        private double rightWorld;
        private double bottomWorld;
        private int screenWidth;
        private int screenHeight;
        private int rootLevel;
        private int visibleTiles;
        private int tileVisits;
        private int tileDraws;
        private int drawBudgetExhausted;
        private int uploadBudgetExhausted;
        private int textureUploads;
        private int texturePublishes;
        private int textureUpdates;
        private int missingTiles;
        private int missingFallbacks;
        private int childSubstitutions;
        private int lodSchedules;
        private int lodTileCount;
        private int pendingLodTiles;
        private int loadedRegions;
        private int pendingRegionLoads;
        private int pendingRegionSaves;
        private int failedRegionLoads;
        private int regionSchedules;
        private int frameBudgetedRegionSchedules;
        private int completedRegionLoads;
        private int failedRegionLoadCompletions;
        private int chunkUpdates;
        private int chunkQueueSize;
        private int queuedChunkCount;
        private long chunkWorkNanos;
        private long chunkFinalizeNanos;
        private long chunkRegionWriteNanos;
        private long chunkLodInvalidateNanos;
        private long renderGcCount;
        private long renderGcMillis;
        private long tileUpdateGcCount;
        private long tileUpdateGcMillis;
        private long renderAllocationStart;
        private long renderAllocatedBytes;
        private long tileUpdateAllocatedBytes;
        private GcSnapshot gcStart;
        private boolean detailedSample;

        private void reset() {
            frameStartNanos = 0L;
            totalRenderNanos = 0L;
            framebufferBeginNanos = 0L;
            tileRenderNanos = 0L;
            framebufferBlitNanos = 0L;
            regionPrefetchNanos = 0L;
            worldTileDrawNanos = 0L;
            maxWorldTileDrawNanos = 0L;
            lodTrimNanos = 0L;
            textureUploadNanos = 0L;
            maxTextureUploadNanos = 0L;
            tileUpdateNanos = 0L;
            consumeLoadsNanos = 0L;
            enqueueNanos = 0L;
            processChunksNanos = 0L;
            consumeChunksNanos = 0L;
            scheduleChunksNanos = 0L;
            saveNanos = 0L;
            trimNanos = 0L;
            scale = 0.0;
            fboScale = 0.0;
            secondaryScale = 0.0;
            leftWorld = 0.0;
            topWorld = 0.0;
            rightWorld = 0.0;
            bottomWorld = 0.0;
            screenWidth = 0;
            screenHeight = 0;
            rootLevel = 0;
            visibleTiles = 0;
            tileVisits = 0;
            tileDraws = 0;
            drawBudgetExhausted = 0;
            uploadBudgetExhausted = 0;
            textureUploads = 0;
            texturePublishes = 0;
            textureUpdates = 0;
            missingTiles = 0;
            missingFallbacks = 0;
            childSubstitutions = 0;
            lodSchedules = 0;
            lodTileCount = 0;
            pendingLodTiles = 0;
            loadedRegions = 0;
            pendingRegionLoads = 0;
            pendingRegionSaves = 0;
            failedRegionLoads = 0;
            regionSchedules = 0;
            frameBudgetedRegionSchedules = 0;
            completedRegionLoads = 0;
            failedRegionLoadCompletions = 0;
            chunkUpdates = 0;
            chunkQueueSize = 0;
            queuedChunkCount = 0;
            chunkWorkNanos = 0L;
            chunkFinalizeNanos = 0L;
            chunkRegionWriteNanos = 0L;
            chunkLodInvalidateNanos = 0L;
            renderGcCount = 0L;
            renderGcMillis = 0L;
            tileUpdateGcCount = 0L;
            tileUpdateGcMillis = 0L;
            renderAllocationStart = -1L;
            renderAllocatedBytes = -1L;
            tileUpdateAllocatedBytes = -1L;
            gcStart = null;
            detailedSample = false;
        }

        private void recordRenderGc(GcSnapshot end) {
            GcDelta delta = end.deltaFrom(gcStart);
            renderGcCount = delta.count();
            renderGcMillis = delta.millis();
        }

        private List<String> renderProblemReasons() {
            List<String> reasons = new ArrayList<>();
            if (totalRenderNanos >= RENDER_SPIKE_NANOS) reasons.add("frame=" + ms(totalRenderNanos));
            if (tileRenderNanos >= TILE_RENDER_SPIKE_NANOS) reasons.add("tiles=" + ms(tileRenderNanos));
            if (framebufferBeginNanos >= FRAMEBUFFER_SPIKE_NANOS)
                reasons.add("fboBegin=" + ms(framebufferBeginNanos));
            if (framebufferBlitNanos >= FRAMEBUFFER_SPIKE_NANOS)
                reasons.add("fboBlit=" + ms(framebufferBlitNanos));
            if (textureUploadNanos >= TEXTURE_UPLOAD_SPIKE_NANOS)
                reasons.add("textureUpload=" + ms(textureUploadNanos));
            if (maxWorldTileDrawNanos >= SINGLE_TILE_DRAW_SPIKE_NANOS)
                reasons.add("singleDraw=" + ms(maxWorldTileDrawNanos));
            if (tileUpdateNanos >= TILE_UPDATE_SPIKE_NANOS)
                reasons.add("tileUpdate=" + ms(tileUpdateNanos));
            if (renderGcCount > 0) reasons.add("renderGc=" + renderGcCount + "/" + renderGcMillis + "ms");
            if (tileUpdateGcCount > 0)
                reasons.add("updateGc=" + tileUpdateGcCount + "/" + tileUpdateGcMillis + "ms");
            if (renderAllocatedBytes >= RENDER_ALLOCATION_SPIKE_BYTES) {
                reasons.add("renderAlloc=" + allocatedMb(renderAllocatedBytes));
            }
            if (tileUpdateAllocatedBytes >= TILE_UPDATE_ALLOCATION_SPIKE_BYTES) {
                reasons.add("updateAlloc=" + allocatedMb(tileUpdateAllocatedBytes));
            }
            if (pendingRegionLoads >= HIGH_PENDING_REGION_LOADS)
                reasons.add("regionBacklog=" + pendingRegionLoads);
            if (pendingLodTiles >= HIGH_PENDING_LOD_TILES) reasons.add("lodBacklog=" + pendingLodTiles);
            if (pendingRegionSaves >= HIGH_PENDING_REGION_SAVES)
                reasons.add("saveBacklog=" + pendingRegionSaves);
            if (drawBudgetExhausted > 0) reasons.add("drawBudgetHit=" + drawBudgetExhausted);
            if (uploadBudgetExhausted > 0 && hasRenderTimingSpike(this)) {
                reasons.add("uploadBudgetHit=" + uploadBudgetExhausted);
            }
            if (failedRegionLoads > 0) reasons.add("failedRegions=" + failedRegionLoads);
            if (failedRegionLoadCompletions > 0)
                reasons.add("failedRegionLoads=" + failedRegionLoadCompletions);
            if (memoryPressure()) reasons.add("memory=" + memoryLine());
            if (estimatedTileImageMb() >= HIGH_ESTIMATED_TILE_IMAGE_MB) {
                reasons.add("tileImages~" + estimatedTileImageMb() + "MB");
            }
            return reasons;
        }

        private List<String> coverageProblemReasons() {
            List<String> reasons = new ArrayList<>();
            if (hasCoverageGap(this) && !hasRenderTimingSpike(this) && !hasPipelineBackpressure(this)) {
                reasons.add(coverageLine(this));
            }
            return reasons;
        }

        private boolean memoryPressure() {
            Runtime runtime = Runtime.getRuntime();
            long max = runtime.maxMemory();
            if (max <= 0L) return false;
            long used = runtime.totalMemory() - runtime.freeMemory();
            return used / (double) max >= MEMORY_PRESSURE_RATIO;
        }

        private long estimatedTileImageMb() {
            long regionImageCount = loadedRegions + pendingRegionLoads + pendingRegionSaves;
            long regionBytes =
                    (long) WorldMapRegion.REGION_PIXEL_SIZE * WorldMapRegion.REGION_PIXEL_SIZE * 4L;
            long renderTileImageCount = lodTileCount + pendingLodTiles;
            long renderTileBytes =
                    (long) WorldMapRenderTileKey.PIXEL_SIZE * WorldMapRenderTileKey.PIXEL_SIZE * 4L;
            return (regionImageCount * regionBytes + renderTileImageCount * renderTileBytes) / 1_048_576L;
        }

        private String toLogLine() {
            return String.format(
                    Locale.ROOT,
                    "scale %.3f fbo %.3fx%.3f root L%d | render %s begin %s tiles %s blit %s tileParts"
                            + " prefetch %s draw %s/max %s trim %s uploads %d/%s max %s new/update %d/%d | update"
                            + " %s (%s) chunks %d queue %d/%d chunkParts work %s finalize %s write %s"
                            + " lodInvalidate %s gc render %d/%dms update %d/%dms alloc render %s update %s |"
                            + " visible %d visits %d draws %d budgets draw/upload %d/%d | missing %d fallback %d"
                            + " childSub %d | lod %d pending %d sched %d | regions %d pending %d saves %d failed"
                            + " %d loads %d/%d complete %d failedComplete %d | view %.0f,%.0f -> %.0f,%.0f %dx%d"
                            + " | mem %s tileImages~%s",
                    scale,
                    fboScale,
                    secondaryScale,
                    rootLevel,
                    ms(totalRenderNanos),
                    ms(framebufferBeginNanos),
                    ms(tileRenderNanos),
                    ms(framebufferBlitNanos),
                    ms(regionPrefetchNanos),
                    ms(worldTileDrawNanos),
                    ms(maxWorldTileDrawNanos),
                    ms(lodTrimNanos),
                    textureUploads,
                    ms(textureUploadNanos),
                    ms(maxTextureUploadNanos),
                    texturePublishes,
                    textureUpdates,
                    ms(tileUpdateNanos),
                    phaseLine(this),
                    chunkUpdates,
                    chunkQueueSize,
                    queuedChunkCount,
                    ms(chunkWorkNanos),
                    ms(chunkFinalizeNanos),
                    ms(chunkRegionWriteNanos),
                    ms(chunkLodInvalidateNanos),
                    renderGcCount,
                    renderGcMillis,
                    tileUpdateGcCount,
                    tileUpdateGcMillis,
                    allocatedMb(renderAllocatedBytes),
                    allocatedMb(tileUpdateAllocatedBytes),
                    visibleTiles,
                    tileVisits,
                    tileDraws,
                    drawBudgetExhausted,
                    uploadBudgetExhausted,
                    missingTiles,
                    missingFallbacks,
                    childSubstitutions,
                    lodTileCount,
                    pendingLodTiles,
                    lodSchedules,
                    loadedRegions,
                    pendingRegionLoads,
                    pendingRegionSaves,
                    failedRegionLoads,
                    regionSchedules,
                    frameBudgetedRegionSchedules,
                    completedRegionLoads,
                    failedRegionLoadCompletions,
                    leftWorld,
                    topWorld,
                    rightWorld,
                    bottomWorld,
                    screenWidth,
                    screenHeight,
                    memoryLine(),
                    tileImageMemoryLine(this));
        }
    }
}
