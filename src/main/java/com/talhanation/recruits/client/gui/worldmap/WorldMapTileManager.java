package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WorldMapTileManager {
    private static final int CHUNK_UPDATES_PER_TICK = 6;
    private static final int MAX_LOADED_REGIONS = 192;
    private static final int MAX_LOD_SOURCE_IMAGES = 16;
    private static final int MAX_REGION_LOAD_SCHEDULES_PER_FRAME = 2;
    private static final int MAX_REGION_LOAD_COMPLETIONS_PER_FRAME = 2;
    private static final int MAX_PENDING_REGION_LOADS = 48;
    private static final int MIN_UPDATE_RADIUS_CHUNKS = 5;
    private static final int MAX_UPDATE_RADIUS_CHUNKS = 12;
    private static final long CHUNK_UPDATE_BUDGET_NANOS = 2_500_000L;
    private static final long QUEUE_REFRESH_INTERVAL_MS = 350L;
    private static final long CHUNK_REFRESH_INTERVAL_MS = 3000L;
    private static final long SAVE_INTERVAL_MS = 2500L;

    private static WorldMapTileManager instance;

    private final Minecraft mc = Minecraft.getInstance();
    private final Map<String, WorldMapRegionTile> loadedRegions = new HashMap<>();
    private final WorldMapLodCache lodCache = new WorldMapLodCache(this);
    private final Map<String, CachedRegionLoad> pendingRegionLoads = new HashMap<>();
    private final Deque<Long> chunkUpdateQueue = new ArrayDeque<>();
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Map<Long, Long> lastChunkUpdateTimes = new HashMap<>();

    private File worldMapDir;
    private File regionDir;
    private long lastQueueRefreshTime;
    private long lastSaveTime;
    private int regionLoadSchedulesLeft;

    public static WorldMapTileManager getInstance() {
        if (instance == null) instance = new WorldMapTileManager();
        return instance;
    }

    public void initialize(Level level) {
        if (level == null) return;

        String worldName = detectStorageId(level);
        File newWorldMapDir = new File(mc.gameDirectory, "recruits/worldmap/" + worldName);
        if (this.worldMapDir != null && this.worldMapDir.equals(newWorldMapDir)) return;

        close();
        this.worldMapDir = newWorldMapDir;
        this.regionDir = new File(newWorldMapDir, "regions");
        this.regionDir.mkdirs();
    }

    public void updateCurrentTile() {
        if (mc.level == null || mc.player == null) return;
        if (this.worldMapDir == null) initialize(mc.level);

        long now = System.currentTimeMillis();
        if (now - lastQueueRefreshTime >= QUEUE_REFRESH_INTERVAL_MS) {
            enqueueLoadedChunksAroundPlayer(now);
            lastQueueRefreshTime = now;
        }

        processQueuedChunks(now);

        if (now - lastSaveTime >= SAVE_INTERVAL_MS) {
            saveDirtyRegions();
            lastSaveTime = now;
        }

        trimLoadedRegions(MAX_LOADED_REGIONS);
        lodCache.trim();
    }

    WorldMapRegionTile getLoadedRegion(int regionX, int regionZ) {
        WorldMapRegionTile region = loadedRegions.get(key(regionX, regionZ));
        if (region != null) region.markAccessed();
        return region;
    }

    void beginRenderFrame() {
        this.regionLoadSchedulesLeft = MAX_REGION_LOAD_SCHEDULES_PER_FRAME;
        consumeReadyRegionLoads(MAX_REGION_LOAD_COMPLETIONS_PER_FRAME);
    }

    WorldMapRegionTile getOrScheduleCachedRegion(int regionX, int regionZ) {
        if (regionDir == null) return null;

        WorldMapRegionTile loaded = getLoadedRegion(regionX, regionZ);
        if (loaded != null) return loaded;

        String regionKey = key(regionX, regionZ);
        CachedRegionLoad pendingLoad = pendingRegionLoads.get(regionKey);
        if (pendingLoad != null) {
            if (pendingLoad.future().isDone()) {
                return consumeReadyRegionLoad(regionKey, pendingLoad);
            }
            return null;
        }

        if (regionLoadSchedulesLeft <= 0 || pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS) return null;

        File regionFile = getRegionFile(regionX, regionZ);
        if (!isUsableImageFile(regionFile)) return null;

        regionLoadSchedulesLeft--;
        CompletableFuture<NativeImage> future = WorldMapAsync.loadRegion(() -> readRegionImageFileIfPresent(regionFile));
        pendingRegionLoads.put(regionKey, new CachedRegionLoad(regionX, regionZ, future));
        return null;
    }

    private void consumeReadyRegionLoads(int maxCompletions) {
        int remaining = maxCompletions;
        Iterator<Map.Entry<String, CachedRegionLoad>> iterator = pendingRegionLoads.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            Map.Entry<String, CachedRegionLoad> entry = iterator.next();
            CachedRegionLoad pendingLoad = entry.getValue();
            if (!pendingLoad.future().isDone()) continue;

            iterator.remove();
            remaining--;
            consumeReadyRegionLoad(entry.getKey(), pendingLoad);
        }
    }

    private WorldMapRegionTile consumeReadyRegionLoad(String regionKey, CachedRegionLoad pendingLoad) {
        pendingRegionLoads.remove(regionKey);

        NativeImage image;
        try {
            image = pendingLoad.future().join();
        } catch (Exception ignored) {
            return null;
        }
        if (image == null) return null;

        if (loadedRegions.containsKey(regionKey)) {
            image.close();
            return loadedRegions.get(regionKey);
        }

        WorldMapRegionTile region = new WorldMapRegionTile(pendingLoad.regionX(), pendingLoad.regionZ());
        region.loadFromImage(image);
        region.markAccessed();
        loadedRegions.put(regionKey, region);
        return region;
    }

    WorldMapLodCache getLodCache() {
        return lodCache;
    }

    NativeImage buildLodImage(int lodTileX, int lodTileZ, int sampleStep) {
        File sourceDir = this.regionDir;
        NativeImage lodImage = new NativeImage(
                NativeImage.Format.RGBA,
                WorldMapRegionTile.REGION_PIXEL_SIZE,
                WorldMapRegionTile.REGION_PIXEL_SIZE,
                false
        );

        if (sourceDir == null) {
            clearImage(lodImage);
            return lodImage;
        }

        LinkedHashMap<String, NativeImage> sourceImages = new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, NativeImage> eldest) {
                if (size() <= MAX_LOD_SOURCE_IMAGES) return false;
                eldest.getValue().close();
                return true;
            }
        };
        Set<String> missingSourceImages = new HashSet<>();

        int tileWorldSize = WorldMapRegionTile.REGION_PIXEL_SIZE * sampleStep;
        int startWorldX = lodTileX * tileWorldSize;
        int startWorldZ = lodTileZ * tileWorldSize;

        try {
            for (int z = 0; z < WorldMapRegionTile.REGION_PIXEL_SIZE; z++) {
                for (int x = 0; x < WorldMapRegionTile.REGION_PIXEL_SIZE; x++) {
                    int worldX = startWorldX + x * sampleStep;
                    int worldZ = startWorldZ + z * sampleStep;
                    int color = sampleLodColor(sourceDir, sourceImages, missingSourceImages, worldX, worldZ, sampleStep);
                    lodImage.setPixelRGBA(x, z, color);
                }
            }
        } finally {
            for (NativeImage sourceImage : sourceImages.values()) {
                sourceImage.close();
            }
        }

        return lodImage;
    }

    void trimLoadedRegions(int maxRegions) {
        if (loadedRegions.size() <= maxRegions) return;

        int protectedRegionX = Integer.MIN_VALUE;
        int protectedRegionZ = Integer.MIN_VALUE;
        if (mc.player != null) {
            protectedRegionX = WorldMapRegionTile.chunkToRegionCoord(mc.player.chunkPosition().x);
            protectedRegionZ = WorldMapRegionTile.chunkToRegionCoord(mc.player.chunkPosition().z);
        }

        ArrayList<WorldMapRegionTile> regions = new ArrayList<>(loadedRegions.values());
        regions.sort(Comparator.comparingLong(WorldMapRegionTile::getLastAccessNanos));

        for (WorldMapRegionTile region : regions) {
            if (loadedRegions.size() <= maxRegions) return;
            if (region.getRegionX() == protectedRegionX && region.getRegionZ() == protectedRegionZ) continue;

            loadedRegions.remove(key(region.getRegionX(), region.getRegionZ()));
            region.saveToFile(getRegionFile(region.getRegionX(), region.getRegionZ()));
            region.close();
        }
    }

    public boolean isChunkExplored(ChunkPos chunk) {
        int regionX = WorldMapRegionTile.chunkToRegionCoord(chunk.x);
        int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunk.z);
        WorldMapRegionTile region = getLoadedRegion(regionX, regionZ);
        if (region == null) region = getOrScheduleCachedRegion(regionX, regionZ);
        if (region == null || region.getImage() == null) return false;

        int localX = WorldMapRegionTile.chunkLocalCoord(chunk.x) * WorldMapRegionTile.PIXELS_PER_CHUNK
                + WorldMapRegionTile.PIXELS_PER_CHUNK / 2;
        int localZ = WorldMapRegionTile.chunkLocalCoord(chunk.z) * WorldMapRegionTile.PIXELS_PER_CHUNK
                + WorldMapRegionTile.PIXELS_PER_CHUNK / 2;

        return ((region.getImage().getPixelRGBA(localX, localZ) >> 24) & 0xFF) > 0;
    }

    public void close() {
        saveDirtyRegions();
        lodCache.close();
        closePendingRegionLoads();

        for (WorldMapRegionTile region : loadedRegions.values()) {
            region.close();
        }
        loadedRegions.clear();
        chunkUpdateQueue.clear();
        queuedChunks.clear();
        lastChunkUpdateTimes.clear();
        worldMapDir = null;
        regionDir = null;
        lastQueueRefreshTime = 0L;
        lastSaveTime = 0L;
    }

    private WorldMapRegionTile getOrCreateRegion(int regionX, int regionZ) {
        String regionKey = key(regionX, regionZ);
        WorldMapRegionTile region = loadedRegions.get(regionKey);
        if (region == null) {
            CachedRegionLoad pendingLoad = pendingRegionLoads.remove(regionKey);
            if (pendingLoad != null) closeFutureImage(pendingLoad.future());

            region = new WorldMapRegionTile(regionX, regionZ);
            region.loadOrCreate(getRegionFile(regionX, regionZ));
            loadedRegions.put(regionKey, region);
        }
        region.markAccessed();
        return region;
    }

    private void enqueueLoadedChunksAroundPlayer(long now) {
        if (mc.level == null || mc.player == null) return;

        int centerChunkX = mc.player.chunkPosition().x;
        int centerChunkZ = mc.player.chunkPosition().z;
        int radius = getUpdateRadiusChunks();

        enqueueChunkIfNeeded(centerChunkX, centerChunkZ, now);
        for (int ring = 1; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                enqueueChunkIfNeeded(centerChunkX + dx, centerChunkZ - ring, now);
                enqueueChunkIfNeeded(centerChunkX + dx, centerChunkZ + ring, now);
            }
            for (int dz = -ring + 1; dz <= ring - 1; dz++) {
                enqueueChunkIfNeeded(centerChunkX - ring, centerChunkZ + dz, now);
                enqueueChunkIfNeeded(centerChunkX + ring, centerChunkZ + dz, now);
            }
        }
    }

    private void enqueueChunkIfNeeded(int chunkX, int chunkZ, long now) {
        long chunkKey = chunkKey(chunkX, chunkZ);
        if (queuedChunks.contains(chunkKey)) return;

        Long lastUpdate = lastChunkUpdateTimes.get(chunkKey);
        if (lastUpdate != null && now - lastUpdate < CHUNK_REFRESH_INTERVAL_MS) return;
        if (!isChunkLoaded(chunkX, chunkZ)) return;

        chunkUpdateQueue.addLast(chunkKey);
        queuedChunks.add(chunkKey);
    }

    private void processQueuedChunks(long now) {
        int updatesLeft = CHUNK_UPDATES_PER_TICK;
        long startNanos = System.nanoTime();
        while (updatesLeft > 0 && !chunkUpdateQueue.isEmpty()) {
            long chunkKey = chunkUpdateQueue.removeFirst();
            queuedChunks.remove(chunkKey);

            int chunkX = chunkX(chunkKey);
            int chunkZ = chunkZ(chunkKey);
            if (isChunkLoaded(chunkX, chunkZ)) {
                updateChunk(chunkX, chunkZ, now);
                updatesLeft--;
            }

            if (updatesLeft < CHUNK_UPDATES_PER_TICK && System.nanoTime() - startNanos >= CHUNK_UPDATE_BUDGET_NANOS) {
                return;
            }
        }
    }

    private void updateChunk(int chunkX, int chunkZ, long now) {
        if (mc.level == null) return;

        int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX);
        int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ);
        WorldMapRegionTile region = getOrCreateRegion(regionX, regionZ);

        ChunkImage chunkImage = new ChunkImage(mc.level, new ChunkPos(chunkX, chunkZ));
        try {
            if (chunkImage.isMeaningful()) {
                region.updateFromChunkImage(
                        chunkImage,
                        WorldMapRegionTile.chunkLocalCoord(chunkX),
                        WorldMapRegionTile.chunkLocalCoord(chunkZ)
                );
                lodCache.invalidateRegion(regionX, regionZ);
            }
        } finally {
            chunkImage.close();
        }

        lastChunkUpdateTimes.put(chunkKey(chunkX, chunkZ), now);
    }

    private boolean isChunkLoaded(int chunkX, int chunkZ) {
        if (mc.level == null) return false;
        try {
            return mc.level.getChunkSource().getChunk(chunkX, chunkZ, false) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int getUpdateRadiusChunks() {
        int radius = 8;
        try {
            radius = mc.options.renderDistance().get();
        } catch (Exception ignored) {
        }
        return Math.max(MIN_UPDATE_RADIUS_CHUNKS, Math.min(MAX_UPDATE_RADIUS_CHUNKS, radius));
    }

    private void saveDirtyRegions() {
        if (regionDir == null) return;
        for (WorldMapRegionTile region : loadedRegions.values()) {
            if (region.isDirty()) {
                region.saveToFile(getRegionFile(region.getRegionX(), region.getRegionZ()));
            }
        }
    }

    private int sampleLodColor(File sourceDir, Map<String, NativeImage> sourceImages, Set<String> missingSourceImages,
                               int worldX, int worldZ, int sampleStep) {
        if (sampleStep <= 1) {
            return getSourcePixel(sourceDir, sourceImages, missingSourceImages, worldX, worldZ);
        }

        int end = sampleStep - 1;
        int mid = sampleStep / 2;
        int center = getSourcePixel(sourceDir, sourceImages, missingSourceImages, worldX + mid, worldZ + mid);
        int topLeft = getSourcePixel(sourceDir, sourceImages, missingSourceImages, worldX, worldZ);
        int topRight = getSourcePixel(sourceDir, sourceImages, missingSourceImages, worldX + end, worldZ);
        int bottomLeft = getSourcePixel(sourceDir, sourceImages, missingSourceImages, worldX, worldZ + end);
        int bottomRight = getSourcePixel(sourceDir, sourceImages, missingSourceImages, worldX + end, worldZ + end);
        return blendColors(center, topLeft, topRight, bottomLeft, bottomRight);
    }

    private int getSourcePixel(File sourceDir, Map<String, NativeImage> sourceImages, Set<String> missingSourceImages,
                               int worldX, int worldZ) {
        int regionX = Math.floorDiv(worldX, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int regionZ = Math.floorDiv(worldZ, WorldMapRegionTile.REGION_PIXEL_SIZE);
        String regionKey = key(regionX, regionZ);
        if (missingSourceImages.contains(regionKey)) return 0x00000000;

        NativeImage image = sourceImages.get(regionKey);
        if (image == null) {
            image = readRegionImageFileIfPresent(sourceDir, regionX, regionZ);
            if (image == null) {
                missingSourceImages.add(regionKey);
                return 0x00000000;
            }
            sourceImages.put(regionKey, image);
        }
        if (image == null) return 0x00000000;

        int localX = Math.floorMod(worldX, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int localZ = Math.floorMod(worldZ, WorldMapRegionTile.REGION_PIXEL_SIZE);
        return image.getPixelRGBA(localX, localZ);
    }

    private NativeImage readRegionImageFileIfPresent(File sourceDir, int regionX, int regionZ) {
        File regionFile = getRegionFile(sourceDir, regionX, regionZ);
        return readRegionImageFileIfPresent(regionFile);
    }

    private NativeImage readRegionImageFileIfPresent(File regionFile) {
        if (!isUsableImageFile(regionFile)) return null;

        try {
            byte[] fileData = java.nio.file.Files.readAllBytes(regionFile.toPath());
            NativeImage image = NativeImage.read(fileData);
            if (image.getWidth() != WorldMapRegionTile.REGION_PIXEL_SIZE
                    || image.getHeight() != WorldMapRegionTile.REGION_PIXEL_SIZE) {
                image.close();
                return null;
            }
            return image;
        } catch (IOException ignored) {
            return null;
        }
    }

    private File getRegionFile(int regionX, int regionZ) {
        return getRegionFile(regionDir, regionX, regionZ);
    }

    private static File getRegionFile(File sourceDir, int regionX, int regionZ) {
        return new File(sourceDir, regionX + "_" + regionZ + ".png");
    }

    private static boolean isUsableImageFile(File imageFile) {
        return imageFile != null && imageFile.exists() && imageFile.length() > 0;
    }

    private static int blendColors(int center, int topLeft, int topRight, int bottomLeft, int bottomRight) {
        int[] colors = {center, center, topLeft, topRight, bottomLeft, bottomRight};
        int count = 0;
        int red = 0;
        int green = 0;
        int blue = 0;

        for (int color : colors) {
            if (((color >> 24) & 0xFF) == 0) continue;
            count++;
            red += (color >> 16) & 0xFF;
            green += (color >> 8) & 0xFF;
            blue += color & 0xFF;
        }

        if (count == 0) return 0x00000000;
        return 0xFF000000
                | ((red / count) << 16)
                | ((green / count) << 8)
                | (blue / count);
    }

    private static void clearImage(NativeImage image) {
        for (int z = 0; z < image.getHeight(); z++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setPixelRGBA(x, z, 0x00000000);
            }
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private static int chunkX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    private static int chunkZ(long chunkKey) {
        return (int) chunkKey;
    }

    private static String key(int x, int z) {
        return x + "_" + z;
    }

    private void closePendingRegionLoads() {
        for (CachedRegionLoad pendingLoad : pendingRegionLoads.values()) {
            closeFutureImage(pendingLoad.future());
        }
        pendingRegionLoads.clear();
    }

    private static void closeFutureImage(CompletableFuture<NativeImage> future) {
        if (future.isDone()) {
            try {
                NativeImage image = future.getNow(null);
                if (image != null) image.close();
            } catch (Exception ignored) {
            }
        } else {
            future.thenAccept(image -> {
                if (image != null) image.close();
            });
        }
    }

    private static String detectStorageId(Level level) {
        try {
            Minecraft mc = Minecraft.getInstance();
            String dimension = level.dimension().location().toString();
            if (mc.getSingleplayerServer() != null) {
                var server = mc.getSingleplayerServer();
                java.nio.file.Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                long seed = 0L;
                try {
                    seed = server.overworld().getSeed();
                } catch (Exception ignored) {
                }
                return stableId("sp", root + "|seed=" + seed + "|dim=" + dimension);
            }
            ServerData sd = mc.getCurrentServer();
            if (sd != null && sd.ip != null && !sd.ip.isEmpty())
                return stableId("mp", sd.ip + "|dim=" + dimension);
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private static String stableId(String prefix, String rawId) {
        return prefix + "_" + UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));
    }

    private record CachedRegionLoad(int regionX, int regionZ, CompletableFuture<NativeImage> future) {
    }
}
