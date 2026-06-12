package com.talhanation.recruits.client.gui.worldmap.storage;

import java.util.Arrays;

/**
 * Saved chunk samples. Needed when cached pixels have to be recolored later.
 */
public final class WorldMapSourceChunk {
    static final int SAMPLE_COUNT =
            WorldMapRegion.PIXELS_PER_CHUNK * WorldMapRegion.PIXELS_PER_CHUNK;
    static final int MISSING = -1;

    private final int[] surfaceStates;
    private final int[] surfaceBiomes;
    private final int[] surfaceHeights;
    private final int[] underlayStates;
    private final int[] underlayBiomes;
    private final int[] underlayHeights;

    private WorldMapSourceChunk(
            int[] surfaceStates,
            int[] surfaceBiomes,
            int[] surfaceHeights,
            int[] underlayStates,
            int[] underlayBiomes,
            int[] underlayHeights) {
        this.surfaceStates = surfaceStates;
        this.surfaceBiomes = surfaceBiomes;
        this.surfaceHeights = surfaceHeights;
        this.underlayStates = underlayStates;
        this.underlayBiomes = underlayBiomes;
        this.underlayHeights = underlayHeights;
    }

    public static Builder builder() {
        return new Builder();
    }

    SourceSample surface(int index) {
        return sample(surfaceStates, surfaceBiomes, surfaceHeights, index);
    }

    SourceSample underlay(int index) {
        return sample(underlayStates, underlayBiomes, underlayHeights, index);
    }

    int[] surfaceStates() {
        return surfaceStates;
    }

    int[] surfaceBiomes() {
        return surfaceBiomes;
    }

    int[] surfaceHeights() {
        return surfaceHeights;
    }

    int[] underlayStates() {
        return underlayStates;
    }

    int[] underlayBiomes() {
        return underlayBiomes;
    }

    int[] underlayHeights() {
        return underlayHeights;
    }

    private static SourceSample sample(int[] states, int[] biomes, int[] heights, int index) {
        if (index < 0 || index >= SAMPLE_COUNT) return SourceSample.MISSING_SAMPLE;
        int stateId = states[index];
        return stateId < 0
                ? SourceSample.MISSING_SAMPLE
                : new SourceSample(stateId, biomes[index], heights[index]);
    }

    public record SourceSample(int stateId, int biomeId, int height) {
        static final SourceSample MISSING_SAMPLE = new SourceSample(MISSING, MISSING, MISSING);

        boolean isPresent() {
            return stateId >= 0;
        }
    }

    public static final class Builder {
        private final int[] surfaceStates = missingArray();
        private final int[] surfaceBiomes = missingArray();
        private final int[] surfaceHeights = missingArray();
        private final int[] underlayStates = missingArray();
        private final int[] underlayBiomes = missingArray();
        private final int[] underlayHeights = missingArray();
        private boolean hasSurface;

        private Builder() {}

        public void setSurface(int index, int stateId, int biomeId, int height) {
            set(surfaceStates, surfaceBiomes, surfaceHeights, index, stateId, biomeId, height);
            hasSurface = true;
        }

        public void setUnderlay(int index, int stateId, int biomeId, int height) {
            set(underlayStates, underlayBiomes, underlayHeights, index, stateId, biomeId, height);
        }

        public WorldMapSourceChunk build() {
            if (!hasSurface) return null;

            return new WorldMapSourceChunk(
                    Arrays.copyOf(surfaceStates, SAMPLE_COUNT),
                    Arrays.copyOf(surfaceBiomes, SAMPLE_COUNT),
                    Arrays.copyOf(surfaceHeights, SAMPLE_COUNT),
                    Arrays.copyOf(underlayStates, SAMPLE_COUNT),
                    Arrays.copyOf(underlayBiomes, SAMPLE_COUNT),
                    Arrays.copyOf(underlayHeights, SAMPLE_COUNT));
        }

        private static void set(
                int[] states, int[] biomes, int[] heights, int index, int stateId, int biomeId, int height) {
            if (index < 0 || index >= SAMPLE_COUNT || stateId < 0) return;
            states[index] = stateId;
            biomes[index] = biomeId;
            heights[index] = height;
        }
    }

    private static int[] missingArray() {
        int[] values = new int[SAMPLE_COUNT];
        Arrays.fill(values, MISSING);
        return values;
    }
}
