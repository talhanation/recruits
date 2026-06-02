package com.talhanation.recruits.client.gui.worldmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

final class WorldMapLodCache {
    private static final int MAX_SCHEDULES_PER_FRAME = 4;
    private static final int MAX_PENDING_LOD_TILES = 12;
    private static final int MAX_LOD_TILES = 192;

    private final WorldMapTileManager tileManager;
    private final Map<String, WorldMapLodTile> tiles = new HashMap<>();
    private int schedulesLeft;
    private int pendingTiles;

    WorldMapLodCache(WorldMapTileManager tileManager) {
        this.tileManager = tileManager;
    }

    void beginFrame() {
        this.schedulesLeft = MAX_SCHEDULES_PER_FRAME;
        this.pendingTiles = countPendingTiles();
    }

    WorldMapLodTile getOrSchedule(int tileX, int tileZ, int sampleStep) {
        String key = key(tileX, tileZ, sampleStep);
        WorldMapLodTile tile = tiles.get(key);
        if (tile == null) {
            tile = new WorldMapLodTile(tileX, tileZ, sampleStep);
            tiles.put(key, tile);
        }

        tile.markAccessed();
        if (!tile.isScheduled() && tile.needsBuild() && schedulesLeft > 0 && pendingTiles < MAX_PENDING_LOD_TILES) {
            schedulesLeft--;
            pendingTiles++;
            WorldMapLodTile scheduledTile = tile;
            tile.schedule(() -> tileManager.buildLodImage(tileX, tileZ, scheduledTile.getSampleStep()));
        }
        return tile;
    }

    WorldMapLodTile getIfPresent(int tileX, int tileZ, int sampleStep) {
        WorldMapLodTile tile = tiles.get(key(tileX, tileZ, sampleStep));
        if (tile != null) tile.markAccessed();
        return tile;
    }

    void invalidateRegion(int regionX, int regionZ) {
        int regionMinX = regionX * WorldMapRegionTile.REGION_PIXEL_SIZE;
        int regionMinZ = regionZ * WorldMapRegionTile.REGION_PIXEL_SIZE;
        int regionMaxX = regionMinX + WorldMapRegionTile.REGION_PIXEL_SIZE;
        int regionMaxZ = regionMinZ + WorldMapRegionTile.REGION_PIXEL_SIZE;

        for (WorldMapLodTile tile : tiles.values()) {
            if (intersects(tile, regionMinX, regionMinZ, regionMaxX, regionMaxZ)) {
                tile.markDirty();
            }
        }
    }

    void trim() {
        if (tiles.size() <= MAX_LOD_TILES) return;

        ArrayList<WorldMapLodTile> sortedTiles = new ArrayList<>(tiles.values());
        sortedTiles.sort(Comparator.comparingLong(WorldMapLodTile::getLastAccessNanos));

        for (WorldMapLodTile tile : sortedTiles) {
            if (tiles.size() <= MAX_LOD_TILES) return;
            tiles.values().remove(tile);
            tile.close();
        }
    }

    void close() {
        for (WorldMapLodTile tile : tiles.values()) {
            tile.close();
        }
        tiles.clear();
    }

    private static boolean intersects(WorldMapLodTile tile, int minX, int minZ, int maxX, int maxZ) {
        return tile.getWorldMinX() < maxX
                && tile.getWorldMaxX() > minX
                && tile.getWorldMinZ() < maxZ
                && tile.getWorldMaxZ() > minZ;
    }

    private int countPendingTiles() {
        int count = 0;
        for (WorldMapLodTile tile : tiles.values()) {
            if (tile.isBuildPending()) count++;
        }
        return count;
    }

    private static String key(int tileX, int tileZ, int sampleStep) {
        return sampleStep + "_" + tileX + "_" + tileZ;
    }
}