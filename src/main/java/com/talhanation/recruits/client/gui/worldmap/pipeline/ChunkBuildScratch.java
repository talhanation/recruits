package com.talhanation.recruits.client.gui.worldmap.pipeline;

import com.talhanation.recruits.client.gui.worldmap.color.MapSample;
import com.talhanation.recruits.client.gui.worldmap.color.MapTintSampler;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegion;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ChunkBuildScratch {
    public static final int SAMPLE_GRID_SIZE = WorldMapRegion.PIXELS_PER_CHUNK + 2;
    public static final int SAMPLE_COUNT = SAMPLE_GRID_SIZE * SAMPLE_GRID_SIZE;
    private static final int MAX_POOLED_SCRATCHES = 8;
    private static final Deque<ChunkBuildScratch> POOL = new ArrayDeque<>();

    private final MapSample[] surfaceSamples = createSamples();
    private final MapSample[] underlaySamples = createSamples();
    private final MapTintSampler tintSampler = new MapTintSampler();

    public MapSample[] surfaceSamples() {
        return surfaceSamples;
    }

    public MapSample[] underlaySamples() {
        return underlaySamples;
    }

    public MapTintSampler tintSampler() {
        return tintSampler;
    }

    public void prepare(ChunkSamplingContext context) {
        tintSampler.prepare(context);
    }

    public static ChunkBuildScratch acquire() {
        synchronized (POOL) {
            ChunkBuildScratch scratch = POOL.pollFirst();
            return scratch != null ? scratch : new ChunkBuildScratch();
        }
    }

    public static void preparePool() {
        synchronized (POOL) {
            while (POOL.size() < MAX_POOLED_SCRATCHES) {
                POOL.addFirst(new ChunkBuildScratch());
            }
        }
    }

    public void release() {
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
