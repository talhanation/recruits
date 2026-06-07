package com.talhanation.recruits.client.gui.worldmap.storage;

final class WorldMapRegionKey {
    private WorldMapRegionKey() {}

    static String of(int regionX, int regionZ) {
        return regionX + "_" + regionZ;
    }
}
