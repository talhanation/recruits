package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Captures the loaded chunks needed to render one map chunk. Cooperative map
 * building can then sample a stable, bounded set of chunk references without
 * repeatedly querying the client chunk source.
 */
final class ChunkSamplingContext {
    private static final int NEIGHBOR_RADIUS = 1;
    private static final int GRID_SIZE = NEIGHBOR_RADIUS * 2 + 1;

    private final ClientLevel level;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final LevelChunk[] chunks;

    private ChunkSamplingContext(ClientLevel level, int centerChunkX, int centerChunkZ, LevelChunk[] chunks) {
        this.level = level;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.chunks = chunks;
    }

    static ChunkSamplingContext capture(ClientLevel level, int centerChunkX, int centerChunkZ) {
        if (level == null) return null;

        LevelChunk[] chunks = new LevelChunk[GRID_SIZE * GRID_SIZE];
        for (int dz = -NEIGHBOR_RADIUS; dz <= NEIGHBOR_RADIUS; dz++) {
            for (int dx = -NEIGHBOR_RADIUS; dx <= NEIGHBOR_RADIUS; dx++) {
                LevelChunk chunk = getLoadedChunk(level, centerChunkX + dx, centerChunkZ + dz);
                if (chunk == null) return null;
                chunks[index(dx, dz)] = chunk;
            }
        }

        return new ChunkSamplingContext(level, centerChunkX, centerChunkZ, chunks);
    }

    ClientLevel level() {
        return level;
    }

    LevelChunk getLoadedChunk(int worldX, int worldZ) {
        int dx = (worldX >> 4) - centerChunkX;
        int dz = (worldZ >> 4) - centerChunkZ;
        if (Math.abs(dx) > NEIGHBOR_RADIUS || Math.abs(dz) > NEIGHBOR_RADIUS) return null;
        return chunks[index(dx, dz)];
    }

    boolean belongsTo(ClientLevel currentLevel) {
        return level == currentLevel;
    }

    private static int index(int dx, int dz) {
        return (dz + NEIGHBOR_RADIUS) * GRID_SIZE + dx + NEIGHBOR_RADIUS;
    }

    private static LevelChunk getLoadedChunk(ClientLevel level, int chunkX, int chunkZ) {
        try {
            return level.getChunkSource().getChunk(chunkX, chunkZ, false);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
