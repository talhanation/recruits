package com.talhanation.recruits.client.gui.worldmap.pipeline;

import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapSourceChunk;

public record ChunkBuildResult(
        int chunkX,
        int chunkZ,
        long chunkKey,
        int[] pixels,
        WorldMapSourceChunk sourceChunk) {
}
