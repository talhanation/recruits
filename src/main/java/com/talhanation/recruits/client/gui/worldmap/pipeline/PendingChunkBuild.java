package com.talhanation.recruits.client.gui.worldmap.pipeline;

import java.util.concurrent.CompletableFuture;

public record PendingChunkBuild(
        ChunkSamplingContext context,
        long revision,
        ChunkImageBuilder builder,
        CompletableFuture<ChunkBuildResult> future,
        boolean urgent,
        boolean forcedRebuild) {
    public void cancel() {
        builder.cancel();
        future.cancel(false);
    }
}
