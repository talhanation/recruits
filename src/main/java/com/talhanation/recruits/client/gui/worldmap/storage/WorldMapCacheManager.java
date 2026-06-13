package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.worldmap.color.MapBlockColorResolver;
import com.talhanation.recruits.client.gui.worldmap.color.MapStateClassifier;
import com.talhanation.recruits.client.gui.worldmap.pipeline.ChunkBuildResult;
import com.talhanation.recruits.client.gui.worldmap.pipeline.ChunkBuildScratch;
import com.talhanation.recruits.client.gui.worldmap.pipeline.ChunkImageBuilder;
import com.talhanation.recruits.client.gui.worldmap.pipeline.ChunkSamplingContext;
import com.talhanation.recruits.client.gui.worldmap.pipeline.PendingChunkBuild;
import com.talhanation.recruits.client.gui.worldmap.pipeline.WorldMapAsync;
import com.talhanation.recruits.client.gui.worldmap.pipeline.WorldMapChunkBuildQueue;
import com.talhanation.recruits.client.gui.worldmap.render.WorldMapRenderer;
import com.talhanation.recruits.client.gui.worldmap.render.tile.WorldMapRenderTileCache;
import com.talhanation.recruits.config.RecruitsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;

import java.io.File;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class WorldMapCacheManager {
    private static final int MAX_CHUNK_BUILD_SCHEDULES_PER_TICK = 8;
    private static final int MAX_CHUNK_BUILD_SCHEDULE_ATTEMPTS_PER_TICK = 512;
    private static final int MAX_CHUNK_BUILD_COMPLETIONS_PER_TICK = 24;
    private static final int MAX_PENDING_CHUNK_BUILDS = 12;
    private static final int MAX_CACHED_CHUNK_COMPLETIONS_PER_TICK = 64;
    private static final int MAX_TRACKED_CHUNKS = 8192;
    private static final int MAX_LOADED_REGIONS = 192;
    private static final int MAX_REGION_LOAD_SCHEDULES_PER_FRAME = 4;
    private static final int MAX_REGION_LOAD_COMPLETIONS_PER_FRAME = 8;
    private static final int MAX_PENDING_REGION_LOADS = 24;
    private static final int MAX_REGION_WAKE_CHUNKS_PER_TICK = 64;
    private static final int MAX_REGION_SAVE_COMPLETIONS_PER_FRAME = 8;
    private static final int MAX_PENDING_REGION_SAVES = 8;
    private static final int MAX_SOURCE_RECOLOR_SCHEDULES_PER_TICK = 2;
    private static final int MAX_SOURCE_RECOLOR_COMPLETIONS_PER_TICK = 4;
    private static final int MAX_PENDING_SOURCE_RECOLORS = 2;
    private static final int MIN_UPDATE_RADIUS_CHUNKS = 5;
    private static final int MAX_UPDATE_RADIUS_CHUNKS = 16;
    private static final int MAX_REGION_SAVES_PER_PASS = 1;
    private static final int MAX_QUEUED_CHUNK_UPDATES = 1024;
    private static final int MAX_CHUNK_ENQUEUE_CHECKS_PER_REFRESH = 512;
    private static final long CHUNK_ENQUEUE_BUDGET_NANOS = 2_000_000L;
    private static final long CHUNK_BUILD_SCHEDULE_BUDGET_NANOS = 1_000_000L;
    private static final long REGION_WAKE_BUDGET_NANOS = 500_000L;
    private static final long CHUNK_LOAD_SETTLE_NANOS = 100_000_000L;
    private static final long BLOCK_UPDATE_SETTLE_NANOS = 75_000_000L;
    private static final long INCOMPLETE_CHUNK_RETRY_NANOS = 100_000_000L;
    private static final long REGION_LOAD_RETRY_DELAY_MS = 1000L;
    private static final long DISCOVERY_INTERVAL_MS = 100L;
    private static final long SAVE_INTERVAL_MS = 60000L;
    private static final long REGION_SAVE_QUIET_PERIOD_NANOS = 3_000_000_000L;
    private static final ChunkScanOffset[] CHUNK_SCAN_OFFSETS = buildChunkScanOffsets();

    private static WorldMapCacheManager instance;

    private final Minecraft mc = Minecraft.getInstance();
    private final WorldMapRenderTileCache renderTileCache = new WorldMapRenderTileCache(this);
    private final WorldMapRegionRepository regionRepository = new WorldMapRegionRepository();
    private final WorldMapChunkBuildQueue chunkBuildQueue = new WorldMapChunkBuildQueue();
    private final Map<String, WorldMapRegion> loadedRegions = new HashMap<>();
    private final Map<String, PendingRegionLoad> pendingRegionLoads = new HashMap<>();
    private final Map<String, Long> failedRegionLoads = new HashMap<>();
    private final Deque<String> sourceRecolorQueue = new ArrayDeque<>();
    private final Set<String> queuedSourceRecolors = new HashSet<>();
    private final Map<String, PendingSourceRecolor> pendingSourceRecolors = new HashMap<>();
    private final WorldMapRegionSaveQueue regionSaveQueue =
            new WorldMapRegionSaveQueue(regionRepository, failedRegionLoads::remove);
    private final Map<Long, Boolean> discoveredChunks =
            new LinkedHashMap<>(1024, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > MAX_TRACKED_CHUNKS;
                }
            };

    private File worldMapDir;
    private long lastDiscoveryTime;
    private long lastSaveTime;
    private boolean savePassPending;
    private long lastEnqueueCenterChunkKey = Long.MIN_VALUE;
    private long regionAccessSequence;
    private int regionLoadSchedulesLeft;
    private int mapColorEpoch = 1;
    private boolean initialModelBakeSeen;
    private boolean initialBlockAtlasStitchSeen;
    private boolean pendingColorRefresh;
    private boolean previousPlayerAreaUpdatesEnabled = true;
    private int sourceRecolorTotalRegions;
    private int sourceRecolorCompletedRegions;
    private int chunkScanOffsetCursor;

    public static WorldMapCacheManager getInstance() {
        if (instance == null) instance = new WorldMapCacheManager();
        return instance;
    }

    public void initialize(Level level) {
        if (level == null) return;

        String worldName = WorldMapStorageId.detect(level);
        File newWorldMapDir = new File(mc.gameDirectory, "recruits/worldmap/" + worldName);
        if (this.worldMapDir != null && this.worldMapDir.equals(newWorldMapDir)) {
            runPendingColorRefresh();
            return;
        }

        close();
        this.worldMapDir = newWorldMapDir;
        regionRepository.open(newWorldMapDir);
        WorldMapAsync.prepareWorkers();
        ChunkBuildScratch.preparePool();
        WorldMapRegion.prepareRenderPixelPool();
        WorldMapRenderer.prepareSharedResources();
        renderTileCache.prepareGpuResources();
        runPendingColorRefresh();
        Main.LOGGER.info(
                "[WorldMap] Initialized compact source region cache at {} with {} indexed regions",
                regionRepository.regionDir(),
                regionRepository.indexedRegionCount());
    }

    public void onClientModelsReloaded() {
        initialModelBakeSeen = true;
        prepareClientResourceCaches();
    }

    public void onClientBlockAtlasStitched() {
        boolean resourceReload = initialBlockAtlasStitchSeen;
        // First stitch is startup; only later stitches need recolor.
        initialBlockAtlasStitchSeen = true;
        prepareClientResourceCaches();
        if (!resourceReload) return;

        advanceMapColorEpoch();
        queueSourceColorRefresh();
    }

    private void prepareClientResourceCaches() {
        WorldMapAsync.prepareWorkers();
        ChunkBuildScratch.preparePool();
        WorldMapRegion.prepareRenderPixelPool();
        MapBlockColorResolver.clearCaches();
        MapStateClassifier.clearCaches();
    }

    private void runPendingColorRefresh() {
        if (!pendingColorRefresh) return;

        pendingColorRefresh = false;
        queueSourceColorRefresh();
    }

    private void queueSourceColorRefresh() {
        if (mc.level == null
                || worldMapDir == null
                || !regionRepository.isReady()) {
            pendingColorRefresh = true;
            return;
        }

        clearChunkBuildPipeline();
        clearSourceRecolorPipeline();
        renderTileCache.clearTiles();

        // Recolor saved .rmap regions first; loaded chunks are just a fallback.
        for (Map.Entry<String, WorldMapRegion> entry : loadedRegions.entrySet()) {
            if (entry.getValue().hasSourceData()) {
                enqueueSourceRecolor(entry.getKey());
            }
        }
        for (String regionKey : regionRepository.indexedSourceDataRegionKeysSnapshot()) {
            enqueueSourceRecolor(regionKey);
        }

        if (sourceRecolorTotalRegions > 0) {
            Main.LOGGER.info(
                    "[WorldMap] Queued {} source regions for resource-pack color refresh",
                    sourceRecolorTotalRegions);
            return;
        }

        queueNearbyLoadedChunksForColorRefresh();
    }

    private void queueNearbyLoadedChunksForColorRefresh() {
        if (mc.level == null
                || mc.player == null
                || worldMapDir == null
                || !regionRepository.isReady()) {
            pendingColorRefresh = true;
            return;
        }

        // Do not reset regions; replace visible chunks as they are rebuilt.
        clearChunkBuildPipeline();

        int enqueued = enqueueNearbyLoadedChunksForColorRefresh();
        Main.LOGGER.info("[WorldMap] Queued {} loaded chunks for map color refresh", enqueued);
    }

    private void clearChunkBuildPipeline() {
        chunkBuildQueue.clear();
        discoveredChunks.clear();
        lastEnqueueCenterChunkKey = Long.MIN_VALUE;
        lastDiscoveryTime = 0L;
        regionLoadSchedulesLeft = 0;
        chunkScanOffsetCursor = 0;
    }

    private void clearSourceRecolorPipeline() {
        for (PendingSourceRecolor pendingRecolor : pendingSourceRecolors.values()) {
            pendingRecolor.future().cancel(false);
        }
        pendingSourceRecolors.clear();
        sourceRecolorQueue.clear();
        queuedSourceRecolors.clear();
        sourceRecolorTotalRegions = 0;
        sourceRecolorCompletedRegions = 0;
    }

    private void enqueueSourceRecolor(String regionKey) {
        if (regionKey == null
                || queuedSourceRecolors.contains(regionKey)
                || pendingSourceRecolors.containsKey(regionKey)) {
            return;
        }

        queuedSourceRecolors.add(regionKey);
        sourceRecolorQueue.addLast(regionKey);
        sourceRecolorTotalRegions++;
    }

    private void advanceMapColorEpoch() {
        mapColorEpoch = mapColorEpoch == Integer.MAX_VALUE ? 1 : mapColorEpoch + 1;
    }

    public void updateCurrentTile() {
        if (mc.level == null || mc.player == null) return;
        if (this.worldMapDir == null || isUsingUnknownStorageId()) initialize(mc.level);

        boolean updatePlayerArea = shouldUpdateAroundPlayer();
        if (!updatePlayerArea && previousPlayerAreaUpdatesEnabled) {
            clearChunkBuildPipeline();
        }
        previousPlayerAreaUpdatesEnabled = updatePlayerArea;

        consumeReadyRegionLoads(MAX_REGION_LOAD_COMPLETIONS_PER_FRAME);
        regionSaveQueue.consumeReadySaves(MAX_REGION_SAVE_COMPLETIONS_PER_FRAME);
        consumeReadySourceRecolors(MAX_SOURCE_RECOLOR_COMPLETIONS_PER_TICK);
        scheduleSourceRecolors(MAX_SOURCE_RECOLOR_SCHEDULES_PER_TICK);

        int chunkUpdates = 0;
        long now = System.currentTimeMillis();
        if (updatePlayerArea) {
            processRegionWakeQueue(MAX_REGION_WAKE_CHUNKS_PER_TICK, REGION_WAKE_BUDGET_NANOS);

            if (shouldDiscoverLoadedChunks(now)) {
                discoverLoadedChunksAroundPlayer();
                lastDiscoveryTime = now;
            }

            chunkUpdates = consumeReadyChunkBuilds(MAX_CHUNK_BUILD_COMPLETIONS_PER_TICK);
            scheduleChunkBuilds(MAX_CHUNK_BUILD_SCHEDULES_PER_TICK);
        }

        if (now - lastSaveTime >= SAVE_INTERVAL_MS) {
            savePassPending = true;
        }
        if (savePassPending
                && chunkUpdates == 0
                && sourceRecolorQueue.isEmpty()
                && pendingSourceRecolors.isEmpty()
                && regionSaveQueue.pendingCount() < MAX_PENDING_REGION_SAVES) {
            int scheduledSaves =
                    regionSaveQueue.saveDirtyRegions(
                            loadedRegions,
                            MAX_REGION_SAVES_PER_PASS,
                            false,
                            MAX_PENDING_REGION_SAVES,
                            REGION_SAVE_QUIET_PERIOD_NANOS);
            if (scheduledSaves == 0
                    && regionSaveQueue.pendingCount() == 0
                    && !regionSaveQueue.hasDirtyRegions(loadedRegions)) {
                savePassPending = false;
                lastSaveTime = now;
            }
        }

        trimLoadedRegions(MAX_LOADED_REGIONS);
    }

    private boolean shouldDiscoverLoadedChunks(long now) {
        if (mc.player == null) return false;

        long centerChunkKey = chunkKey(mc.player.chunkPosition().x, mc.player.chunkPosition().z);
        return centerChunkKey != lastEnqueueCenterChunkKey
                || now - lastDiscoveryTime >= DISCOVERY_INTERVAL_MS;
    }

    public WorldMapRegion getLoadedRegion(int regionX, int regionZ) {
        WorldMapRegion region = loadedRegions.get(WorldMapRegionKey.of(regionX, regionZ));
        return region == null ? null : touchRegion(region);
    }

    public WorldMapRenderTileCache getRenderTileCache() {
        return renderTileCache;
    }

    public boolean shouldLoadViewedRegions() {
        return RecruitsClientConfig.WorldMapLoadViewedRegions.get();
    }

    public void beginRenderFrame() {
        this.regionLoadSchedulesLeft = MAX_REGION_LOAD_SCHEDULES_PER_FRAME;
        consumeReadyRegionLoads(MAX_REGION_LOAD_COMPLETIONS_PER_FRAME);
        regionSaveQueue.consumeReadySaves(MAX_REGION_SAVE_COMPLETIONS_PER_FRAME);
    }

    public WorldMapRegion getOrSchedulePersistedRegion(
            int regionX, int regionZ, long generation, int distanceSquared) {
        return getOrSchedulePersistedRegion(regionX, regionZ, true, generation, distanceSquared);
    }

    private WorldMapRegion getOrSchedulePersistedRegion(
            int regionX, int regionZ, boolean useFrameBudget, long generation, int distanceSquared) {
        if (!regionRepository.isReady()) return null;

        WorldMapRegion loaded = getLoadedRegion(regionX, regionZ);
        if (loaded != null) return loaded;

        String regionKey = WorldMapRegionKey.of(regionX, regionZ);
        PendingRegionLoad pendingLoad = pendingRegionLoads.get(regionKey);
        if (pendingLoad != null) {
            return null;
        }

        if (!regionRepository.hasSource(regionKey)) return null;
        scheduleRegionLoad(
                regionKey, regionX, regionZ, useFrameBudget, true, generation, distanceSquared);
        return null;
    }

    private void consumeReadyRegionLoads(int maxCompletions) {
        int remaining = maxCompletions;
        Iterator<Map.Entry<String, PendingRegionLoad>> iterator =
                pendingRegionLoads.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            Map.Entry<String, PendingRegionLoad> entry = iterator.next();
            PendingRegionLoad pendingLoad = entry.getValue();
            if (!pendingLoad.future().isDone()) continue;

            iterator.remove();
            remaining--;
            consumeReadyRegionLoad(entry.getKey(), pendingLoad);
        }
        scheduleWaitingRegionLoads(Math.max(1, maxCompletions - remaining));
    }

    private WorldMapRegion consumeReadyRegionLoad(String regionKey, PendingRegionLoad pendingLoad) {
        pendingRegionLoads.remove(regionKey);

        try {
            WorldMapRegionRepository.WorldMapRegionLoadResult result;
            try {
                result = pendingLoad.future().join();
            } catch (CancellationException ignored) {
                return null;
            } catch (Exception ignored) {
                recordFailedRegionLoad(regionKey);
                return null;
            }
            if (result == null || result.pixels() == null) {
                regionRepository.removeMissingSource(
                        regionKey, pendingLoad.regionX(), pendingLoad.regionZ());
                recordFailedRegionLoad(regionKey);
                return null;
            }

            if (loadedRegions.containsKey(regionKey)) {
                return loadedRegions.get(regionKey);
            }

            WorldMapRegion region = new WorldMapRegion(pendingLoad.regionX(), pendingLoad.regionZ());
            region.loadFromPixelsAndSource(result.pixels(), result.source(), mapColorEpoch);
            touchRegion(region);
            loadedRegions.put(regionKey, region);
            regionRepository.recordSource(regionKey);
            if (result.source() != null) {
                regionRepository.recordSourceData(regionKey);
            }
            failedRegionLoads.remove(regionKey);
            return region;
        } finally {
            queueRegionWake(regionKey);
        }
    }

    public void trimLoadedRegions(int maxRegions) {
        if (loadedRegions.size() <= maxRegions) return;

        int protectedRegionX = Integer.MIN_VALUE;
        int protectedRegionZ = Integer.MIN_VALUE;
        if (mc.player != null) {
            protectedRegionX = WorldMapRegion.chunkToRegionCoord(mc.player.chunkPosition().x);
            protectedRegionZ = WorldMapRegion.chunkToRegionCoord(mc.player.chunkPosition().z);
        }

        ArrayList<WorldMapRegion> regions = new ArrayList<>(loadedRegions.values());
        regions.sort(Comparator.comparingLong(WorldMapRegion::getLastAccessOrder));

        for (WorldMapRegion region : regions) {
            if (loadedRegions.size() <= maxRegions) return;
            if (region.getRegionX() == protectedRegionX && region.getRegionZ() == protectedRegionZ)
                continue;

            String regionKey = WorldMapRegionKey.of(region.getRegionX(), region.getRegionZ());
            if (region.isDirty() || regionSaveQueue.isPending(regionKey)) {
                regionSaveQueue.saveRegion(region, MAX_PENDING_REGION_SAVES);
                continue;
            }

            loadedRegions.remove(regionKey);
            regionSaveQueue.forget(regionKey);
            region.close();
        }
    }

    public boolean isChunkExplored(ChunkPos chunk) {
        int regionX = WorldMapRegion.chunkToRegionCoord(chunk.x);
        int regionZ = WorldMapRegion.chunkToRegionCoord(chunk.z);
        WorldMapRegion region = getLoadedRegion(regionX, regionZ);
        if (region == null) region = getOrSchedulePersistedRegion(regionX, regionZ, false, 0L, 0);
        if (region == null) return false;

        int localX =
                WorldMapRegion.chunkLocalCoord(chunk.x) * WorldMapRegion.PIXELS_PER_CHUNK
                        + WorldMapRegion.PIXELS_PER_CHUNK / 2;
        int localZ =
                WorldMapRegion.chunkLocalCoord(chunk.z) * WorldMapRegion.PIXELS_PER_CHUNK
                        + WorldMapRegion.PIXELS_PER_CHUNK / 2;

        return region.hasVisiblePixel(localX, localZ);
    }

    public void onChunkLoaded(Level level, ChunkPos chunkPos) {
        if (!isCurrentClientLevel(level) || chunkPos == null) return;
        if (!shouldUpdateAroundPlayer()) return;

        markChunkDirty(chunkPos.x, chunkPos.z, false, CHUNK_LOAD_SETTLE_NANOS, true);
        // New neighbors can improve the one-pixel border on nearby chunks.
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) continue;

                int neighborChunkX = chunkPos.x + dx;
                int neighborChunkZ = chunkPos.z + dz;
                if (hasFreshChunkPixels(neighborChunkX, neighborChunkZ)) {
                    markChunkDirty(neighborChunkX, neighborChunkZ, false, CHUNK_LOAD_SETTLE_NANOS, true);
                }
            }
        }
    }

    public void onChunkUnloaded(Level level, ChunkPos chunkPos) {
        if (!isCurrentClientLevel(level) || chunkPos == null) return;

        long chunkKey = chunkKey(chunkPos.x, chunkPos.z);
        forgetChunk(chunkKey);
    }

    public void onBlockUpdated(Level level, BlockPos pos) {
        if (!isCurrentClientLevel(level) || pos == null) return;
        if (!shouldUpdateAroundPlayer()) return;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        markChunkDirty(chunkX, chunkZ, true, BLOCK_UPDATE_SETTLE_NANOS);

        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        if (localX == 0) markChunkDirty(chunkX - 1, chunkZ, true, BLOCK_UPDATE_SETTLE_NANOS);
        if (localX == 15) markChunkDirty(chunkX + 1, chunkZ, true, BLOCK_UPDATE_SETTLE_NANOS);
        if (localZ == 0) markChunkDirty(chunkX, chunkZ - 1, true, BLOCK_UPDATE_SETTLE_NANOS);
        if (localZ == 15) markChunkDirty(chunkX, chunkZ + 1, true, BLOCK_UPDATE_SETTLE_NANOS);
        if (localX == 15 && localZ == 15) {
            markChunkDirty(chunkX + 1, chunkZ + 1, true, BLOCK_UPDATE_SETTLE_NANOS);
        }
    }

    public void close() {
        regionSaveQueue.saveAllDirtyRegionsAndWait(
                loadedRegions,
                MAX_PENDING_REGION_SAVES,
                REGION_SAVE_QUIET_PERIOD_NANOS,
                MAX_LOADED_REGIONS + MAX_PENDING_REGION_SAVES);
        closePendingRegionLoads();
        closePendingSourceRecolors();
        renderTileCache.close();
        WorldMapRenderer.releaseSharedResources();

        for (WorldMapRegion region : loadedRegions.values()) {
            region.close();
        }
        loadedRegions.clear();
        regionSaveQueue.clear();
        regionRepository.close();
        failedRegionLoads.clear();
        chunkBuildQueue.clear();
        discoveredChunks.clear();
        clearSourceRecolorPipeline();
        worldMapDir = null;
        lastDiscoveryTime = 0L;
        lastSaveTime = 0L;
        savePassPending = false;
        lastEnqueueCenterChunkKey = Long.MIN_VALUE;
        chunkScanOffsetCursor = 0;
        regionAccessSequence = 0L;
        regionLoadSchedulesLeft = 0;
        previousPlayerAreaUpdatesEnabled = true;
        sourceRecolorTotalRegions = 0;
        sourceRecolorCompletedRegions = 0;
    }

    private WorldMapRegion getOrCreateRegion(int regionX, int regionZ) {
        String regionKey = WorldMapRegionKey.of(regionX, regionZ);
        WorldMapRegion region = loadedRegions.get(regionKey);
        if (region == null) {
            PendingRegionLoad pendingLoad = pendingRegionLoads.get(regionKey);
            if (pendingLoad != null) {
                return null;
            }

            if (regionRepository.hasSource(regionKey)) {
                if (!isRegionLoadRetryBlocked(regionKey)) {
                    scheduleRegionLoad(regionKey, regionX, regionZ, false, false, 0L, 0);
                }
                return null;
            }

            region = new WorldMapRegion(regionX, regionZ);
            region.createBlank(mapColorEpoch);
            loadedRegions.put(regionKey, region);
        }
        return touchRegion(region);
    }

    private void discoverLoadedChunksAroundPlayer() {
        if (mc.level == null || mc.player == null) return;

        int centerChunkX = mc.player.chunkPosition().x;
        int centerChunkZ = mc.player.chunkPosition().z;
        int radius = getUpdateRadiusChunks();
        long centerChunkKey = chunkKey(centerChunkX, centerChunkZ);

        if (centerChunkKey != lastEnqueueCenterChunkKey) {
            lastEnqueueCenterChunkKey = centerChunkKey;
            chunkScanOffsetCursor = 0;
            discardQueuedChunksOutside(centerChunkX, centerChunkZ, radius + 2);
            prioritizeQueuedChunksAround(centerChunkX, centerChunkZ);
        }

        discoverChunk(centerChunkX, centerChunkZ);

        long deadlineNanos = System.nanoTime() + CHUNK_ENQUEUE_BUDGET_NANOS;
        int checkedChunks = 0;
        int radiusSquared = radius * radius;
        int scanLimit = chunkScanOffsetLimit(radiusSquared);
        if (chunkScanOffsetCursor >= scanLimit) chunkScanOffsetCursor = 0;

        int visitedOffsets = 0;
        while (visitedOffsets < scanLimit
                && checkedChunks < MAX_CHUNK_ENQUEUE_CHECKS_PER_REFRESH
                && chunkBuildQueue.queuedChunkCount() < MAX_QUEUED_CHUNK_UPDATES
                && System.nanoTime() < deadlineNanos) {
            ChunkScanOffset offset = CHUNK_SCAN_OFFSETS[chunkScanOffsetCursor];
            chunkScanOffsetCursor++;
            if (chunkScanOffsetCursor >= scanLimit) chunkScanOffsetCursor = 0;
            visitedOffsets++;

            int chunkX = centerChunkX + offset.dx();
            int chunkZ = centerChunkZ + offset.dz();
            checkedChunks++;

            discoverChunk(chunkX, chunkZ);
        }
        prioritizeQueuedChunksAround(centerChunkX, centerChunkZ);
    }

    private int enqueueNearbyLoadedChunksForColorRefresh() {
        if (mc.level == null || mc.player == null) return 0;

        int centerChunkX = mc.player.chunkPosition().x;
        int centerChunkZ = mc.player.chunkPosition().z;
        int radius = getUpdateRadiusChunks();
        int enqueued = 0;

        if (markChunkDirty(centerChunkX, centerChunkZ, false, 0L, true)) {
            enqueued++;
        }

        for (ChunkScanOffset offset : CHUNK_SCAN_OFFSETS) {
            if (offset.distanceSquared() > radius * radius
                    || chunkBuildQueue.queuedChunkCount() >= MAX_QUEUED_CHUNK_UPDATES) {
                break;
            }
            if (markChunkDirty(centerChunkX + offset.dx(), centerChunkZ + offset.dz(), false, 0L, true)) {
                enqueued++;
            }
        }
        prioritizeQueuedChunksAround(centerChunkX, centerChunkZ);
        return enqueued;
    }

    private void discardQueuedChunksOutside(int centerChunkX, int centerChunkZ, int keepRadius) {
        chunkBuildQueue.discardOutside(
                centerChunkX, centerChunkZ, keepRadius, discoveredChunks::remove);
    }

    private void prioritizeQueuedChunksAround(int centerChunkX, int centerChunkZ) {
        chunkBuildQueue.prioritizeAround(centerChunkX, centerChunkZ);
    }

    private void discoverChunk(int chunkX, int chunkZ) {
        long chunkKey = chunkKey(chunkX, chunkZ);
        if (!isChunkLoaded(chunkX, chunkZ)) return;
        if (discoveredChunks.containsKey(chunkKey)) {
            if (hasFreshChunkPixels(chunkX, chunkZ) || chunkBuildQueue.hasWork(chunkKey)) return;
            discoveredChunks.remove(chunkKey);
        }

        markChunkDirty(chunkX, chunkZ, false, CHUNK_LOAD_SETTLE_NANOS);
    }

    private boolean markChunkDirty(int chunkX, int chunkZ, boolean urgent, long settleNanos) {
        return markChunkDirty(chunkX, chunkZ, urgent, settleNanos, false);
    }

    private boolean markChunkDirty(
            int chunkX, int chunkZ, boolean urgent, long settleNanos, boolean forceRebuild) {
        if (!isChunkLoaded(chunkX, chunkZ)) return false;

        long chunkKey = chunkKey(chunkX, chunkZ);
        if (!forceRebuild
                && !urgent
                && completeChunkFromLoadedRegionIfFresh(chunkKey, chunkX, chunkZ)) {
            return true;
        }

        long readyNanos = System.nanoTime() + settleNanos;
        // Chunk load/block events can arrive before the client chunk is stable.
        if (forceRebuild && !urgent && chunkBuildQueue.canMergeForcedReadyTime(chunkKey)) {
            chunkBuildQueue.mergeReadyNanos(chunkKey, readyNanos);
            discoveredChunks.put(chunkKey, Boolean.TRUE);
            return true;
        }

        chunkBuildQueue.startRevision(chunkKey, readyNanos, forceRebuild);
        if (enqueueChunkForBuild(chunkKey, urgent)) {
            discoveredChunks.put(chunkKey, Boolean.TRUE);
            return true;
        } else {
            chunkBuildQueue.removeBuildMetadata(chunkKey);
            discoveredChunks.remove(chunkKey);
            return false;
        }
    }

    private boolean enqueueChunkForBuild(long chunkKey, boolean urgent) {
        return chunkBuildQueue.enqueueForBuild(chunkKey, urgent, MAX_QUEUED_CHUNK_UPDATES);
    }

    private boolean completeChunkFromLoadedRegionIfFresh(long chunkKey, int chunkX, int chunkZ) {
        int regionX = WorldMapRegion.chunkToRegionCoord(chunkX);
        int regionZ = WorldMapRegion.chunkToRegionCoord(chunkZ);
        return completeChunkFromRegionIfFresh(
                chunkKey, chunkX, chunkZ, getLoadedRegion(regionX, regionZ));
    }

    private boolean hasFreshChunkPixels(int chunkX, int chunkZ) {
        int regionX = WorldMapRegion.chunkToRegionCoord(chunkX);
        int regionZ = WorldMapRegion.chunkToRegionCoord(chunkZ);
        WorldMapRegion region = loadedRegions.get(WorldMapRegionKey.of(regionX, regionZ));
        return region != null
                && region.hasChunkPixelsForColorEpoch(
                        WorldMapRegion.chunkLocalCoord(chunkX),
                        WorldMapRegion.chunkLocalCoord(chunkZ),
                        mapColorEpoch);
    }

    private boolean completeChunkFromRegionIfFresh(
            long chunkKey, int chunkX, int chunkZ, WorldMapRegion region) {
        if (region == null || hasRequiredChunkBuild(chunkKey)) return false;

        int chunkXInRegion = WorldMapRegion.chunkLocalCoord(chunkX);
        int chunkZInRegion = WorldMapRegion.chunkLocalCoord(chunkZ);
        if (!region.hasChunkPixelsForColorEpoch(chunkXInRegion, chunkZInRegion, mapColorEpoch))
            return false;

        cancelChunkWork(chunkKey);
        discoveredChunks.put(chunkKey, Boolean.TRUE);
        return true;
    }

    private boolean hasRequiredChunkBuild(long chunkKey) {
        return chunkBuildQueue.hasRequiredBuild(chunkKey);
    }

    private void cancelChunkWork(long chunkKey) {
        chunkBuildQueue.cancelWork(chunkKey);
    }

    private static ChunkScanOffset[] buildChunkScanOffsets() {
        ArrayList<ChunkScanOffset> offsets = new ArrayList<>();
        int maxDistanceSquared = MAX_UPDATE_RADIUS_CHUNKS * MAX_UPDATE_RADIUS_CHUNKS;
        for (int dz = -MAX_UPDATE_RADIUS_CHUNKS; dz <= MAX_UPDATE_RADIUS_CHUNKS; dz++) {
            for (int dx = -MAX_UPDATE_RADIUS_CHUNKS; dx <= MAX_UPDATE_RADIUS_CHUNKS; dx++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared == 0 || distanceSquared > maxDistanceSquared) continue;
                offsets.add(new ChunkScanOffset(dx, dz, distanceSquared, scanAngleOrder(dx, dz)));
            }
        }
        offsets.sort(
                Comparator.comparingInt(ChunkScanOffset::distanceSquared)
                        .thenComparingInt(ChunkScanOffset::angleOrder));
        return offsets.toArray(ChunkScanOffset[]::new);
    }

    private static int scanAngleOrder(int dx, int dz) {
        return (int) Math.round((Math.atan2(dz, dx) + Math.PI) * 1024.0);
    }

    private static int chunkScanOffsetLimit(int radiusSquared) {
        int limit = 0;
        while (limit < CHUNK_SCAN_OFFSETS.length
                && CHUNK_SCAN_OFFSETS[limit].distanceSquared() <= radiusSquared) {
            limit++;
        }
        return limit;
    }

    private void scheduleChunkBuilds(int maxSchedules) {
        if (mc.level == null || mc.player == null) return;

        // Start only a few async builds per tick; the rest stays queued.
        int scheduled = 0;
        int attempts = 0;
        int maxAttempts =
                Math.min(MAX_CHUNK_BUILD_SCHEDULE_ATTEMPTS_PER_TICK, chunkBuildQueue.queuedChunkCount());
        long nowNanos = System.nanoTime();
        long deadlineNanos = nowNanos + CHUNK_BUILD_SCHEDULE_BUDGET_NANOS;
        int centerChunkX = mc.player.chunkPosition().x;
        int centerChunkZ = mc.player.chunkPosition().z;
        int cachedCompletions = 0;
        prioritizeQueuedChunksAround(centerChunkX, centerChunkZ);
        while (scheduled < maxSchedules
                && chunkBuildQueue.pendingBuildCount() < MAX_PENDING_CHUNK_BUILDS
                && chunkBuildQueue.hasQueuedChunks()
                && attempts < maxAttempts
                && System.nanoTime() < deadlineNanos) {
            WorldMapChunkBuildQueue.BuildCandidate candidate =
                    chunkBuildQueue.pollNextForBuild(
                            nowNanos, maxAttempts - attempts, deadlineNanos);
            if (candidate == null) return;
            attempts += candidate.checkedChunks();
            if (!candidate.hasChunk()) break;

            long chunkKey = candidate.chunkKey();
            boolean urgent = candidate.urgent();

            Long revision = chunkBuildQueue.revision(chunkKey);
            if (revision == null) continue;

            long readyNanos = chunkBuildQueue.readyNanosOrZero(chunkKey);
            if (nowNanos < readyNanos) {
                enqueueDelayedChunk(chunkKey, urgent);
                continue;
            }

            int chunkX = chunkX(chunkKey);
            int chunkZ = chunkZ(chunkKey);
            boolean forcedRebuild = chunkBuildQueue.isForcedRebuild(chunkKey);
            ChunkSamplingContext context = ChunkSamplingContext.capture(mc.level, chunkX, chunkZ);
            if (context == null) {
                if (isChunkLoaded(chunkX, chunkZ)) {
                    chunkBuildQueue.removeBuildMetadata(chunkKey);
                    discoveredChunks.remove(chunkKey);
                } else {
                    forgetChunk(chunkKey);
                }
                continue;
            }

            int regionX = WorldMapRegion.chunkToRegionCoord(chunkX);
            int regionZ = WorldMapRegion.chunkToRegionCoord(chunkZ);
            String regionKey = WorldMapRegionKey.of(regionX, regionZ);
            WorldMapRegion region = getOrCreateRegion(regionX, regionZ);
            if (region == null) {
                parkChunkForRegion(regionKey, chunkKey, urgent);
                continue;
            }
            if (!urgent
                    && !forcedRebuild
                    && completeChunkFromRegionIfFresh(chunkKey, chunkX, chunkZ, region)) {
                cachedCompletions++;
                if (cachedCompletions >= MAX_CACHED_CHUNK_COMPLETIONS_PER_TICK) {
                    break;
                }
                continue;
            }

            ChunkImageBuilder builder = ChunkImageBuilder.begin(context, chunkX, chunkZ, chunkKey);
            CompletableFuture<ChunkBuildResult> future =
                    WorldMapAsync.buildChunk(
                            urgent,
                            distanceSquaredToChunk(chunkKey, centerChunkX, centerChunkZ),
                            builder::buildFully);
            chunkBuildQueue.putPendingBuild(
                    chunkKey,
                    new PendingChunkBuild(context, revision, builder, future, urgent, forcedRebuild));
            scheduled++;
        }
    }

    private int consumeReadyChunkBuilds(int maxCompletions) {
        int completed = 0;
        Iterator<Map.Entry<Long, PendingChunkBuild>> iterator = chunkBuildQueue.pendingBuildIterator();
        while (iterator.hasNext() && completed < maxCompletions) {
            Map.Entry<Long, PendingChunkBuild> entry = iterator.next();
            PendingChunkBuild pendingBuild = entry.getValue();
            if (!pendingBuild.future().isDone()) continue;

            iterator.remove();
            completed++;
            ChunkBuildResult result;
            try {
                result = pendingBuild.future().join();
            } catch (CancellationException ignored) {
                pendingBuild.builder().cancel();
                result = null;
            } catch (Exception ignored) {
                pendingBuild.builder().cancel();
                result = null;
            }

            long chunkKey = entry.getKey();
            boolean urgentRetry = consumePendingUrgency(chunkKey, pendingBuild);
            Long currentRevision = chunkBuildQueue.revision(chunkKey);
            boolean resultIsCurrent =
                    currentRevision != null
                            && currentRevision == pendingBuild.revision()
                            && pendingBuild.context().belongsTo(mc.level)
                            && isChunkLoaded(chunkX(chunkKey), chunkZ(chunkKey));
            if (resultIsCurrent && result != null) {
                if (result.sourceChunk() == null) {
                    chunkBuildQueue.mergeReadyNanos(
                            chunkKey, System.nanoTime() + INCOMPLETE_CHUNK_RETRY_NANOS);
                    enqueueDeferredChunk(chunkKey, urgentRetry);
                    continue;
                }
                if (finishChunkBuild(result)) {
                    chunkBuildQueue.completeRevision(chunkKey, currentRevision);
                } else {
                    int regionX = WorldMapRegion.chunkToRegionCoord(chunkX(chunkKey));
                    int regionZ = WorldMapRegion.chunkToRegionCoord(chunkZ(chunkKey));
                    parkChunkForRegion(WorldMapRegionKey.of(regionX, regionZ), chunkKey, urgentRetry);
                }
            } else if (pendingBuild.context().belongsTo(mc.level)
                    && isChunkLoaded(chunkX(chunkKey), chunkZ(chunkKey))) {
                if (currentRevision != null) {
                    chunkBuildQueue.mergeReadyNanos(
                            chunkKey, System.nanoTime() + INCOMPLETE_CHUNK_RETRY_NANOS);
                }
                enqueueDeferredChunk(chunkKey, urgentRetry);
            }
        }
        return completed;
    }

    private boolean finishChunkBuild(ChunkBuildResult result) {
        int chunkX = result.chunkX();
        int chunkZ = result.chunkZ();
        int regionX = WorldMapRegion.chunkToRegionCoord(chunkX);
        int regionZ = WorldMapRegion.chunkToRegionCoord(chunkZ);
        WorldMapRegion region = getOrCreateRegion(regionX, regionZ);
        if (region == null) {
            return false;
        }

        region.updateFromChunkPixelsAndSource(
                result.pixels(),
                result.sourceChunk(),
                WorldMapRegion.chunkLocalCoord(chunkX),
                WorldMapRegion.chunkLocalCoord(chunkZ),
                mapColorEpoch);
        renderTileCache.invalidateChunk(
                region,
                WorldMapRegion.chunkLocalCoord(chunkX),
                WorldMapRegion.chunkLocalCoord(chunkZ),
                result.pixels());
        regionSaveQueue.markDirty(WorldMapRegionKey.of(regionX, regionZ));
        return true;
    }

    private void scheduleSourceRecolors(int maxSchedules) {
        if (mc.level == null || !regionRepository.isReady()) return;

        ClientLevel level = mc.level;
        int scheduled = 0;
        while (scheduled < maxSchedules
                && pendingSourceRecolors.size() < MAX_PENDING_SOURCE_RECOLORS
                && !sourceRecolorQueue.isEmpty()) {
            String regionKey = sourceRecolorQueue.removeFirst();
            queuedSourceRecolors.remove(regionKey);
            RegionCoords coords = parseRegionKey(regionKey);
            if (coords == null) {
                sourceRecolorCompletedRegions++;
                continue;
            }

            WorldMapRegion loadedRegion = loadedRegions.get(regionKey);
            WorldMapSourceRegion loadedSource =
                    loadedRegion != null && loadedRegion.hasSourceData()
                            ? loadedRegion.sourceData()
                            : null;
            // Loaded region: reuse memory. Unloaded region: worker reads the .rmap.
            CompletableFuture<SourceRecolorResult> future =
                    WorldMapAsync.recolorSourceRegion(
                            () -> recolorSourceRegion(level, coords, loadedSource));
            pendingSourceRecolors.put(regionKey, new PendingSourceRecolor(coords, future));
            scheduled++;
        }
    }

    private SourceRecolorResult recolorSourceRegion(
            ClientLevel level, RegionCoords coords, WorldMapSourceRegion loadedSource) {
        WorldMapSourceRegion source =
                loadedSource != null && loadedSource.hasAnySource()
                        ? loadedSource
                        : regionRepository.readSourceIfPresent(coords.regionX(), coords.regionZ());
        if (source == null) return null;

        WorldMapRegionPixels pixels = source.rebuildPixels(level, coords.regionX(), coords.regionZ());
        if (pixels == null) return null;

        return new SourceRecolorResult(coords.regionX(), coords.regionZ(), pixels, source);
    }

    private void consumeReadySourceRecolors(int maxCompletions) {
        int remaining = maxCompletions;
        Iterator<Map.Entry<String, PendingSourceRecolor>> iterator =
                pendingSourceRecolors.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            Map.Entry<String, PendingSourceRecolor> entry = iterator.next();
            PendingSourceRecolor pendingRecolor = entry.getValue();
            if (!pendingRecolor.future().isDone()) continue;

            iterator.remove();
            remaining--;
            sourceRecolorCompletedRegions++;

            SourceRecolorResult result;
            try {
                result = pendingRecolor.future().join();
            } catch (CancellationException ignored) {
                result = null;
            } catch (Exception ignored) {
                result = null;
            }
            if (result != null) {
                applySourceRecolor(entry.getKey(), result);
            }
        }

        if (sourceRecolorTotalRegions > 0
                && sourceRecolorQueue.isEmpty()
                && pendingSourceRecolors.isEmpty()) {
            Main.LOGGER.info(
                    "[WorldMap] Resource-pack color refresh finished for {} source regions",
                    sourceRecolorCompletedRegions);
            sourceRecolorTotalRegions = 0;
            sourceRecolorCompletedRegions = 0;
        }
    }

    private void applySourceRecolor(String regionKey, SourceRecolorResult result) {
        WorldMapRegion region = loadedRegions.get(regionKey);
        if (region == null) {
            region = new WorldMapRegion(result.regionX(), result.regionZ());
            region.loadFromPixelsAndSource(result.pixels(), result.source(), mapColorEpoch);
            loadedRegions.put(regionKey, region);
        } else {
            region.updateSourceBackedChunks(result.pixels(), result.source(), mapColorEpoch);
        }

        touchRegion(region);
        renderTileCache.invalidateRegion(region);
        regionRepository.recordSource(regionKey);
        regionRepository.recordSourceData(regionKey);
    }

    private void enqueueDeferredChunk(long chunkKey, boolean urgent) {
        chunkBuildQueue.enqueueDeferred(chunkKey, urgent);
    }

    private void enqueueDelayedChunk(long chunkKey, boolean urgent) {
        chunkBuildQueue.enqueueDelayed(chunkKey, urgent);
    }

    private void parkChunkForRegion(String regionKey, long chunkKey, boolean urgent) {
        chunkBuildQueue.parkForRegion(regionKey, chunkKey, urgent);
    }

    private void queueRegionWake(String regionKey) {
        chunkBuildQueue.queueRegionWake(regionKey);
    }

    private void processRegionWakeQueue(int maxChunks, long budgetNanos) {
        long deadlineNanos = System.nanoTime() + budgetNanos;
        int processed = 0;
        while (processed < maxChunks
                && chunkBuildQueue.hasRegionWake()
                && System.nanoTime() < deadlineNanos) {
            String regionKey = chunkBuildQueue.peekRegionWake();
            Long waitingChunkKey = chunkBuildQueue.takeWaitingChunkForRegion(regionKey);
            if (waitingChunkKey == null) continue;

            long chunkKey = waitingChunkKey;
            if (!chunkBuildQueue.hasRevision(chunkKey)
                    || !isChunkLoaded(chunkX(chunkKey), chunkZ(chunkKey))) {
                forgetChunk(chunkKey);
            } else {
                boolean urgent = chunkBuildQueue.consumeUrgency(chunkKey);
                boolean forcedRebuild = chunkBuildQueue.isForcedRebuild(chunkKey);
                WorldMapRegion region = loadedRegions.get(regionKey);
                if (!urgent
                        && !forcedRebuild
                        && completeChunkFromRegionIfFresh(
                                chunkKey, chunkX(chunkKey), chunkZ(chunkKey), region)) {
                    processed++;
                    continue;
                }
                if (!enqueueChunkForBuild(chunkKey, urgent)) {
                    discoveredChunks.remove(chunkKey);
                }
            }
            processed++;
        }
    }

    private void scheduleWaitingRegionLoads(int maxRegions) {
        if (pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS
                || !chunkBuildQueue.hasWaitingRegions()) return;

        int scheduled = 0;
        for (String regionKey : chunkBuildQueue.waitingRegionKeysSnapshot()) {
            if (scheduled >= maxRegions || pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS) return;

            if (pendingRegionLoads.containsKey(regionKey)) continue;
            if (loadedRegions.containsKey(regionKey)) {
                queueRegionWake(regionKey);
                continue;
            }

            Long waitingChunkKey = chunkBuildQueue.peekWaitingChunk(regionKey);
            if (waitingChunkKey == null) {
                chunkBuildQueue.removeWaitingRegionIfEmpty(regionKey);
                continue;
            }

            long chunkKey = waitingChunkKey;
            int regionX = WorldMapRegion.chunkToRegionCoord(chunkX(chunkKey));
            int regionZ = WorldMapRegion.chunkToRegionCoord(chunkZ(chunkKey));
            if (scheduleRegionLoad(regionKey, regionX, regionZ, false, false, 0L, 0)) {
                scheduled++;
            }
        }
    }

    private void forgetChunk(long chunkKey) {
        chunkBuildQueue.forget(chunkKey);
        discoveredChunks.remove(chunkKey);
    }

    private boolean scheduleRegionLoad(
            String regionKey,
            int regionX,
            int regionZ,
            boolean useFrameBudget,
            boolean viewOnly,
            long generation,
            int distanceSquared) {
        if (isRegionLoadRetryBlocked(regionKey)) return false;
        if (pendingRegionLoads.containsKey(regionKey)) return false;
        if (pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS) return false;
        if (useFrameBudget) {
            if (regionLoadSchedulesLeft <= 0) return false;
            regionLoadSchedulesLeft--;
        }

        ClientLevel level = mc.level;
        if (level == null) return false;

        CompletableFuture<WorldMapRegionRepository.WorldMapRegionLoadResult> future =
                WorldMapAsync.loadRegion(
                        !viewOnly,
                        generation,
                        distanceSquared,
                        () -> regionRepository.readRegionIfPresent(level, regionX, regionZ));
        pendingRegionLoads.put(regionKey, new PendingRegionLoad(regionX, regionZ, future));
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

    private boolean isCurrentClientLevel(Level level) {
        return level != null && level.isClientSide && level == mc.level;
    }

    private boolean isChunkLoaded(int chunkX, int chunkZ) {
        if (mc.level == null) return false;
        try {
            LevelChunk chunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
            return chunk != null && !(chunk instanceof EmptyLevelChunk);
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

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private static int chunkX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    private static int chunkZ(long chunkKey) {
        return (int) chunkKey;
    }

    private static int distanceSquaredToChunk(long chunkKey, int centerChunkX, int centerChunkZ) {
        int dx = chunkX(chunkKey) - centerChunkX;
        int dz = chunkZ(chunkKey) - centerChunkZ;
        return dx * dx + dz * dz;
    }

    private boolean consumePendingUrgency(long chunkKey, PendingChunkBuild pendingBuild) {
        return chunkBuildQueue.consumePendingUrgency(chunkKey, pendingBuild);
    }

    private WorldMapRegion touchRegion(WorldMapRegion region) {
        region.markAccessed(++regionAccessSequence);
        return region;
    }

    private void closePendingRegionLoads() {
        for (PendingRegionLoad pendingLoad : pendingRegionLoads.values()) {
            pendingLoad.future().cancel(false);
        }
        pendingRegionLoads.clear();
    }

    private void closePendingSourceRecolors() {
        for (PendingSourceRecolor pendingRecolor : pendingSourceRecolors.values()) {
            pendingRecolor.future().cancel(false);
        }
        pendingSourceRecolors.clear();
    }

    private boolean isUsingUnknownStorageId() {
        return worldMapDir != null && "unknown".equals(worldMapDir.getName());
    }

    private static boolean shouldUpdateAroundPlayer() {
        return RecruitsClientConfig.UpdateMapTiles.get()
                && RecruitsClientConfig.WorldMapUpdateAroundPlayer.get();
    }

    private static RegionCoords parseRegionKey(String regionKey) {
        int separator = regionKey == null ? -1 : regionKey.indexOf('_');
        if (separator <= 0 || separator >= regionKey.length() - 1) return null;

        try {
            return new RegionCoords(
                    Integer.parseInt(regionKey.substring(0, separator)),
                    Integer.parseInt(regionKey.substring(separator + 1)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record PendingRegionLoad(
            int regionX,
            int regionZ,
            CompletableFuture<WorldMapRegionRepository.WorldMapRegionLoadResult> future) {}

    private record PendingSourceRecolor(
            RegionCoords coords, CompletableFuture<SourceRecolorResult> future) {}

    private record SourceRecolorResult(
            int regionX,
            int regionZ,
            WorldMapRegionPixels pixels,
            WorldMapSourceRegion source) {}

    private record RegionCoords(int regionX, int regionZ) {}

    private record ChunkScanOffset(int dx, int dz, int distanceSquared, int angleOrder) {}
}
