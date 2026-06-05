package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.Main;

import java.util.Locale;

final class WorldMapBuildProfiler {
    private static final long CHUNK_BUILD_SPIKE_NANOS = 8_000_000L;
    private static final long SINGLE_OPERATION_SPIKE_NANOS = 4_000_000L;
    private static final long FRAME_WORK_SPIKE_NANOS = 3_000_000L;
    private static final long LOG_COOLDOWN_NANOS = 1_000_000_000L;

    private static final ThreadLocal<ChunkMetrics> ACTIVE = ThreadLocal.withInitial(ChunkMetrics::new);
    private static long lastLogNanos;
    private static long lastFrameLogNanos;

    private WorldMapBuildProfiler() {
    }

    static void beginChunk(int chunkX, int chunkZ) {
        ACTIVE.get().begin(chunkX, chunkZ);
    }

    static void finishChunk() {
        ChunkMetrics metrics = ACTIVE.get();
        if (!metrics.active) return;

        metrics.wallNanos = System.nanoTime() - metrics.startedNanos;
        metrics.active = false;
        long now = System.nanoTime();
        if (metrics.isSlow() && now - lastLogNanos >= LOG_COOLDOWN_NANOS) {
            lastLogNanos = now;
            Main.LOGGER.warn("[WorldMapPerf] slow chunk build: {}", metrics.toLogLine());
        }
    }

    static void recordSamplePhase(long nanos) {
        ChunkMetrics metrics = ACTIVE.get();
        if (metrics.active) metrics.sampleNanos += nanos;
    }

    static void recordRenderPhase(long nanos) {
        ChunkMetrics metrics = ACTIVE.get();
        if (metrics.active) metrics.renderNanos += nanos;
    }

    static void recordColorPreparePhase(long nanos) {
        ChunkMetrics metrics = ACTIVE.get();
        if (metrics.active) metrics.colorPrepareNanos += nanos;
    }

    static void recordTextureColorMiss(long nanos, String blockName) {
        ChunkMetrics metrics = ACTIVE.get();
        if (!metrics.active) return;
        metrics.textureColorMisses++;
        metrics.textureColorMissNanos += nanos;
        if (nanos > metrics.maxTextureColorMissNanos) {
            metrics.maxTextureColorMissNanos = nanos;
            metrics.slowestTextureBlock = blockName;
        }
    }

    static void recordFrameWork(long workNanos, long budgetNanos, int pendingBuilds) {
        if (workNanos < FRAME_WORK_SPIKE_NANOS && workNanos < budgetNanos * 3L) return;

        long now = System.nanoTime();
        if (now - lastFrameLogNanos < LOG_COOLDOWN_NANOS) return;
        lastFrameLogNanos = now;
        Main.LOGGER.warn(
                "[WorldMapPerf] cooperative chunk work exceeded frame budget: work {} budget {} pending {}",
                ms(workNanos),
                ms(budgetNanos),
                pendingBuilds
        );
    }

    private static String ms(long nanos) {
        if (nanos < 0L) return "-";
        return String.format(Locale.ROOT, "%.2fms", nanos / 1_000_000.0);
    }

    private static final class ChunkMetrics {
        private boolean active;
        private int chunkX;
        private int chunkZ;
        private long startedNanos;
        private long wallNanos;
        private long sampleNanos;
        private long colorPrepareNanos;
        private long renderNanos;
        private long textureColorMissNanos;
        private long maxTextureColorMissNanos;
        private int textureColorMisses;
        private String slowestTextureBlock = "-";

        private void begin(int chunkX, int chunkZ) {
            active = true;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            startedNanos = System.nanoTime();
            wallNanos = 0L;
            sampleNanos = 0L;
            colorPrepareNanos = 0L;
            renderNanos = 0L;
            textureColorMissNanos = 0L;
            maxTextureColorMissNanos = 0L;
            textureColorMisses = 0;
            slowestTextureBlock = "-";
        }

        private boolean isSlow() {
            return wallNanos >= CHUNK_BUILD_SPIKE_NANOS
                    || maxTextureColorMissNanos >= SINGLE_OPERATION_SPIKE_NANOS;
        }

        private String toLogLine() {
            return String.format(Locale.ROOT,
                    "chunk %d,%d | wall %s | sample %s colors %s render %s other %s | textureMiss %d/%s max %s block %s",
                    chunkX, chunkZ, ms(wallNanos),
                    ms(sampleNanos), ms(colorPrepareNanos), ms(renderNanos),
                    ms(Math.max(0L, wallNanos - sampleNanos - colorPrepareNanos - renderNanos)),
                    textureColorMisses, ms(textureColorMissNanos), ms(maxTextureColorMissNanos), slowestTextureBlock
            );
        }
    }
}
