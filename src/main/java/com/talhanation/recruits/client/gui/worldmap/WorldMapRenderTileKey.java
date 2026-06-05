package com.talhanation.recruits.client.gui.worldmap;

record WorldMapRenderTileKey(int level, int x, int z) {
    static final int PIXEL_SIZE = 128;
    static final int MAX_LEVEL = 2;

    int worldSize() {
        return PIXEL_SIZE << level;
    }

    int worldMinX() {
        return x * worldSize();
    }

    int worldMinZ() {
        return z * worldSize();
    }

    WorldMapRenderTileKey parent() {
        return level >= MAX_LEVEL ? null : new WorldMapRenderTileKey(level + 1, Math.floorDiv(x, 2), Math.floorDiv(z, 2));
    }

    WorldMapRenderTileKey child(int childX, int childZ) {
        return level <= 0 ? null : new WorldMapRenderTileKey(level - 1, x * 2 + childX, z * 2 + childZ);
    }

    int regionX() {
        return Math.floorDiv(worldMinX(), WorldMapRegionTile.REGION_PIXEL_SIZE);
    }

    int regionZ() {
        return Math.floorDiv(worldMinZ(), WorldMapRegionTile.REGION_PIXEL_SIZE);
    }

    int localXInRegion() {
        return Math.floorMod(worldMinX(), WorldMapRegionTile.REGION_PIXEL_SIZE) / worldSize();
    }

    int localZInRegion() {
        return Math.floorMod(worldMinZ(), WorldMapRegionTile.REGION_PIXEL_SIZE) / worldSize();
    }

    static WorldMapRenderTileKey fromRegionLocal(int level, int regionX, int regionZ,
                                                  int localX, int localZ) {
        int tilesPerRegion = WorldMapRegionTile.REGION_PIXEL_SIZE / (PIXEL_SIZE << level);
        return new WorldMapRenderTileKey(
                level,
                regionX * tilesPerRegion + localX,
                regionZ * tilesPerRegion + localZ
        );
    }
}
