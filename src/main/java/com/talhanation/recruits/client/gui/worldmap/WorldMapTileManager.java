package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import com.talhanation.recruits.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WorldMapTileManager {
    private static final int CHUNK_UPDATES_PER_TICK = 6;
    private static final int MAX_LOADED_REGIONS = 192;
    private static final int MAX_REGION_LOAD_SCHEDULES_PER_FRAME = 8;
    private static final int MAX_REGION_LOAD_COMPLETIONS_PER_FRAME = 8;
    private static final int MAX_PENDING_REGION_LOADS = 96;
    private static final int MAX_VISIBLE_REGION_PREFETCH_RADIUS = 12;
    private static final int MIN_UPDATE_RADIUS_CHUNKS = 5;
    private static final int MAX_UPDATE_RADIUS_CHUNKS = 12;
    private static final int MAX_REGION_SAVES_PER_PASS = 1;
    private static final long CHUNK_UPDATE_BUDGET_NANOS = 2_500_000L;
    private static final long REGION_LOAD_RETRY_DELAY_MS = 1000L;
    private static final long QUEUE_REFRESH_INTERVAL_MS = 350L;
    private static final long CHUNK_REFRESH_INTERVAL_MS = 3000L;
    private static final long SAVE_INTERVAL_MS = 750L;

    private static WorldMapTileManager instance;

    private final Minecraft mc = Minecraft.getInstance();
    private final Map<String, WorldMapRegionTile> loadedRegions = new ConcurrentHashMap<>();
    private final WorldMapLodCache lodCache = new WorldMapLodCache(this);
    private final Map<String, CachedRegionLoad> pendingRegionLoads = new HashMap<>();
    private final Map<String, Long> failedRegionLoads = new HashMap<>();
    private final Set<String> persistedRegionSources = ConcurrentHashMap.newKeySet();
    private final Deque<Long> chunkUpdateQueue = new ArrayDeque<>();
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Map<Long, Long> lastChunkUpdateTimes = new HashMap<>();
    private ChunkImageBuilder activeChunkBuilder;

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
        rebuildRegionSourceIndex();
    }

    public void updateCurrentTile() {
        if (mc.level == null || mc.player == null) return;
        if (this.worldMapDir == null || isUsingUnknownStorageId()) initialize(mc.level);

        long debugStartNanos = System.nanoTime();
        consumeReadyRegionLoads(MAX_REGION_LOAD_COMPLETIONS_PER_FRAME);

        long now = System.currentTimeMillis();
        if (now - lastQueueRefreshTime >= QUEUE_REFRESH_INTERVAL_MS) {
            enqueueLoadedChunksAroundPlayer(now);
            lastQueueRefreshTime = now;
        }

        int chunkUpdates = processQueuedChunks(now);

        if (now - lastSaveTime >= SAVE_INTERVAL_MS) {
            if (chunkUpdates == 0) {
                saveDirtyRegions(MAX_REGION_SAVES_PER_PASS);
            }
            lastSaveTime = now;
        }

        trimLoadedRegions(MAX_LOADED_REGIONS);
        lodCache.trim();
        recordDebugState();
        WorldMapDebugProfiler.recordTileUpdate(System.nanoTime() - debugStartNanos, chunkUpdates,
                chunkUpdateQueue.size(), queuedChunks.size());
    }

    WorldMapRegionTile getLoadedRegion(int regionX, int regionZ) {
        WorldMapRegionTile region = loadedRegions.get(key(regionX, regionZ));
        if (region != null) region.markAccessed();
        return region;
    }

    void beginRenderFrame() {
        this.regionLoadSchedulesLeft = MAX_REGION_LOAD_SCHEDULES_PER_FRAME;
        consumeReadyRegionLoads(MAX_REGION_LOAD_COMPLETIONS_PER_FRAME);
        recordDebugState();
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

        File regionFile = getReadableRegionFile(regionX, regionZ);
        scheduleRegionLoad(regionKey, regionX, regionZ, regionFile, true);
        return null;
    }

    void scheduleVisibleRegionLoads(double leftWorld, double rightWorld, double topWorld, double bottomWorld) {
        if (regionDir == null) return;

        int startRegionX = (int) Math.floor(leftWorld / WorldMapRegionTile.REGION_PIXEL_SIZE) - 1;
        int endRegionX = (int) Math.ceil(rightWorld / WorldMapRegionTile.REGION_PIXEL_SIZE) + 1;
        int startRegionZ = (int) Math.floor(topWorld / WorldMapRegionTile.REGION_PIXEL_SIZE) - 1;
        int endRegionZ = (int) Math.ceil(bottomWorld / WorldMapRegionTile.REGION_PIXEL_SIZE) + 1;

        int centerRegionX = (int) Math.floor(((leftWorld + rightWorld) * 0.5) / WorldMapRegionTile.REGION_PIXEL_SIZE);
        int centerRegionZ = (int) Math.floor(((topWorld + bottomWorld) * 0.5) / WorldMapRegionTile.REGION_PIXEL_SIZE);

        startRegionX = Math.max(startRegionX, centerRegionX - MAX_VISIBLE_REGION_PREFETCH_RADIUS);
        endRegionX = Math.min(endRegionX, centerRegionX + MAX_VISIBLE_REGION_PREFETCH_RADIUS);
        startRegionZ = Math.max(startRegionZ, centerRegionZ - MAX_VISIBLE_REGION_PREFETCH_RADIUS);
        endRegionZ = Math.min(endRegionZ, centerRegionZ + MAX_VISIBLE_REGION_PREFETCH_RADIUS);

        ArrayList<RegionLoadCandidate> candidates = new ArrayList<>();
        for (int regionZ = startRegionZ; regionZ <= endRegionZ; regionZ++) {
            for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
                String regionKey = key(regionX, regionZ);
                if (loadedRegions.containsKey(regionKey) || pendingRegionLoads.containsKey(regionKey)) continue;
                if (isRegionLoadRetryBlocked(regionKey)) continue;
                if (!persistedRegionSources.contains(regionKey)) continue;

                File regionFile = getReadableRegionFile(regionX, regionZ);
                if (!isUsableImageFile(regionFile)) continue;

                double dx = regionX + 0.5 - centerRegionX;
                double dz = regionZ + 0.5 - centerRegionZ;
                candidates.add(new RegionLoadCandidate(regionX, regionZ, dx * dx + dz * dz, regionFile));
            }
        }

        candidates.sort(Comparator.comparingDouble(RegionLoadCandidate::centerDistance));
        for (RegionLoadCandidate candidate : candidates) {
            if (regionLoadSchedulesLeft <= 0) return;
            scheduleRegionLoad(key(candidate.regionX(), candidate.regionZ()), candidate.regionX(), candidate.regionZ(),
                    candidate.regionFile(), true);
        }
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
            recordFailedRegionLoad(regionKey);
            WorldMapDebugProfiler.recordRegionLoadCompleted(false);
            return null;
        }
        if (image == null) {
            recordFailedRegionLoad(regionKey);
            WorldMapDebugProfiler.recordRegionLoadCompleted(false);
            return null;
        }

        if (loadedRegions.containsKey(regionKey)) {
            image.close();
            WorldMapDebugProfiler.recordRegionLoadCompleted(true);
            return loadedRegions.get(regionKey);
        }

        WorldMapRegionTile region = new WorldMapRegionTile(pendingLoad.regionX(), pendingLoad.regionZ());
        region.loadFromImage(image);
        region.markAccessed();
        loadedRegions.put(regionKey, region);
        persistedRegionSources.add(regionKey);
        failedRegionLoads.remove(regionKey);
        lodCache.invalidateRegion(pendingLoad.regionX(), pendingLoad.regionZ());
        WorldMapDebugProfiler.recordRegionLoadCompleted(true);
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

        clearImage(lodImage);
        Set<String> unreadableSourceImages = new HashSet<>();

        int tileWorldSize = WorldMapRegionTile.REGION_PIXEL_SIZE * sampleStep;
        int startWorldX = lodTileX * tileWorldSize;
        int startWorldZ = lodTileZ * tileWorldSize;
        int endWorldX = startWorldX + tileWorldSize - 1;
        int endWorldZ = startWorldZ + tileWorldSize - 1;
        int startRegionX = Math.floorDiv(startWorldX, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int endRegionX = Math.floorDiv(endWorldX, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int startRegionZ = Math.floorDiv(startWorldZ, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int endRegionZ = Math.floorDiv(endWorldZ, WorldMapRegionTile.REGION_PIXEL_SIZE);

        boolean foundSource = false;
        int visiblePixels = 0;
        for (int regionZ = startRegionZ; regionZ <= endRegionZ; regionZ++) {
            for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
                String regionKey = key(regionX, regionZ);
                NativeImage sourceImage = copyLoadedRegionImage(regionX, regionZ);
                if (sourceImage == null) {
                    sourceImage = readSourceRegionImage(sourceDir, regionX, regionZ, regionKey, unreadableSourceImages);
                }
                if (sourceImage == null) continue;

                foundSource = true;
                try {
                    visiblePixels += writeSourceRegionToLod(lodImage, sourceImage, regionX, regionZ,
                            startWorldX, startWorldZ, sampleStep);
                } finally {
                    sourceImage.close();
                }
            }
        }

        if (visiblePixels == 0 && !foundSource) {
            lodImage.close();
            return null;
        }

        return lodImage;
    }

    private int writeSourceRegionToLod(NativeImage lodImage, NativeImage sourceImage,
                                       int regionX, int regionZ,
                                       int lodStartWorldX, int lodStartWorldZ,
                                       int sampleStep) {
        int sourceWorldX = regionX * WorldMapRegionTile.REGION_PIXEL_SIZE;
        int sourceWorldZ = regionZ * WorldMapRegionTile.REGION_PIXEL_SIZE;
        int outputStartX = Math.max(0, (sourceWorldX - lodStartWorldX) / sampleStep);
        int outputStartZ = Math.max(0, (sourceWorldZ - lodStartWorldZ) / sampleStep);
        int outputEndX = Math.min(WorldMapRegionTile.REGION_PIXEL_SIZE,
                (sourceWorldX + WorldMapRegionTile.REGION_PIXEL_SIZE - lodStartWorldX) / sampleStep);
        int outputEndZ = Math.min(WorldMapRegionTile.REGION_PIXEL_SIZE,
                (sourceWorldZ + WorldMapRegionTile.REGION_PIXEL_SIZE - lodStartWorldZ) / sampleStep);

        int visiblePixels = 0;
        for (int z = outputStartZ; z < outputEndZ; z++) {
            int sourceZ = lodStartWorldZ + z * sampleStep - sourceWorldZ;
            for (int x = outputStartX; x < outputEndX; x++) {
                int sourceX = lodStartWorldX + x * sampleStep - sourceWorldX;
                int color = sampleSourceRegionColor(sourceImage, sourceX, sourceZ, sampleStep);
                if (isVisible(color)) visiblePixels++;
                lodImage.setPixelRGBA(x, z, color);
            }
        }
        return visiblePixels;
    }

    private int sampleSourceRegionColor(NativeImage sourceImage, int sourceX, int sourceZ, int sampleStep) {
        if (sampleStep <= 1) {
            return getLocalSourcePixel(sourceImage, sourceX, sourceZ);
        }
        return sampleSharpSourceRegionColor(sourceImage, sourceX, sourceZ, sampleStep);
    }

    private int sampleSharpSourceRegionColor(NativeImage sourceImage, int sourceX, int sourceZ, int sampleStep) {
        int end = sampleStep - 1;
        int mid = sampleStep / 2;
        int center = getLocalSourcePixel(sourceImage, sourceX + mid, sourceZ + mid);
        if (isVisible(center)) return center;

        int top = getLocalSourcePixel(sourceImage, sourceX + mid, sourceZ);
        if (isVisible(top)) return top;
        int bottom = getLocalSourcePixel(sourceImage, sourceX + mid, sourceZ + end);
        if (isVisible(bottom)) return bottom;
        int left = getLocalSourcePixel(sourceImage, sourceX, sourceZ + mid);
        if (isVisible(left)) return left;
        int right = getLocalSourcePixel(sourceImage, sourceX + end, sourceZ + mid);
        if (isVisible(right)) return right;
        return 0x00000000;
    }

    private static int getLocalSourcePixel(NativeImage image, int x, int z) {
        if (x < 0 || z < 0 || x >= image.getWidth() || z >= image.getHeight()) {
            return 0x00000000;
        }
        return image.getPixelRGBA(x, z);
    }

    boolean mayHaveLodSourceData(int lodTileX, int lodTileZ, int sampleStep) {
        int tileWorldSize = WorldMapRegionTile.REGION_PIXEL_SIZE * sampleStep;
        int startWorldX = lodTileX * tileWorldSize;
        int startWorldZ = lodTileZ * tileWorldSize;
        int endWorldX = startWorldX + tileWorldSize - 1;
        int endWorldZ = startWorldZ + tileWorldSize - 1;

        int startRegionX = Math.floorDiv(startWorldX, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int endRegionX = Math.floorDiv(endWorldX, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int startRegionZ = Math.floorDiv(startWorldZ, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int endRegionZ = Math.floorDiv(endWorldZ, WorldMapRegionTile.REGION_PIXEL_SIZE);

        for (int regionZ = startRegionZ; regionZ <= endRegionZ; regionZ++) {
            for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
                if (hasRegionSourceInMemory(regionX, regionZ)) return true;
            }
        }
        return false;
    }

    private boolean hasRegionSourceInMemory(int regionX, int regionZ) {
        String regionKey = key(regionX, regionZ);
        return loadedRegions.containsKey(regionKey) || persistedRegionSources.contains(regionKey);
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
            saveRegion(region);
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
        persistedRegionSources.clear();
        failedRegionLoads.clear();
        chunkUpdateQueue.clear();
        queuedChunks.clear();
        lastChunkUpdateTimes.clear();
        closeActiveChunkBuilder();
        worldMapDir = null;
        regionDir = null;
        lastQueueRefreshTime = 0L;
        lastSaveTime = 0L;
    }

    public void flush() {
        saveDirtyRegions();
        lastSaveTime = System.currentTimeMillis();
    }

    private WorldMapRegionTile getOrCreateRegion(int regionX, int regionZ) {
        String regionKey = key(regionX, regionZ);
        WorldMapRegionTile region = loadedRegions.get(regionKey);
        if (region == null) {
            CachedRegionLoad pendingLoad = pendingRegionLoads.get(regionKey);
            if (pendingLoad != null) {
                return pendingLoad.future().isDone() ? consumeReadyRegionLoad(regionKey, pendingLoad) : null;
            }

            File regionFile = getReadableRegionFile(regionX, regionZ);
            if (isUsableImageFile(regionFile)) {
                if (!isRegionLoadRetryBlocked(regionKey)) {
                    scheduleRegionLoad(regionKey, regionX, regionZ, regionFile, false);
                }
                return null;
            }

            region = new WorldMapRegionTile(regionX, regionZ);
            region.createBlank();
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
        if (activeChunkBuilder != null && activeChunkBuilder.chunkKey() == chunkKey) return;

        Long lastUpdate = lastChunkUpdateTimes.get(chunkKey);
        if (lastUpdate != null && now - lastUpdate < CHUNK_REFRESH_INTERVAL_MS) return;
        if (!isChunkLoaded(chunkX, chunkZ)) return;

        chunkUpdateQueue.addLast(chunkKey);
        queuedChunks.add(chunkKey);
    }

    private int processQueuedChunks(long now) {
        int updatesDone = 0;
        long deadlineNanos = System.nanoTime() + CHUNK_UPDATE_BUDGET_NANOS;

        while (updatesDone < CHUNK_UPDATES_PER_TICK && System.nanoTime() < deadlineNanos) {
            if (activeChunkBuilder == null && !startNextChunkBuilder()) {
                break;
            }

            if (activeChunkBuilder == null) {
                continue;
            }

            if (!activeChunkBuilder.workUntil(deadlineNanos)) {
                break;
            }

            finishActiveChunkBuilder(now);
            updatesDone++;
        }

        return updatesDone;
    }

    private boolean startNextChunkBuilder() {
        if (mc.level == null) return false;

        while (!chunkUpdateQueue.isEmpty()) {
            long chunkKey = chunkUpdateQueue.removeFirst();
            queuedChunks.remove(chunkKey);

            int chunkX = chunkX(chunkKey);
            int chunkZ = chunkZ(chunkKey);
            if (!isChunkLoaded(chunkX, chunkZ)) continue;

            int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX);
            int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ);
            if (getOrCreateRegion(regionX, regionZ) == null) continue;

            activeChunkBuilder = new ChunkImageBuilder(mc.level, chunkX, chunkZ, chunkKey);
            return true;
        }

        return false;
    }

    private void finishActiveChunkBuilder(long now) {
        ChunkImageBuilder builder = activeChunkBuilder;
        activeChunkBuilder = null;
        if (builder == null) return;

        try {
            int chunkX = builder.chunkX();
            int chunkZ = builder.chunkZ();
            long chunkKey = builder.chunkKey();
            if (!isChunkLoaded(chunkX, chunkZ)) {
                lastChunkUpdateTimes.put(chunkKey, now);
                return;
            }

            int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX);
            int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ);
            WorldMapRegionTile region = getOrCreateRegion(regionX, regionZ);
            if (region == null) return;

            if (!builder.isMeaningful()) {
                lastChunkUpdateTimes.put(chunkKey, now);
                return;
            }

            ChunkImage chunkImage = builder.takeImage();
            if (chunkImage == null) return;

            try {
                region.updateFromChunkImage(
                        chunkImage,
                        WorldMapRegionTile.chunkLocalCoord(chunkX),
                        WorldMapRegionTile.chunkLocalCoord(chunkZ)
                );
                lodCache.invalidateRegion(regionX, regionZ);
            } finally {
                chunkImage.close();
            }

            lastChunkUpdateTimes.put(chunkKey, now);
        } finally {
            builder.close();
        }
    }

    private void closeActiveChunkBuilder() {
        if (activeChunkBuilder == null) return;

        activeChunkBuilder.close();
        activeChunkBuilder = null;
    }

    private boolean scheduleRegionLoad(String regionKey, int regionX, int regionZ, File regionFile, boolean useFrameBudget) {
        if (isRegionLoadRetryBlocked(regionKey)) return false;
        if (!isUsableImageFile(regionFile)) return false;
        if (pendingRegionLoads.containsKey(regionKey)) return false;
        if (pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS) return false;
        if (useFrameBudget) {
            if (regionLoadSchedulesLeft <= 0) return false;
            regionLoadSchedulesLeft--;
        }

        CompletableFuture<NativeImage> future = WorldMapAsync.loadRegion(() -> readRegionImageFileIfPresent(regionFile));
        pendingRegionLoads.put(regionKey, new CachedRegionLoad(regionX, regionZ, future));
        persistedRegionSources.add(regionKey);
        WorldMapDebugProfiler.recordRegionLoadScheduled(useFrameBudget);
        return true;
    }

    private boolean isRegionLoadRetryBlocked(String regionKey) {
        Long failedAt = failedRegionLoads.get(regionKey);
        if (failedAt == null) return false;
        if (System.currentTimeMillis() - failedAt < REGION_LOAD_RETRY_DELAY_MS) return true;

        failedRegionLoads.remove(regionKey);
        return false;
    }

    private void recordFailedRegionLoad(String regionKey) {
        failedRegionLoads.put(regionKey, System.currentTimeMillis());
    }

    private void recordDebugState() {
        WorldMapDebugProfiler.recordTileManagerState(
                loadedRegions.size(),
                pendingRegionLoads.size(),
                failedRegionLoads.size(),
                chunkUpdateQueue.size(),
                queuedChunks.size(),
                lodCache.size(),
                lodCache.pendingCount()
        );
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
        saveDirtyRegions(Integer.MAX_VALUE);
    }

    private void saveDirtyRegions(int maxRegions) {
        if (regionDir == null) return;
        int saved = 0;
        for (WorldMapRegionTile region : loadedRegions.values()) {
            if (region.isDirty()) {
                saveRegion(region);
                saved++;
                if (saved >= maxRegions) return;
            }
        }
    }

    private void saveRegion(WorldMapRegionTile region) {
        if (region.saveToFile(getRegionFile(region.getRegionX(), region.getRegionZ()))) {
            String regionKey = key(region.getRegionX(), region.getRegionZ());
            persistedRegionSources.add(regionKey);
            failedRegionLoads.remove(regionKey);
        }
    }

    private NativeImage readSourceRegionImage(File sourceDir, int regionX, int regionZ, String regionKey,
                                              Set<String> unreadableSourceImages) {
        File regionFile = getRegionFile(sourceDir, regionX, regionZ);
        boolean expectedSource = isUsableImageFile(regionFile);
        NativeImage image = readRegionImageFileIfPresent(regionFile);
        if (image != null) return image;

        if (sourceDir != null && regionDir != null && sourceDir.equals(regionDir) && worldMapDir != null) {
            File legacyRegionFile = getRegionFile(worldMapDir, regionX, regionZ);
            if (!legacyRegionFile.equals(regionFile)) {
                expectedSource |= isUsableImageFile(legacyRegionFile);
                image = readRegionImageFileIfPresent(legacyRegionFile);
                if (image != null) return image;
            }
        }

        if (expectedSource) {
            unreadableSourceImages.add(regionKey);
        }
        return null;
    }

    private NativeImage copyLoadedRegionImage(int regionX, int regionZ) {
        WorldMapRegionTile region = loadedRegions.get(key(regionX, regionZ));
        if (region == null) return null;
        return region.copyImage();
    }

    private NativeImage readRegionImageFileIfPresent(File sourceDir, int regionX, int regionZ) {
        File regionFile = getRegionFile(sourceDir, regionX, regionZ);
        NativeImage image = readRegionImageFileIfPresent(regionFile);
        if (image != null) return image;

        if (sourceDir != null && regionDir != null && sourceDir.equals(regionDir) && worldMapDir != null) {
            File legacyRegionFile = getRegionFile(worldMapDir, regionX, regionZ);
            if (!legacyRegionFile.equals(regionFile)) {
                return readRegionImageFileIfPresent(legacyRegionFile);
            }
        }
        return null;
    }

    private NativeImage readRegionImageFileIfPresent(File regionFile) {
        if (!isUsableImageFile(regionFile)) return null;

        try {
            try (FileInputStream stream = new FileInputStream(regionFile)) {
                NativeImage image = NativeImage.read(stream);
                if (image.getWidth() != WorldMapRegionTile.REGION_PIXEL_SIZE
                        || image.getHeight() != WorldMapRegionTile.REGION_PIXEL_SIZE) {
                    image.close();
                    Main.LOGGER.warn("Ignoring world map region {} with unexpected size {}x{}",
                            regionFile, image.getWidth(), image.getHeight());
                    return null;
                }
                return image;
            }
        } catch (IOException | RuntimeException exception) {
            Main.LOGGER.warn("Failed to load world map region {}", regionFile, exception);
            return null;
        }
    }

    private File getRegionFile(int regionX, int regionZ) {
        return getRegionFile(regionDir, regionX, regionZ);
    }

    private void rebuildRegionSourceIndex() {
        persistedRegionSources.clear();
        indexRegionSourceDir(regionDir);
        if (worldMapDir != null && !worldMapDir.equals(regionDir)) {
            indexRegionSourceDir(worldMapDir);
        }
    }

    private void indexRegionSourceDir(File sourceDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) return;

        File[] files = sourceDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String regionKey = regionKeyFromFileName(file);
            if (regionKey != null) {
                persistedRegionSources.add(regionKey);
            }
        }
    }

    private static String regionKeyFromFileName(File file) {
        if (!isUsableImageFile(file)) return null;

        String name = file.getName();
        if (!name.endsWith(".png")) return null;

        String coords = name.substring(0, name.length() - 4);
        int separator = coords.indexOf('_');
        if (separator <= 0 || separator >= coords.length() - 1) return null;

        try {
            int regionX = Integer.parseInt(coords.substring(0, separator));
            int regionZ = Integer.parseInt(coords.substring(separator + 1));
            return key(regionX, regionZ);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private File getReadableRegionFile(int regionX, int regionZ) {
        File primaryRegionFile = getRegionFile(regionX, regionZ);
        if (isUsableImageFile(primaryRegionFile) || worldMapDir == null) return primaryRegionFile;

        File legacyRegionFile = getRegionFile(worldMapDir, regionX, regionZ);
        if (!legacyRegionFile.equals(primaryRegionFile) && isUsableImageFile(legacyRegionFile)) {
            return legacyRegionFile;
        }
        return primaryRegionFile;
    }

    private static File getRegionFile(File sourceDir, int regionX, int regionZ) {
        return new File(sourceDir, regionX + "_" + regionZ + ".png");
    }

    private static boolean isUsableImageFile(File imageFile) {
        return imageFile != null && imageFile.exists() && imageFile.length() > 0;
    }

    private static boolean isVisible(int color) {
        return ((color >> 24) & 0xFF) > 0;
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

    private boolean isUsingUnknownStorageId() {
        return worldMapDir != null && "unknown".equals(worldMapDir.getName());
    }

    private static String stableId(String prefix, String rawId) {
        return prefix + "_" + UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));
    }

    private record CachedRegionLoad(int regionX, int regionZ, CompletableFuture<NativeImage> future) {
    }

    private record RegionLoadCandidate(int regionX, int regionZ, double centerDistance, File regionFile) {
    }
}
