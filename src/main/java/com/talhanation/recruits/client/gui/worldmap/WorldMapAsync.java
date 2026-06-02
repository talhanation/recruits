package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

final class WorldMapAsync {
    private static final ExecutorService REGION_IO_EXECUTOR = Executors.newSingleThreadExecutor(
            namedDaemonFactory("recruits-worldmap-io"));
    private static final ExecutorService LOD_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4)),
            namedDaemonFactory("recruits-worldmap-lod"));

    private WorldMapAsync() {
    }

    static CompletableFuture<NativeImage> loadRegion(Supplier<NativeImage> supplier) {
        return CompletableFuture.supplyAsync(supplier, REGION_IO_EXECUTOR);
    }

    static CompletableFuture<NativeImage> buildLod(Supplier<NativeImage> supplier) {
        return CompletableFuture.supplyAsync(supplier, LOD_EXECUTOR);
    }

    private static ThreadFactory namedDaemonFactory(String namePrefix) {
        AtomicInteger index = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, namePrefix + "-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
