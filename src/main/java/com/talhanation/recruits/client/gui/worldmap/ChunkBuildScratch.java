package com.talhanation.recruits.client.gui.worldmap;

import java.util.ArrayDeque;
import java.util.Deque;

final class ChunkBuildScratch {
    static final int SAMPLE_GRID_SIZE = WorldMapRegionTile.PIXELS_PER_CHUNK + 2;
    static final int SAMPLE_COUNT = SAMPLE_GRID_SIZE * SAMPLE_GRID_SIZE;
    private static final int MAX_POOLED_SCRATCHES = 8;
    private static final Deque<ChunkBuildScratch> POOL = new ArrayDeque<>();

    private final MapSample[] surfaceSamples = createSamples();
    private final MapSample[] underlaySamples = createSamples();
    private final MapTintSampler tintSampler = new MapTintSampler();

    MapSample[] surfaceSamples() {
        return surfaceSamples;
    }

    MapSample[] underlaySamples() {
        return underlaySamples;
    }

    MapTintSampler tintSampler() {
        return tintSampler;
    }

    void prepare(ChunkSamplingContext context) {
        tintSampler.prepare(context);
    }

    static ChunkBuildScratch acquire() {
        synchronized (POOL) {
            ChunkBuildScratch scratch = POOL.pollFirst();
            return scratch != null ? scratch : new ChunkBuildScratch();
        }
    }

    static void preparePool() {
        synchronized (POOL) {
            while (POOL.size() < MAX_POOLED_SCRATCHES) {
                POOL.addFirst(new ChunkBuildScratch());
            }
        }
    }

    void release() {
        tintSampler.clear();
        synchronized (POOL) {
            if (POOL.size() < MAX_POOLED_SCRATCHES) {
                POOL.addFirst(this);
            }
        }
    }

    private static MapSample[] createSamples() {
        MapSample[] samples = new MapSample[SAMPLE_COUNT];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = new MapSample();
        }
        return samples;
    }
}
