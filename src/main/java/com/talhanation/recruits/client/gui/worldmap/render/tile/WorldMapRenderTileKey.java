package com.talhanation.recruits.client.gui.worldmap.render.tile;

import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegion;

public record WorldMapRenderTileKey(int level, int x, int z) {
    public static final int PIXEL_SIZE = 64;
    public static final int MAX_LEVEL = 2;

    public int worldSize() {
        return PIXEL_SIZE << level;
    }

    public int worldMinX() {
        return x * worldSize();
    }

    public int worldMinZ() {
        return z * worldSize();
    }

    public WorldMapRenderTileKey parent() {
        return level >= MAX_LEVEL
                ? null
                : new WorldMapRenderTileKey(level + 1, Math.floorDiv(x, 2), Math.floorDiv(z, 2));
    }

    public WorldMapRenderTileKey child(int childX, int childZ) {
        return level <= 0 ? null : new WorldMapRenderTileKey(level - 1, x * 2 + childX, z * 2 + childZ);
    }

    public int regionX() {
        return Math.floorDiv(worldMinX(), WorldMapRegion.REGION_PIXEL_SIZE);
    }

    public int regionZ() {
        return Math.floorDiv(worldMinZ(), WorldMapRegion.REGION_PIXEL_SIZE);
    }

    public int localXInRegion() {
        return Math.floorMod(worldMinX(), WorldMapRegion.REGION_PIXEL_SIZE) / worldSize();
    }

    public int localZInRegion() {
        return Math.floorMod(worldMinZ(), WorldMapRegion.REGION_PIXEL_SIZE) / worldSize();
    }

    public static WorldMapRenderTileKey fromRegionLocal(
            int level, int regionX, int regionZ, int localX, int localZ) {
        int tilesPerRegion = WorldMapRegion.REGION_PIXEL_SIZE / (PIXEL_SIZE << level);
        return new WorldMapRenderTileKey(
                level, regionX * tilesPerRegion + localX, regionZ * tilesPerRegion + localZ);
    }
}
