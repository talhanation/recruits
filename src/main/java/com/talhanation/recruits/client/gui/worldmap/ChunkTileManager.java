package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ChunkTileManager {
    private static ChunkTileManager instance;
    private final Map<String, ChunkTile> loadedTiles = new HashMap<>();
    private final Minecraft mc = Minecraft.getInstance();
    private File worldMapDir;
    private int currentTileX = Integer.MAX_VALUE;
    private int currentTileZ = Integer.MAX_VALUE;
    private final Map<String, Long> lastUpdateTimes = new HashMap<>();
    private long lastNeighborUpdateTime = 0;
    private int lastUpdatedNeighborIndex = 0;

    public static ChunkTileManager getInstance() {
        if (instance == null) instance = new ChunkTileManager();
        return instance;
    }

    public void initialize(Level level) {
        if (level == null) return;
        String worldName = detectStorageId();
        this.worldMapDir = new File(mc.gameDirectory, "recruits/worldmap/" + worldName);
        this.worldMapDir.mkdirs();
    }

    public void updateCurrentTile() {
        if (mc.level == null || mc.player == null) return;

        int chunkX = mc.player.chunkPosition().x;
        int chunkZ = mc.player.chunkPosition().z;
        int tileX = ChunkTile.chunkToTileCoord(chunkX);
        int tileZ = ChunkTile.chunkToTileCoord(chunkZ);
        String currentTileKey = tileX + "_" + tileZ;

        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTimes.get(currentTileKey);

        if (tileX != currentTileX || tileZ != currentTileZ ||
                lastUpdate == null || currentTime - lastUpdate > 1000) {
            updateTile(tileX, tileZ);
            currentTileX = tileX;
            currentTileZ = tileZ;
        }

        if (currentTime - lastNeighborUpdateTime >= 500) {
            updateOneNeighborTile(tileX, tileZ);
            lastNeighborUpdateTime = currentTime;
        }
    }

    private void updateOneNeighborTile(int centerX, int centerZ) {
        int[][] neighbors = {
                {centerX-1, centerZ-1}, {centerX, centerZ-1}, {centerX+1, centerZ-1},
                {centerX-1, centerZ}, {centerX+1, centerZ},
                {centerX-1, centerZ+1}, {centerX, centerZ+1}, {centerX+1, centerZ+1}
        };

        if (lastUpdatedNeighborIndex >= neighbors.length) lastUpdatedNeighborIndex = 0;
        int[] neighbor = neighbors[lastUpdatedNeighborIndex];

        String neighborKey = neighbor[0] + "_" + neighbor[1];
        Long neighborLastUpdate = lastUpdateTimes.get(neighborKey);
        if (neighborLastUpdate == null || System.currentTimeMillis() - neighborLastUpdate > 10000) {
            updateTile(neighbor[0], neighbor[1]);
        }
        lastUpdatedNeighborIndex++;
    }

    private void updateTile(int tileX, int tileZ) {
        ChunkTile tile = getOrCreateTile(tileX, tileZ);
        File tileFile = getTileFile(tileX, tileZ);
        if (tileFile.exists()) tile.mergeWithExistingTile(tileFile);
        updateOnlyLoadedChunks(tile);
        tile.saveToFile(tileFile);
        lastUpdateTimes.put(tileX + "_" + tileZ, System.currentTimeMillis());
    }

    private void updateOnlyLoadedChunks(ChunkTile tile) {
        if (mc.level == null || mc.player == null) return;

        int startChunkX = ChunkTile.tileToChunkCoord(tile.getTileX());
        int startChunkZ = ChunkTile.tileToChunkCoord(tile.getTileZ());

        for (int cz = 0; cz < ChunkTile.TILE_SIZE; cz++) {
            for (int cx = 0; cx < ChunkTile.TILE_SIZE; cx++) {
                ChunkPos chunkPos = new ChunkPos(startChunkX + cx, startChunkZ + cz);
                if (isChunkLoaded(chunkPos)) {
                    ChunkImage chunkImage = new ChunkImage(mc.level, chunkPos);
                    tile.updateFromChunkImage(chunkImage, cx, cz);
                    chunkImage.close();
                }
            }
        }
    }

    private boolean isChunkLoaded(ChunkPos chunkPos) {
        if (mc.level == null || mc.player == null) return false;
        try {
            return mc.level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public ChunkTile getOrCreateTile(int tileX, int tileZ) {
        String key = tileX + "_" + tileZ;
        ChunkTile tile = loadedTiles.get(key);
        if (tile == null) {
            tile = new ChunkTile(tileX, tileZ);
            tile.loadOrCreate(getTileFile(tileX, tileZ));
            loadedTiles.put(key, tile);
        }
        tile.markAccessed();
        return tile;
    }

    private static String detectStorageId() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                String levelName = mc.getSingleplayerServer().getWorldData().getLevelName();
                if (levelName != null && !levelName.isEmpty())
                    return levelName.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
            }
            ServerData sd = mc.getCurrentServer();
            if (sd != null && sd.ip != null && !sd.ip.isEmpty())
                return sd.ip.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        } catch (Exception ignored) {}
        return "unknown";
    }

    private File getTileFile(int tileX, int tileZ) {
        return new File(worldMapDir, tileX + "_" + tileZ + ".png");
    }

    public void close() {
        for (ChunkTile tile : loadedTiles.values()) {
            tile.saveToFile(getTileFile(tile.getTileX(), tile.getTileZ()));
            tile.close();
        }
        loadedTiles.clear();
    }

    public Map<String, ChunkTile> getLoadedTiles() {
        return loadedTiles;
    }
}
