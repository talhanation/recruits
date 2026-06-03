package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Happy birthday, Daniel!
 * Did you really think I'd let this day pass quietly?
 * I know the kind of power you vibe with, the kind that keeps climbing no matter what.
 * So here's your gift: a profile picture wrapped in that same aura.
 * Use it anywhere, you know the rule: even if the stairs never end, we keep climbing!
 */
final class WorldMapDebugProfiler {
    private static final long RENDER_SPIKE_NANOS = 40_000_000L;
    private static final long TILE_RENDER_SPIKE_NANOS = 24_000_000L;
    private static final long FRAMEBUFFER_SPIKE_NANOS = 8_000_000L;
    private static final long TILE_UPDATE_SPIKE_NANOS = 8_000_000L;
    private static final long LOG_COOLDOWN_NANOS = 2_000_000_000L;
    private static final double MEMORY_PRESSURE_RATIO = 0.85;
    private static final int HIGH_PENDING_REGION_LOADS = 24;
    private static final int HIGH_PENDING_LOD_TILES = 3;
    private static final int HIGH_MISSING_TILE_COUNT = 48;

    private static final Metrics CURRENT = new Metrics();

    private static long lastRenderLogNanos;
    private static long lastTileUpdateLogNanos;
    private static long lastTileUpdateNanos;
    private static int lastChunkUpdates;
    private static int lastChunkQueueSize;
    private static int lastQueuedChunkCount;

    private WorldMapDebugProfiler() {
    }

    static void beginMapRender(int screenWidth, int screenHeight, double scale) {
        CURRENT.reset();
        CURRENT.frameStartNanos = System.nanoTime();
        CURRENT.screenWidth = screenWidth;
        CURRENT.screenHeight = screenHeight;
        CURRENT.scale = scale;
        CURRENT.tileUpdateNanos = lastTileUpdateNanos;
        CURRENT.chunkUpdates = lastChunkUpdates;
        CURRENT.chunkQueueSize = lastChunkQueueSize;
        CURRENT.queuedChunkCount = lastQueuedChunkCount;
    }

    static void recordFramebuffer(MapFramebufferPass.Frame frame) {
        CURRENT.fboScale = frame.fboScale();
        CURRENT.secondaryScale = frame.secondaryScale();
        CURRENT.leftWorld = frame.leftWorld();
        CURRENT.topWorld = frame.topWorld();
        CURRENT.rightWorld = frame.rightWorld();
        CURRENT.bottomWorld = frame.bottomWorld();
    }

    static void recordFramebufferBegin(long nanos) {
        CURRENT.framebufferBeginNanos = nanos;
    }

    static void recordTileRender(long nanos) {
        CURRENT.tileRenderNanos = nanos;
    }

    static void recordFramebufferBlit(long nanos) {
        CURRENT.framebufferBlitNanos = nanos;
    }

    static void finishMapRender() {
        long now = System.nanoTime();
        CURRENT.totalRenderNanos = now - CURRENT.frameStartNanos;
        List<String> reasons = CURRENT.renderProblemReasons();
        if (!reasons.isEmpty() && now - lastRenderLogNanos >= LOG_COOLDOWN_NANOS) {
            lastRenderLogNanos = now;
            Main.LOGGER.warn("[WorldMapPerf] render spike: {} | {}", String.join(", ", reasons), CURRENT.toLogLine());
        }
    }

    static void recordRootLevel(int rootLevel, int visibleTiles) {
        CURRENT.rootLevel = rootLevel;
        CURRENT.visibleTiles = visibleTiles;
    }

    static void recordTileVisit() {
        CURRENT.tileVisits++;
    }

    static void recordTileDraw() {
        CURRENT.tileDraws++;
    }

    static void recordVisitBudgetExhausted() {
        CURRENT.visitBudgetExhausted++;
    }

    static void recordDrawBudgetExhausted() {
        CURRENT.drawBudgetExhausted++;
    }

    static void recordUploadBudgetExhausted() {
        CURRENT.uploadBudgetExhausted++;
    }

    static void recordMissingTile() {
        CURRENT.missingTiles++;
    }

    static void recordMissingFallback() {
        CURRENT.missingFallbacks++;
    }

    static void recordChildSubstitution() {
        CURRENT.childSubstitutions++;
    }

    static void recordLodSchedule() {
        CURRENT.lodSchedules++;
    }

    static void recordLodState(int lodTileCount, int pendingLodTiles) {
        CURRENT.lodTileCount = lodTileCount;
        CURRENT.pendingLodTiles = pendingLodTiles;
    }

    static void recordTileManagerState(int loadedRegions, int pendingRegionLoads, int failedRegionLoads,
                                       int chunkQueueSize, int queuedChunkCount, int lodTileCount,
                                       int pendingLodTiles) {
        CURRENT.loadedRegions = loadedRegions;
        CURRENT.pendingRegionLoads = pendingRegionLoads;
        CURRENT.failedRegionLoads = failedRegionLoads;
        CURRENT.chunkQueueSize = chunkQueueSize;
        CURRENT.queuedChunkCount = queuedChunkCount;
        CURRENT.lodTileCount = lodTileCount;
        CURRENT.pendingLodTiles = pendingLodTiles;
    }

    static void recordRegionLoadScheduled(boolean frameBudgeted) {
        CURRENT.regionSchedules++;
        if (frameBudgeted) CURRENT.frameBudgetedRegionSchedules++;
    }

    static void recordRegionLoadCompleted(boolean success) {
        if (success) {
            CURRENT.completedRegionLoads++;
        } else {
            CURRENT.failedRegionLoadCompletions++;
        }
    }

    static void recordTileUpdate(long nanos, int chunkUpdates, int chunkQueueSize, int queuedChunkCount) {
        lastTileUpdateNanos = nanos;
        lastChunkUpdates = chunkUpdates;
        lastChunkQueueSize = chunkQueueSize;
        lastQueuedChunkCount = queuedChunkCount;
        CURRENT.tileUpdateNanos = nanos;
        CURRENT.chunkUpdates = chunkUpdates;
        CURRENT.chunkQueueSize = chunkQueueSize;
        CURRENT.queuedChunkCount = queuedChunkCount;

        long now = System.nanoTime();
        if (nanos >= TILE_UPDATE_SPIKE_NANOS && now - lastTileUpdateLogNanos >= LOG_COOLDOWN_NANOS) {
            lastTileUpdateLogNanos = now;
            Main.LOGGER.warn(
                    "[WorldMapPerf] tile update spike: update {} chunks {} queue {}/{} | regions {} pending {} failed {} | lod {} pending {} | mem {}",
                    ms(nanos), chunkUpdates, chunkQueueSize, queuedChunkCount,
                    CURRENT.loadedRegions, CURRENT.pendingRegionLoads, CURRENT.failedRegionLoads,
                    CURRENT.lodTileCount, CURRENT.pendingLodTiles, memoryLine()
            );
        }
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

    private static final class Metrics {
        private long frameStartNanos;
        private long totalRenderNanos;
        private long framebufferBeginNanos;
        private long tileRenderNanos;
        private long framebufferBlitNanos;
        private long tileUpdateNanos;
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
        private int visitBudgetExhausted;
        private int drawBudgetExhausted;
        private int uploadBudgetExhausted;
        private int missingTiles;
        private int missingFallbacks;
        private int childSubstitutions;
        private int lodSchedules;
        private int lodTileCount;
        private int pendingLodTiles;
        private int loadedRegions;
        private int pendingRegionLoads;
        private int failedRegionLoads;
        private int regionSchedules;
        private int frameBudgetedRegionSchedules;
        private int completedRegionLoads;
        private int failedRegionLoadCompletions;
        private int chunkUpdates;
        private int chunkQueueSize;
        private int queuedChunkCount;

        private void reset() {
            frameStartNanos = 0L;
            totalRenderNanos = 0L;
            framebufferBeginNanos = 0L;
            tileRenderNanos = 0L;
            framebufferBlitNanos = 0L;
            tileUpdateNanos = 0L;
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
            visitBudgetExhausted = 0;
            drawBudgetExhausted = 0;
            uploadBudgetExhausted = 0;
            missingTiles = 0;
            missingFallbacks = 0;
            childSubstitutions = 0;
            lodSchedules = 0;
            lodTileCount = 0;
            pendingLodTiles = 0;
            loadedRegions = 0;
            pendingRegionLoads = 0;
            failedRegionLoads = 0;
            regionSchedules = 0;
            frameBudgetedRegionSchedules = 0;
            completedRegionLoads = 0;
            failedRegionLoadCompletions = 0;
            chunkUpdates = 0;
            chunkQueueSize = 0;
            queuedChunkCount = 0;
        }

        private List<String> renderProblemReasons() {
            List<String> reasons = new ArrayList<>();
            if (totalRenderNanos >= RENDER_SPIKE_NANOS) reasons.add("frame=" + ms(totalRenderNanos));
            if (tileRenderNanos >= TILE_RENDER_SPIKE_NANOS) reasons.add("tiles=" + ms(tileRenderNanos));
            if (framebufferBeginNanos >= FRAMEBUFFER_SPIKE_NANOS) reasons.add("fboBegin=" + ms(framebufferBeginNanos));
            if (framebufferBlitNanos >= FRAMEBUFFER_SPIKE_NANOS) reasons.add("fboBlit=" + ms(framebufferBlitNanos));
            if (tileUpdateNanos >= TILE_UPDATE_SPIKE_NANOS) reasons.add("tileUpdate=" + ms(tileUpdateNanos));
            if (pendingRegionLoads >= HIGH_PENDING_REGION_LOADS) reasons.add("regionBacklog=" + pendingRegionLoads);
            if (pendingLodTiles >= HIGH_PENDING_LOD_TILES) reasons.add("lodBacklog=" + pendingLodTiles);
            if (missingTiles >= HIGH_MISSING_TILE_COUNT) reasons.add("missingTiles=" + missingTiles);
            if (visitBudgetExhausted > 0) reasons.add("visitBudgetHit=" + visitBudgetExhausted);
            if (drawBudgetExhausted > 0) reasons.add("drawBudgetHit=" + drawBudgetExhausted);
            if (uploadBudgetExhausted > 0) reasons.add("uploadBudgetHit=" + uploadBudgetExhausted);
            if (failedRegionLoads > 0) reasons.add("failedRegions=" + failedRegionLoads);
            if (failedRegionLoadCompletions > 0) reasons.add("failedRegionLoads=" + failedRegionLoadCompletions);
            if (memoryPressure()) reasons.add("memory=" + memoryLine());
            return reasons;
        }

        private boolean memoryPressure() {
            Runtime runtime = Runtime.getRuntime();
            long max = runtime.maxMemory();
            if (max <= 0L) return false;
            long used = runtime.totalMemory() - runtime.freeMemory();
            return used / (double) max >= MEMORY_PRESSURE_RATIO;
        }

        private String toLogLine() {
            return String.format(Locale.ROOT,
                    "scale %.3f fbo %.3fx%.3f root L%d | render %s begin %s tiles %s blit %s | update %s chunks %d queue %d/%d | visible %d visits %d draws %d budgets v/d/u %d/%d/%d | missing %d fallback %d childSub %d | lod %d pending %d sched %d | regions %d pending %d failed %d loads %d/%d complete %d failedComplete %d | view %.0f,%.0f -> %.0f,%.0f %dx%d | mem %s",
                    scale, fboScale, secondaryScale, rootLevel,
                    ms(totalRenderNanos), ms(framebufferBeginNanos), ms(tileRenderNanos), ms(framebufferBlitNanos),
                    ms(tileUpdateNanos), chunkUpdates, chunkQueueSize, queuedChunkCount,
                    visibleTiles, tileVisits, tileDraws, visitBudgetExhausted, drawBudgetExhausted, uploadBudgetExhausted,
                    missingTiles, missingFallbacks, childSubstitutions,
                    lodTileCount, pendingLodTiles, lodSchedules,
                    loadedRegions, pendingRegionLoads, failedRegionLoads,
                    regionSchedules, frameBudgetedRegionSchedules, completedRegionLoads, failedRegionLoadCompletions,
                    leftWorld, topWorld, rightWorld, bottomWorld, screenWidth, screenHeight,
                    memoryLine());
        }
    }
}
