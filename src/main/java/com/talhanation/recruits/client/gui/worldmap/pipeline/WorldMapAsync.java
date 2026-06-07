package com.talhanation.recruits.client.gui.worldmap.pipeline;

import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegion;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegionPixels;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegionStorage;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class WorldMapAsync {
    private static final AtomicLong REGION_LOAD_SEQUENCE = new AtomicLong();
    private static final AtomicLong RENDER_TILE_SEQUENCE = new AtomicLong();
    private static final AtomicLong CHUNK_BUILD_SEQUENCE = new AtomicLong();
    private static final ThreadPoolExecutor REGION_LOAD_EXECUTOR =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new PriorityBlockingQueue<>(),
                    namedDaemonFactory("recruits-worldmap-load"));
    private static final ThreadPoolExecutor REGION_SAVE_EXECUTOR =
            singleThreadExecutor("recruits-worldmap-save");
    private static final ThreadPoolExecutor RENDER_TILE_EXECUTOR =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new PriorityBlockingQueue<>(),
                    namedDaemonFactory("recruits-worldmap-render-tile"));
    private static final ThreadPoolExecutor CHUNK_BUILD_EXECUTOR =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new PriorityBlockingQueue<>(),
                    namedDaemonFactory("recruits-worldmap-chunk"));

    private WorldMapAsync() {}

    public static void prepareWorkers() {
        REGION_LOAD_EXECUTOR.prestartAllCoreThreads();
        REGION_SAVE_EXECUTOR.prestartAllCoreThreads();
        RENDER_TILE_EXECUTOR.prestartAllCoreThreads();
        CHUNK_BUILD_EXECUTOR.prestartAllCoreThreads();
    }

    public static CompletableFuture<WorldMapRegionPixels> loadRegion(
            boolean urgent,
            long generation,
            int distanceSquared,
            Supplier<WorldMapRegionPixels> supplier) {
        CompletableFuture<WorldMapRegionPixels> future = new CompletableFuture<>();
        RegionLoadTask task =
                new RegionLoadTask(
                        urgent,
                        generation,
                        distanceSquared,
                        REGION_LOAD_SEQUENCE.getAndIncrement(),
                        supplier,
                        future);
        future.whenComplete(
                (ignored, error) -> {
                    if (future.isCancelled()) {
                        REGION_LOAD_EXECUTOR.remove(task);
                    }
                });
        REGION_LOAD_EXECUTOR.execute(task);
        return future;
    }

    public static CompletableFuture<RegionSaveResult> saveRegion(
            File file, WorldMapRegion.SaveSnapshot snapshot) {
        return timedTask(
                () -> {
                    try {
                        WorldMapRegionStorage.write(file, snapshot.pixels());
                        return new RegionSaveResult(true, snapshot.dirtyVersion());
                    } catch (Exception ignored) {
                        return new RegionSaveResult(false, snapshot.dirtyVersion());
                    }
                },
                REGION_SAVE_EXECUTOR);
    }

    public static CompletableFuture<WorldMapRegion.RenderSnapshot> buildRenderTile(
            long generation,
            int distanceSquared,
            Supplier<WorldMapRegion.RenderSnapshot> supplier) {
        CompletableFuture<WorldMapRegion.RenderSnapshot> future = new CompletableFuture<>();
        RenderTileBuildTask task =
                new RenderTileBuildTask(
                        generation,
                        distanceSquared,
                        RENDER_TILE_SEQUENCE.getAndIncrement(),
                        supplier,
                        future);
        future.whenComplete(
                (ignored, error) -> {
                    if (future.isCancelled()) {
                        RENDER_TILE_EXECUTOR.remove(task);
                    }
                });
        RENDER_TILE_EXECUTOR.execute(task);
        return future;
    }

    public static CompletableFuture<ChunkBuildResult> buildChunk(
            boolean urgent, int distanceSquared, Supplier<ChunkBuildResult> supplier) {
        CompletableFuture<ChunkBuildResult> future = new CompletableFuture<>();
        ChunkBuildTask task = new ChunkBuildTask(
                urgent,
                distanceSquared,
                CHUNK_BUILD_SEQUENCE.getAndIncrement(),
                supplier,
                future
        );
        future.whenComplete((ignored, error) -> {
            if (future.isCancelled()) {
                CHUNK_BUILD_EXECUTOR.remove(task);
            }
        });
        CHUNK_BUILD_EXECUTOR.execute(task);
        return future;
    }

    private static <T> CompletableFuture<T> timedTask(Supplier<T> supplier, ExecutorService executor) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private static ThreadFactory namedDaemonFactory(String namePrefix) {
        AtomicInteger index = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, namePrefix + "-" + index.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
            return thread;
        };
    }

    private static ThreadPoolExecutor singleThreadExecutor(String namePrefix) {
        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(),
                namedDaemonFactory(namePrefix));
    }

    public record RegionSaveResult(boolean success, int dirtyVersion) {}

    private static final class RegionLoadTask implements Runnable, Comparable<RegionLoadTask> {
        private final boolean urgent;
        private final long generation;
        private final int distanceSquared;
        private final long sequence;
        private final Supplier<WorldMapRegionPixels> supplier;
        private final CompletableFuture<WorldMapRegionPixels> future;

        private RegionLoadTask(boolean urgent, long generation, int distanceSquared, long sequence,
                               Supplier<WorldMapRegionPixels> supplier,
                               CompletableFuture<WorldMapRegionPixels> future) {
            this.urgent = urgent;
            this.generation = generation;
            this.distanceSquared = distanceSquared;
            this.sequence = sequence;
            this.supplier = supplier;
            this.future = future;
        }

        @Override
        public void run() {
            if (future.isCancelled()) return;

            try {
                WorldMapRegionPixels result = supplier.get();
                future.complete(result);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }

        @Override
        public int compareTo(RegionLoadTask other) {
            if (urgent != other.urgent) return urgent ? -1 : 1;

            int generationOrder = Long.compare(other.generation, generation);
            if (generationOrder != 0) return generationOrder;

            int distanceOrder = Integer.compare(distanceSquared, other.distanceSquared);
            return distanceOrder != 0 ? distanceOrder : Long.compare(sequence, other.sequence);
        }
    }

    private static final class RenderTileBuildTask implements Runnable, Comparable<RenderTileBuildTask> {
        private final long generation;
        private final int distanceSquared;
        private final long sequence;
        private final Supplier<WorldMapRegion.RenderSnapshot> supplier;
        private final CompletableFuture<WorldMapRegion.RenderSnapshot> future;

        private RenderTileBuildTask(
                long generation,
                int distanceSquared,
                long sequence,
                Supplier<WorldMapRegion.RenderSnapshot> supplier,
                CompletableFuture<WorldMapRegion.RenderSnapshot> future) {
            this.generation = generation;
            this.distanceSquared = distanceSquared;
            this.sequence = sequence;
            this.supplier = supplier;
            this.future = future;
        }

        @Override
        public void run() {
            if (future.isCancelled()) return;

            try {
                WorldMapRegion.RenderSnapshot result = supplier.get();
                if (!future.complete(result) && result != null) {
                    result.release();
                }
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }

        @Override
        public int compareTo(RenderTileBuildTask other) {
            int generationOrder = Long.compare(other.generation, generation);
            if (generationOrder != 0) return generationOrder;

            int distanceOrder = Integer.compare(distanceSquared, other.distanceSquared);
            return distanceOrder != 0 ? distanceOrder : Long.compare(sequence, other.sequence);
        }
    }

    private static final class ChunkBuildTask implements Runnable, Comparable<ChunkBuildTask> {
        private final boolean urgent;
        private final int distanceSquared;
        private final long sequence;
        private final Supplier<ChunkBuildResult> supplier;
        private final CompletableFuture<ChunkBuildResult> future;

        private ChunkBuildTask(boolean urgent, int distanceSquared, long sequence,
                               Supplier<ChunkBuildResult> supplier, CompletableFuture<ChunkBuildResult> future) {
            this.urgent = urgent;
            this.distanceSquared = distanceSquared;
            this.sequence = sequence;
            this.supplier = supplier;
            this.future = future;
        }

        @Override
        public void run() {
            if (future.isCancelled()) return;

            try {
                ChunkBuildResult result = supplier.get();
                future.complete(result);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }

        @Override
        public int compareTo(ChunkBuildTask other) {
            if (urgent != other.urgent) return urgent ? -1 : 1;

            int distanceOrder = Integer.compare(distanceSquared, other.distanceSquared);
            return distanceOrder != 0 ? distanceOrder : Long.compare(sequence, other.sequence);
        }
    }
}
