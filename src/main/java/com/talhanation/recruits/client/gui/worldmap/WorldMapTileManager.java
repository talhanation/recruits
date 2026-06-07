package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;

public class WorldMapTileManager {
    private static final int MAX_CHUNK_BUILD_SCHEDULES_PER_TICK = 4;
    private static final int MAX_CHUNK_BUILD_SCHEDULE_ATTEMPTS_PER_TICK = 64;
    private static final int MAX_CHUNK_BUILD_COMPLETIONS_PER_TICK = 16;
    private static final int MAX_PENDING_CHUNK_BUILDS = 8;
    private static final int MAX_CACHED_CHUNK_COMPLETIONS_PER_TICK = 32;
    private static final int MAX_TRACKED_CHUNKS = 8192;
    private static final int MAX_LOADED_REGIONS = 192;
    private static final int MAX_REGION_LOAD_SCHEDULES_PER_FRAME = 4;
    private static final int MAX_REGION_LOAD_COMPLETIONS_PER_FRAME = 8;
    private static final int MAX_PENDING_REGION_LOADS = 24;
    private static final int MAX_REGION_WAKE_CHUNKS_PER_TICK = 32;
    private static final int MAX_REGION_SAVE_COMPLETIONS_PER_FRAME = 8;
    private static final int MAX_PENDING_REGION_SAVES = 8;
    private static final int MIN_UPDATE_RADIUS_CHUNKS = 5;
    private static final int MAX_UPDATE_RADIUS_CHUNKS = 12;
    private static final int MAX_REGION_SAVES_PER_PASS = 1;
    private static final int MAX_QUEUED_CHUNK_UPDATES = 512;
    private static final int MAX_CHUNK_ENQUEUE_CHECKS_PER_REFRESH = 48;
    private static final long CHUNK_ENQUEUE_BUDGET_NANOS = 250_000L;
    private static final long CHUNK_BUILD_SCHEDULE_BUDGET_NANOS = 500_000L;
    private static final long REGION_WAKE_BUDGET_NANOS = 500_000L;
    private static final long CHUNK_LOAD_SETTLE_NANOS = 150_000_000L;
    private static final long BLOCK_UPDATE_SETTLE_NANOS = 75_000_000L;
    private static final long REGION_LOAD_RETRY_DELAY_MS = 1000L;
    private static final long DISCOVERY_INTERVAL_MS = 250L;
    private static final long SAVE_INTERVAL_MS = 60000L;
    private static final long REGION_SAVE_QUIET_PERIOD_NANOS = 3_000_000_000L;
    private static final ChunkScanOffset[] CHUNK_SCAN_OFFSETS = buildChunkScanOffsets();

    private static WorldMapTileManager instance;

    private final Minecraft mc = Minecraft.getInstance();
    private final WorldMapRenderTileCache renderTileCache = new WorldMapRenderTileCache(this);
    private final Map<String, WorldMapRegionTile> loadedRegions = new HashMap<>();
    private final Map<String, CachedRegionLoad> pendingRegionLoads = new HashMap<>();
    private final Map<String, PendingRegionSave> pendingRegionSaves = new HashMap<>();
    private final Map<String, Long> failedRegionLoads = new HashMap<>();
    private final Set<String> persistedRegionSources = new HashSet<>();
    private final Set<String> dirtyRegionKeys = new LinkedHashSet<>();
    private final Deque<Long> chunkUpdateQueue = new ArrayDeque<>();
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Set<Long> urgentChunks = new HashSet<>();
    private final Set<Long> forcedRebuildChunks = new HashSet<>();
    private final Map<Long, Long> chunkRevisions = new HashMap<>();
    private final Map<Long, Long> chunkReadyNanos = new HashMap<>();
    private final Map<Long, String> chunkWaitingRegions = new HashMap<>();
    private final Map<String, Set<Long>> chunksWaitingByRegion = new HashMap<>();
    private final Deque<String> regionWakeQueue = new ArrayDeque<>();
    private final Set<String> queuedRegionWakes = new HashSet<>();
    private final Map<Long, PendingChunkBuild> pendingChunkBuilds = new LinkedHashMap<>();
    private final Map<Long, Boolean> discoveredChunks = new LinkedHashMap<>(1024, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
            return size() > MAX_TRACKED_CHUNKS;
        }
    };

    private File worldMapDir;
    private File regionDir;
    private File legacyLevelZeroDir;
    private long lastDiscoveryTime;
    private long lastSaveTime;
    private boolean savePassPending;
    private long lastEnqueueCenterChunkKey = Long.MIN_VALUE;
    private long nextChunkRevision;
    private long regionAccessSequence;
    private int enqueueOffsetCursor;
    private int regionLoadSchedulesLeft;
    private int mapContentEpoch = 1;
    private boolean cachedRegionsStaleForResourceReload;
    private boolean sawInitialModelBake;
    private boolean rebuildLoadedChunksOnNextInitialize;

    public static WorldMapTileManager getInstance() {
        if (instance == null) instance = new WorldMapTileManager();
        return instance;
    }

    public void initialize(Level level) {
        if (level == null) return;

        String worldName = detectStorageId(level);
        File newWorldMapDir = new File(mc.gameDirectory, "recruits/worldmap/" + worldName);
        if (this.worldMapDir != null && this.worldMapDir.equals(newWorldMapDir)) {
            if (rebuildLoadedChunksOnNextInitialize) {
                rebuildLoadedChunksOnNextInitialize = false;
                rebuildLoadedChunksForResourceReload();
            }
            return;
        }

        close();
        this.worldMapDir = newWorldMapDir;
        this.regionDir = new File(newWorldMapDir, "regions");
        this.legacyLevelZeroDir = new File(this.regionDir, "l0");
        this.regionDir.mkdirs();
        rebuildRegionSourceIndex();
        WorldMapAsync.prepareWorkers();
        ChunkBuildScratch.preparePool();
        WorldMapRegionTile.prepareRenderPixelPool();
        WorldMapRenderer.prepareSharedResources();
        renderTileCache.prepareGpuResources();
        if (rebuildLoadedChunksOnNextInitialize) {
            rebuildLoadedChunksOnNextInitialize = false;
            rebuildLoadedChunksForResourceReload();
        }
        Main.LOGGER.info("[WorldMap] Initialized direct region cache at {} with {} indexed regions",
                this.regionDir, this.persistedRegionSources.size());
    }

    public void onClientModelsReloaded() {
        boolean resourceReload = sawInitialModelBake;
        sawInitialModelBake = true;

        WorldMapAsync.prepareWorkers();
        ChunkBuildScratch.preparePool();
        WorldMapRegionTile.prepareRenderPixelPool();
        MapBlockColorResolver.clearCaches();
        MapStateSampler.clearCaches();
        if (!resourceReload) return;

        beginResourceReloadContentEpoch();
        if (mc.level == null || worldMapDir == null || regionDir == null) {
            rebuildLoadedChunksOnNextInitialize = true;
            return;
        }

        rebuildLoadedChunksForResourceReload();
    }

    private void rebuildLoadedChunksForResourceReload() {
        if (mc.level == null || mc.player == null) {
            rebuildLoadedChunksOnNextInitialize = true;
            return;
        }

        closePendingChunkBuilds();
        chunkUpdateQueue.clear();
        queuedChunks.clear();
        urgentChunks.clear();
        forcedRebuildChunks.clear();
        chunkRevisions.clear();
        chunkReadyNanos.clear();
        chunkWaitingRegions.clear();
        chunksWaitingByRegion.clear();
        regionWakeQueue.clear();
        queuedRegionWakes.clear();
        discoveredChunks.clear();
        lastEnqueueCenterChunkKey = Long.MIN_VALUE;
        lastDiscoveryTime = 0L;
        nextChunkRevision = 0L;
        enqueueOffsetCursor = 0;
        regionLoadSchedulesLeft = 0;

        int enqueued = enqueueLoadedChunksForResourceRebuild();
        Main.LOGGER.info("[WorldMap] Queued {} loaded chunks for map recolor after resource reload", enqueued);
    }

    private void beginResourceReloadContentEpoch() {
        mapContentEpoch = mapContentEpoch == Integer.MAX_VALUE ? 1 : mapContentEpoch + 1;
        cachedRegionsStaleForResourceReload = true;
    }

    public void updateCurrentTile() {
        if (mc.level == null || mc.player == null) return;
        if (this.worldMapDir == null || isUsingUnknownStorageId()) initialize(mc.level);

        WorldMapDebugProfiler.beginTileUpdate();
        long debugStartNanos = System.nanoTime();
        long phaseStartNanos = debugStartNanos;
        consumeReadyRegionLoads(MAX_REGION_LOAD_COMPLETIONS_PER_FRAME);
        consumeReadyRegionSaves(MAX_REGION_SAVE_COMPLETIONS_PER_FRAME);
        processRegionWakeQueue(MAX_REGION_WAKE_CHUNKS_PER_TICK, REGION_WAKE_BUDGET_NANOS);
        long consumeLoadsNanos = System.nanoTime() - phaseStartNanos;

        long now = System.currentTimeMillis();
        long enqueueNanos = 0L;
        if (shouldDiscoverLoadedChunks(now)) {
            phaseStartNanos = System.nanoTime();
            discoverLoadedChunksAroundPlayer();
            enqueueNanos = System.nanoTime() - phaseStartNanos;
            lastDiscoveryTime = now;
        }

        phaseStartNanos = System.nanoTime();
        int chunkUpdates = consumeReadyChunkBuilds(MAX_CHUNK_BUILD_COMPLETIONS_PER_TICK);
        long consumeChunksNanos = System.nanoTime() - phaseStartNanos;
        phaseStartNanos = System.nanoTime();
        scheduleChunkBuilds(MAX_CHUNK_BUILD_SCHEDULES_PER_TICK);
        long scheduleChunksNanos = System.nanoTime() - phaseStartNanos;
        long processChunksNanos = consumeChunksNanos + scheduleChunksNanos;

        long saveNanos = 0L;
        if (now - lastSaveTime >= SAVE_INTERVAL_MS) {
            savePassPending = true;
        }
        if (savePassPending && chunkUpdates == 0 && pendingRegionSaves.size() < MAX_PENDING_REGION_SAVES) {
            phaseStartNanos = System.nanoTime();
            int scheduledSaves = saveDirtyRegions(MAX_REGION_SAVES_PER_PASS, false);
            saveNanos = System.nanoTime() - phaseStartNanos;
            if (scheduledSaves == 0 && pendingRegionSaves.isEmpty() && !hasDirtyRegions()) {
                savePassPending = false;
                lastSaveTime = now;
            }
        }

        phaseStartNanos = System.nanoTime();
        trimLoadedRegions(MAX_LOADED_REGIONS);
        long trimNanos = System.nanoTime() - phaseStartNanos;

        recordDebugState();
        int chunkPipelineSize = queuedChunks.size() + pendingChunkBuilds.size() + chunkWaitingRegions.size();
        WorldMapDebugProfiler.recordTileUpdate(System.nanoTime() - debugStartNanos, chunkUpdates,
                chunkUpdateQueue.size(), chunkPipelineSize, consumeLoadsNanos, enqueueNanos,
                processChunksNanos, consumeChunksNanos, scheduleChunksNanos, saveNanos, trimNanos);
    }

    private boolean shouldDiscoverLoadedChunks(long now) {
        if (mc.player == null) return false;

        long centerChunkKey = chunkKey(mc.player.chunkPosition().x, mc.player.chunkPosition().z);
        return centerChunkKey != lastEnqueueCenterChunkKey
                || now - lastDiscoveryTime >= DISCOVERY_INTERVAL_MS;
    }

    WorldMapRegionTile getLoadedRegion(int regionX, int regionZ) {
        WorldMapRegionTile region = loadedRegions.get(key(regionX, regionZ));
        return region == null ? null : touchRegion(region);
    }

    WorldMapRenderTileCache getRenderTileCache() {
        return renderTileCache;
    }

    void beginRenderFrame() {
        this.regionLoadSchedulesLeft = MAX_REGION_LOAD_SCHEDULES_PER_FRAME;
        consumeReadyRegionLoads(MAX_REGION_LOAD_COMPLETIONS_PER_FRAME);
        consumeReadyRegionSaves(MAX_REGION_SAVE_COMPLETIONS_PER_FRAME);
        recordDebugState();
    }

    WorldMapRegionTile getOrScheduleCachedRegion(int regionX, int regionZ, long generation, int distanceSquared) {
        return getOrScheduleCachedRegion(regionX, regionZ, true, generation, distanceSquared);
    }

    private WorldMapRegionTile getOrScheduleCachedRegion(int regionX, int regionZ, boolean useFrameBudget,
                                                         long generation, int distanceSquared) {
        if (regionDir == null) return null;

        WorldMapRegionTile loaded = getLoadedRegion(regionX, regionZ);
        if (loaded != null) return loaded;

        String regionKey = key(regionX, regionZ);
        CachedRegionLoad pendingLoad = pendingRegionLoads.get(regionKey);
        if (pendingLoad != null) {
            return null;
        }

        if (!persistedRegionSources.contains(regionKey)) return null;
        scheduleRegionLoad(regionKey, regionX, regionZ, useFrameBudget, true, generation, distanceSquared);
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
        scheduleWaitingRegionLoads(Math.max(1, maxCompletions - remaining));
    }

    private void consumeReadyRegionSaves(int maxCompletions) {
        int remaining = maxCompletions;
        Iterator<Map.Entry<String, PendingRegionSave>> iterator = pendingRegionSaves.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            Map.Entry<String, PendingRegionSave> entry = iterator.next();
            PendingRegionSave pendingSave = entry.getValue();
            if (!pendingSave.future().isDone()) continue;

            iterator.remove();
            remaining--;
            completeRegionSave(entry.getKey(), pendingSave);
        }
    }

    private void completeRegionSave(String regionKey, PendingRegionSave pendingSave) {
        WorldMapAsync.RegionSaveResult result;
        try {
            result = pendingSave.future().join();
        } catch (Exception ignored) {
            result = null;
        }

        boolean success = result != null && result.success();
        pendingSave.region().finishSave(pendingSave.dirtyVersion(), success);
        if (!success) {
            Main.LOGGER.warn("Failed to save world map region {}", regionKey);
            return;
        }

        if (!pendingSave.region().isDirty()) {
            dirtyRegionKeys.remove(regionKey);
        }
        persistedRegionSources.add(regionKey);
        failedRegionLoads.remove(regionKey);
    }

    private void waitForPendingRegionSaves() {
        while (!pendingRegionSaves.isEmpty()) {
            ArrayList<Map.Entry<String, PendingRegionSave>> saves = new ArrayList<>(pendingRegionSaves.entrySet());
            pendingRegionSaves.clear();
            for (Map.Entry<String, PendingRegionSave> entry : saves) {
                completeRegionSave(entry.getKey(), entry.getValue());
            }
        }
    }

    private void saveAllDirtyRegionsAndWait() {
        int attempts = 0;
        while (hasDirtyRegions() || !pendingRegionSaves.isEmpty()) {
            saveDirtyRegions(Integer.MAX_VALUE, true);
            waitForPendingRegionSaves();
            attempts++;
            if (attempts > MAX_LOADED_REGIONS + MAX_PENDING_REGION_SAVES) {
                break;
            }
        }
    }

    private boolean hasDirtyRegions() {
        Iterator<String> iterator = dirtyRegionKeys.iterator();
        while (iterator.hasNext()) {
            String regionKey = iterator.next();
            WorldMapRegionTile region = loadedRegions.get(regionKey);
            if (region != null && region.isDirty()) return true;
            iterator.remove();
        }
        return false;
    }

    private WorldMapRegionTile consumeReadyRegionLoad(String regionKey, CachedRegionLoad pendingLoad) {
        pendingRegionLoads.remove(regionKey);

        try {
            WorldMapRegionPixels pixels;
            try {
                pixels = pendingLoad.future().join();
            } catch (CancellationException ignored) {
                return null;
            } catch (Exception ignored) {
                recordFailedRegionLoad(regionKey);
                WorldMapDebugProfiler.recordRegionLoadCompleted(false);
                return null;
            }
            if (pixels == null) {
                removeMissingRegionSource(regionKey, pendingLoad.regionX(), pendingLoad.regionZ());
                recordFailedRegionLoad(regionKey);
                WorldMapDebugProfiler.recordRegionLoadCompleted(false);
                return null;
            }

            if (loadedRegions.containsKey(regionKey)) {
                WorldMapDebugProfiler.recordRegionLoadCompleted(true);
                return loadedRegions.get(regionKey);
            }

            WorldMapRegionTile region = new WorldMapRegionTile(pendingLoad.regionX(), pendingLoad.regionZ());
            region.loadFromPixels(pixels, loadedRegionContentEpoch());
            touchRegion(region);
            loadedRegions.put(regionKey, region);
            persistedRegionSources.add(regionKey);
            failedRegionLoads.remove(regionKey);
            WorldMapDebugProfiler.recordRegionLoadCompleted(true);
            return region;
        } finally {
            queueRegionWake(regionKey);
        }
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
        regions.sort(Comparator.comparingLong(WorldMapRegionTile::getLastAccessOrder));

        for (WorldMapRegionTile region : regions) {
            if (loadedRegions.size() <= maxRegions) return;
            if (region.getRegionX() == protectedRegionX && region.getRegionZ() == protectedRegionZ) continue;

            String regionKey = key(region.getRegionX(), region.getRegionZ());
            if (region.isDirty() || pendingRegionSaves.containsKey(regionKey)) {
                saveRegion(region);
                continue;
            }

            loadedRegions.remove(regionKey);
            dirtyRegionKeys.remove(regionKey);
            region.close();
        }
    }

    public boolean isChunkExplored(ChunkPos chunk) {
        int regionX = WorldMapRegionTile.chunkToRegionCoord(chunk.x);
        int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunk.z);
        WorldMapRegionTile region = getLoadedRegion(regionX, regionZ);
        if (region == null) region = getOrScheduleCachedRegion(regionX, regionZ, false, 0L, 0);
        if (region == null) return false;

        int localX = WorldMapRegionTile.chunkLocalCoord(chunk.x) * WorldMapRegionTile.PIXELS_PER_CHUNK
                + WorldMapRegionTile.PIXELS_PER_CHUNK / 2;
        int localZ = WorldMapRegionTile.chunkLocalCoord(chunk.z) * WorldMapRegionTile.PIXELS_PER_CHUNK
                + WorldMapRegionTile.PIXELS_PER_CHUNK / 2;

        return region.hasVisiblePixel(localX, localZ);
    }

    public void onChunkLoaded(Level level, ChunkPos chunkPos) {
        if (!isCurrentClientLevel(level) || chunkPos == null) return;

        markChunkDirty(chunkPos.x, chunkPos.z, false, CHUNK_LOAD_SETTLE_NANOS, true);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) continue;

                int neighborChunkX = chunkPos.x + dx;
                int neighborChunkZ = chunkPos.z + dz;
                if (hasLoadedChunkPixels(neighborChunkX, neighborChunkZ)) {
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
        saveAllDirtyRegionsAndWait();
        closePendingRegionLoads();
        renderTileCache.close();
        WorldMapRenderer.releaseSharedResources();

        for (WorldMapRegionTile region : loadedRegions.values()) {
            region.close();
        }
        loadedRegions.clear();
        pendingRegionSaves.clear();
        dirtyRegionKeys.clear();
        persistedRegionSources.clear();
        failedRegionLoads.clear();
        chunkUpdateQueue.clear();
        queuedChunks.clear();
        urgentChunks.clear();
        forcedRebuildChunks.clear();
        chunkRevisions.clear();
        chunkReadyNanos.clear();
        chunkWaitingRegions.clear();
        chunksWaitingByRegion.clear();
        regionWakeQueue.clear();
        queuedRegionWakes.clear();
        discoveredChunks.clear();
        closePendingChunkBuilds();
        worldMapDir = null;
        regionDir = null;
        legacyLevelZeroDir = null;
        lastDiscoveryTime = 0L;
        lastSaveTime = 0L;
        savePassPending = false;
        lastEnqueueCenterChunkKey = Long.MIN_VALUE;
        nextChunkRevision = 0L;
        regionAccessSequence = 0L;
        enqueueOffsetCursor = 0;
        regionLoadSchedulesLeft = 0;
    }

    private WorldMapRegionTile getOrCreateRegion(int regionX, int regionZ) {
        String regionKey = key(regionX, regionZ);
        WorldMapRegionTile region = loadedRegions.get(regionKey);
        if (region == null) {
            CachedRegionLoad pendingLoad = pendingRegionLoads.get(regionKey);
            if (pendingLoad != null) {
                return null;
            }

            if (persistedRegionSources.contains(regionKey)) {
                if (!isRegionLoadRetryBlocked(regionKey)) {
                    scheduleRegionLoad(regionKey, regionX, regionZ, false, false, 0L, 0);
                }
                return null;
            }

            region = new WorldMapRegionTile(regionX, regionZ);
            region.createBlank(mapContentEpoch);
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
            enqueueOffsetCursor = 0;
            discardQueuedChunksOutside(centerChunkX, centerChunkZ, radius + 2);
            prioritizeQueuedChunksAround(centerChunkX, centerChunkZ);
        }

        discoverChunk(centerChunkX, centerChunkZ);

        long deadlineNanos = System.nanoTime() + CHUNK_ENQUEUE_BUDGET_NANOS;
        int visitedOffsets = 0;
        int checkedChunks = 0;
        while (visitedOffsets < CHUNK_SCAN_OFFSETS.length
                && checkedChunks < MAX_CHUNK_ENQUEUE_CHECKS_PER_REFRESH
                && queuedChunks.size() < MAX_QUEUED_CHUNK_UPDATES
                && System.nanoTime() < deadlineNanos) {
            ChunkScanOffset offset = CHUNK_SCAN_OFFSETS[enqueueOffsetCursor];
            enqueueOffsetCursor = (enqueueOffsetCursor + 1) % CHUNK_SCAN_OFFSETS.length;
            visitedOffsets++;

            if (offset.distanceSquared() > radius * radius) {
                enqueueOffsetCursor = 0;
                break;
            }

            checkedChunks++;
            discoverChunk(centerChunkX + offset.dx(), centerChunkZ + offset.dz());
        }
    }

    private int enqueueLoadedChunksForResourceRebuild() {
        if (mc.level == null || mc.player == null) return 0;

        int centerChunkX = mc.player.chunkPosition().x;
        int centerChunkZ = mc.player.chunkPosition().z;
        int radius = getUpdateRadiusChunks();
        int enqueued = 0;

        if (markChunkDirty(centerChunkX, centerChunkZ, false, 0L, true)) {
            enqueued++;
        }

        for (ChunkScanOffset offset : CHUNK_SCAN_OFFSETS) {
            if (offset.distanceSquared() > radius * radius || queuedChunks.size() >= MAX_QUEUED_CHUNK_UPDATES) {
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
        int keepRadiusSquared = keepRadius * keepRadius;
        Iterator<Long> iterator = chunkUpdateQueue.iterator();
        while (iterator.hasNext()) {
            long chunkKey = iterator.next();
            int dx = chunkX(chunkKey) - centerChunkX;
            int dz = chunkZ(chunkKey) - centerChunkZ;
            if (dx * dx + dz * dz <= keepRadiusSquared) continue;

            iterator.remove();
            queuedChunks.remove(chunkKey);
            urgentChunks.remove(chunkKey);
            forcedRebuildChunks.remove(chunkKey);
            chunkRevisions.remove(chunkKey);
            chunkReadyNanos.remove(chunkKey);
            removeChunkFromWaiters(chunkKey);
            discoveredChunks.remove(chunkKey);
        }

        Iterator<Map.Entry<Long, PendingChunkBuild>> pendingIterator = pendingChunkBuilds.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<Long, PendingChunkBuild> entry = pendingIterator.next();
            long chunkKey = entry.getKey();
            int dx = chunkX(chunkKey) - centerChunkX;
            int dz = chunkZ(chunkKey) - centerChunkZ;
            if (dx * dx + dz * dz <= keepRadiusSquared) continue;

            pendingIterator.remove();
            entry.getValue().cancel();
            urgentChunks.remove(chunkKey);
            forcedRebuildChunks.remove(chunkKey);
            chunkRevisions.remove(chunkKey);
            chunkReadyNanos.remove(chunkKey);
            removeChunkFromWaiters(chunkKey);
            discoveredChunks.remove(chunkKey);
        }

        ArrayList<Long> waitingChunks = new ArrayList<>(chunkWaitingRegions.keySet());
        for (long chunkKey : waitingChunks) {
            int dx = chunkX(chunkKey) - centerChunkX;
            int dz = chunkZ(chunkKey) - centerChunkZ;
            if (dx * dx + dz * dz > keepRadiusSquared) {
                forgetChunk(chunkKey);
            }
        }
    }

    private void prioritizeQueuedChunksAround(int centerChunkX, int centerChunkZ) {
        if (chunkUpdateQueue.size() < 2) return;

        ArrayList<Long> orderedChunks = new ArrayList<>(chunkUpdateQueue);
        orderedChunks.sort((left, right) -> {
            boolean leftUrgent = urgentChunks.contains(left);
            boolean rightUrgent = urgentChunks.contains(right);
            if (leftUrgent != rightUrgent) return leftUrgent ? -1 : 1;

            return Integer.compare(
                    distanceSquaredToChunk(left, centerChunkX, centerChunkZ),
                    distanceSquaredToChunk(right, centerChunkX, centerChunkZ)
            );
        });
        chunkUpdateQueue.clear();
        chunkUpdateQueue.addAll(orderedChunks);
    }

    private void discoverChunk(int chunkX, int chunkZ) {
        long chunkKey = chunkKey(chunkX, chunkZ);
        if (discoveredChunks.containsKey(chunkKey) || !isChunkLoaded(chunkX, chunkZ)) return;

        markChunkDirty(chunkX, chunkZ, false, 0L);
    }

    private boolean markChunkDirty(int chunkX, int chunkZ, boolean urgent, long settleNanos) {
        return markChunkDirty(chunkX, chunkZ, urgent, settleNanos, false);
    }

    private boolean markChunkDirty(int chunkX, int chunkZ, boolean urgent, long settleNanos, boolean forceRebuild) {
        if (!isChunkLoaded(chunkX, chunkZ)) return false;

        long chunkKey = chunkKey(chunkX, chunkZ);
        if (!forceRebuild && !urgent && completeCachedChunkIfLoaded(chunkKey, chunkX, chunkZ)) {
            return true;
        }

        long readyNanos = System.nanoTime() + settleNanos;
        if (forceRebuild && !urgent && chunkRevisions.containsKey(chunkKey)
                && forcedRebuildChunks.contains(chunkKey)) {
            if (queuedChunks.contains(chunkKey) || chunkWaitingRegions.containsKey(chunkKey)) {
                chunkReadyNanos.merge(chunkKey, readyNanos, Math::max);
                discoveredChunks.put(chunkKey, Boolean.TRUE);
                return true;
            }
        }

        if (forceRebuild) {
            forcedRebuildChunks.add(chunkKey);
        }
        chunkRevisions.put(chunkKey, ++nextChunkRevision);
        chunkReadyNanos.put(chunkKey, readyNanos);
        if (enqueueChunkForBuild(chunkKey, urgent)) {
            discoveredChunks.put(chunkKey, Boolean.TRUE);
            return true;
        } else {
            forcedRebuildChunks.remove(chunkKey);
            chunkRevisions.remove(chunkKey);
            chunkReadyNanos.remove(chunkKey);
            discoveredChunks.remove(chunkKey);
            return false;
        }
    }

    private boolean enqueueChunkForBuild(long chunkKey, boolean urgent) {
        if (pendingChunkBuilds.containsKey(chunkKey)) {
            removeChunkFromWaiters(chunkKey);
            if (urgent) urgentChunks.add(chunkKey);
            return true;
        }
        if (queuedChunks.contains(chunkKey)) {
            removeChunkFromWaiters(chunkKey);
            if (urgent && urgentChunks.add(chunkKey)) {
                chunkUpdateQueue.remove(chunkKey);
                chunkUpdateQueue.addFirst(chunkKey);
            }
            return true;
        }
        if (queuedChunks.size() >= MAX_QUEUED_CHUNK_UPDATES) return false;

        removeChunkFromWaiters(chunkKey);
        queuedChunks.add(chunkKey);
        if (urgent) {
            urgentChunks.add(chunkKey);
            chunkUpdateQueue.addFirst(chunkKey);
        } else {
            chunkUpdateQueue.addLast(chunkKey);
        }
        return true;
    }

    private boolean completeCachedChunkIfLoaded(long chunkKey, int chunkX, int chunkZ) {
        int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX);
        int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ);
        return completeCachedChunkIfPresent(chunkKey, chunkX, chunkZ, getLoadedRegion(regionX, regionZ));
    }

    private boolean completeCachedChunkIfPresent(long chunkKey, int chunkX, int chunkZ, WorldMapRegionTile region) {
        if (region == null || hasRequiredChunkBuild(chunkKey)) return false;

        int chunkXInRegion = WorldMapRegionTile.chunkLocalCoord(chunkX);
        int chunkZInRegion = WorldMapRegionTile.chunkLocalCoord(chunkZ);
        if (!region.hasFreshChunkPixels(chunkXInRegion, chunkZInRegion, mapContentEpoch)) return false;

        cancelChunkWork(chunkKey);
        discoveredChunks.put(chunkKey, Boolean.TRUE);
        return true;
    }

    private boolean hasRequiredChunkBuild(long chunkKey) {
        if (urgentChunks.contains(chunkKey)) return true;
        if (forcedRebuildChunks.contains(chunkKey)) return true;

        PendingChunkBuild pendingBuild = pendingChunkBuilds.get(chunkKey);
        return pendingBuild != null && (pendingBuild.urgent() || pendingBuild.forcedRebuild());
    }

    private void cancelChunkWork(long chunkKey) {
        PendingChunkBuild pendingBuild = pendingChunkBuilds.remove(chunkKey);
        if (pendingBuild != null) {
            pendingBuild.cancel();
        }
        if (queuedChunks.remove(chunkKey)) {
            chunkUpdateQueue.remove(chunkKey);
        }
        urgentChunks.remove(chunkKey);
        forcedRebuildChunks.remove(chunkKey);
        chunkRevisions.remove(chunkKey);
        chunkReadyNanos.remove(chunkKey);
        removeChunkFromWaiters(chunkKey);
    }

    private boolean hasLoadedChunkPixels(int chunkX, int chunkZ) {
        int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX);
        int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ);
        WorldMapRegionTile region = loadedRegions.get(key(regionX, regionZ));
        if (region == null) return false;

        return region.hasChunkPixels(
                WorldMapRegionTile.chunkLocalCoord(chunkX),
                WorldMapRegionTile.chunkLocalCoord(chunkZ)
        );
    }

    private static ChunkScanOffset[] buildChunkScanOffsets() {
        ArrayList<ChunkScanOffset> offsets = new ArrayList<>();
        int maxDistanceSquared = MAX_UPDATE_RADIUS_CHUNKS * MAX_UPDATE_RADIUS_CHUNKS;
        for (int dz = -MAX_UPDATE_RADIUS_CHUNKS; dz <= MAX_UPDATE_RADIUS_CHUNKS; dz++) {
            for (int dx = -MAX_UPDATE_RADIUS_CHUNKS; dx <= MAX_UPDATE_RADIUS_CHUNKS; dx++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared == 0 || distanceSquared > maxDistanceSquared) continue;
                offsets.add(new ChunkScanOffset(dx, dz, distanceSquared));
            }
        }
        offsets.sort(Comparator.comparingInt(ChunkScanOffset::distanceSquared));
        return offsets.toArray(ChunkScanOffset[]::new);
    }

    private void scheduleChunkBuilds(int maxSchedules) {
        if (mc.level == null || mc.player == null) return;

        int scheduled = 0;
        int attempts = 0;
        int maxAttempts = Math.min(MAX_CHUNK_BUILD_SCHEDULE_ATTEMPTS_PER_TICK, queuedChunks.size());
        long nowNanos = System.nanoTime();
        long deadlineNanos = nowNanos + CHUNK_BUILD_SCHEDULE_BUDGET_NANOS;
        int centerChunkX = mc.player.chunkPosition().x;
        int centerChunkZ = mc.player.chunkPosition().z;
        int cachedCompletions = 0;
        while (scheduled < maxSchedules && pendingChunkBuilds.size() < MAX_PENDING_CHUNK_BUILDS
                && !queuedChunks.isEmpty() && attempts < maxAttempts && System.nanoTime() < deadlineNanos) {
            BuildCandidate candidate = pollNextChunkForBuild(
                    nowNanos,
                    maxAttempts - attempts,
                    centerChunkX,
                    centerChunkZ,
                    deadlineNanos
            );
            if (candidate == null) return;
            attempts += candidate.checkedChunks();
            if (!candidate.hasChunk()) break;

            long chunkKey = candidate.chunkKey();
            boolean urgent = candidate.urgent();

            Long revision = chunkRevisions.get(chunkKey);
            if (revision == null) continue;

            long readyNanos = chunkReadyNanos.getOrDefault(chunkKey, 0L);
            if (nowNanos < readyNanos) {
                enqueueDelayedChunk(chunkKey, urgent);
                continue;
            }

            int chunkX = chunkX(chunkKey);
            int chunkZ = chunkZ(chunkKey);
            boolean forcedRebuild = forcedRebuildChunks.contains(chunkKey);
            ChunkSamplingContext context = ChunkSamplingContext.capture(mc.level, chunkX, chunkZ);
            if (context == null) {
                forgetChunk(chunkKey);
                continue;
            }

            int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX);
            int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ);
            String regionKey = key(regionX, regionZ);
            WorldMapRegionTile region = getOrCreateRegion(regionX, regionZ);
            if (region == null) {
                parkChunkForRegion(regionKey, chunkKey, urgent);
                if (pendingRegionLoads.containsKey(regionKey) || persistedRegionSources.contains(regionKey)) {
                    break;
                }
                continue;
            }
            if (!urgent && !forcedRebuild && completeCachedChunkIfPresent(chunkKey, chunkX, chunkZ, region)) {
                cachedCompletions++;
                if (cachedCompletions >= MAX_CACHED_CHUNK_COMPLETIONS_PER_TICK) {
                    break;
                }
                continue;
            }

            ChunkImageBuilder builder = ChunkImageBuilder.begin(context, chunkX, chunkZ, chunkKey);
            CompletableFuture<ChunkBuildResult> future = WorldMapAsync.buildChunk(
                    chunkX + "," + chunkZ,
                    urgent,
                    distanceSquaredToChunk(chunkKey, centerChunkX, centerChunkZ),
                    builder::buildFully
            );
            pendingChunkBuilds.put(chunkKey, new PendingChunkBuild(
                    context,
                    revision,
                    builder,
                    future,
                    urgent,
                    forcedRebuild
            ));
            scheduled++;
        }
    }

    private BuildCandidate pollNextChunkForBuild(long nowNanos, int maxAttempts, int centerChunkX, int centerChunkZ,
                                                 long deadlineNanos) {
        if (maxAttempts <= 0) return null;

        Long bestChunkKey = null;
        boolean bestUrgent = false;
        int bestDistanceSquared = Integer.MAX_VALUE;
        int checkedChunks = 0;
        Iterator<Long> iterator = chunkUpdateQueue.iterator();
        while (iterator.hasNext()
                && checkedChunks < maxAttempts
                && System.nanoTime() < deadlineNanos) {
            long chunkKey = iterator.next();
            checkedChunks++;

            if (!queuedChunks.contains(chunkKey)) {
                iterator.remove();
                continue;
            }

            Long revision = chunkRevisions.get(chunkKey);
            if (revision == null) {
                iterator.remove();
                queuedChunks.remove(chunkKey);
                urgentChunks.remove(chunkKey);
                forcedRebuildChunks.remove(chunkKey);
                chunkReadyNanos.remove(chunkKey);
                continue;
            }

            long readyNanos = chunkReadyNanos.getOrDefault(chunkKey, 0L);
            if (nowNanos < readyNanos) continue;

            boolean urgent = urgentChunks.contains(chunkKey);
            int distanceSquared = distanceSquaredToChunk(chunkKey, centerChunkX, centerChunkZ);
            if (bestChunkKey == null || isHigherPriority(
                    urgent,
                    distanceSquared,
                    chunkKey,
                    bestUrgent,
                    bestDistanceSquared,
                    bestChunkKey
            )) {
                bestChunkKey = chunkKey;
                bestUrgent = urgent;
                bestDistanceSquared = distanceSquared;
            }
        }

        if (bestChunkKey == null) {
            return new BuildCandidate(Long.MIN_VALUE, false, checkedChunks);
        }

        chunkUpdateQueue.remove(bestChunkKey);
        queuedChunks.remove(bestChunkKey);
        return new BuildCandidate(bestChunkKey, urgentChunks.remove(bestChunkKey) || bestUrgent, checkedChunks);
    }

    private int consumeReadyChunkBuilds(int maxCompletions) {
        int completed = 0;
        Iterator<Map.Entry<Long, PendingChunkBuild>> iterator = pendingChunkBuilds.entrySet().iterator();
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
            Long currentRevision = chunkRevisions.get(chunkKey);
            boolean resultIsCurrent = currentRevision != null
                    && currentRevision == pendingBuild.revision()
                    && pendingBuild.context().belongsTo(mc.level)
                    && isChunkLoaded(chunkX(chunkKey), chunkZ(chunkKey));
            if (resultIsCurrent && result != null) {
                if (finishChunkBuild(result)) {
                    chunkRevisions.remove(chunkKey, currentRevision);
                    chunkReadyNanos.remove(chunkKey);
                    forcedRebuildChunks.remove(chunkKey);
                } else {
                    int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX(chunkKey));
                    int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ(chunkKey));
                    parkChunkForRegion(key(regionX, regionZ), chunkKey, urgentRetry);
                }
            } else if (pendingBuild.context().belongsTo(mc.level)
                    && isChunkLoaded(chunkX(chunkKey), chunkZ(chunkKey))) {
                enqueueDeferredChunk(chunkKey, urgentRetry);
            }
        }
        return completed;
    }

    private boolean finishChunkBuild(ChunkBuildResult result) {
        long finalizeStartNanos = System.nanoTime();
        long regionWriteNanos = 0L;
        try {
            int chunkX = result.chunkX();
            int chunkZ = result.chunkZ();
            int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX);
            int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ);
            WorldMapRegionTile region = getOrCreateRegion(regionX, regionZ);
            if (region == null) {
                return false;
            }

            long regionWriteStartNanos = System.nanoTime();
            region.updateFromChunkPixels(
                    result.pixels(),
                    WorldMapRegionTile.chunkLocalCoord(chunkX),
                    WorldMapRegionTile.chunkLocalCoord(chunkZ),
                    mapContentEpoch
            );
            renderTileCache.invalidateChunk(
                    region,
                    WorldMapRegionTile.chunkLocalCoord(chunkX),
                    WorldMapRegionTile.chunkLocalCoord(chunkZ),
                    result.pixels()
            );
            dirtyRegionKeys.add(key(regionX, regionZ));
            regionWriteNanos = System.nanoTime() - regionWriteStartNanos;
            return true;
        } finally {
            WorldMapDebugProfiler.recordChunkFinalize(
                    System.nanoTime() - finalizeStartNanos,
                    regionWriteNanos,
                    0L
            );
        }
    }

    private void enqueueDeferredChunk(long chunkKey, boolean urgent) {
        if (urgent) {
            urgentChunks.add(chunkKey);
        }
        if (queuedChunks.add(chunkKey)) {
            if (urgent) {
                chunkUpdateQueue.addFirst(chunkKey);
            } else {
                chunkUpdateQueue.addLast(chunkKey);
            }
        } else if (urgent && chunkUpdateQueue.remove(chunkKey)) {
            chunkUpdateQueue.addFirst(chunkKey);
        }
    }

    private void enqueueDelayedChunk(long chunkKey, boolean urgent) {
        if (urgent) {
            urgentChunks.add(chunkKey);
        }
        if (queuedChunks.add(chunkKey)) {
            chunkUpdateQueue.addLast(chunkKey);
        }
    }

    private void parkChunkForRegion(String regionKey, long chunkKey, boolean urgent) {
        removeChunkFromRegionWait(chunkKey);
        chunkWaitingRegions.put(chunkKey, regionKey);
        chunksWaitingByRegion.computeIfAbsent(regionKey, ignored -> new HashSet<>()).add(chunkKey);
        if (urgent) urgentChunks.add(chunkKey);
    }

    private void queueRegionWake(String regionKey) {
        if (regionKey != null && queuedRegionWakes.add(regionKey)) {
            regionWakeQueue.addLast(regionKey);
        }
    }

    private void processRegionWakeQueue(int maxChunks, long budgetNanos) {
        long deadlineNanos = System.nanoTime() + budgetNanos;
        int processed = 0;
        while (processed < maxChunks && !regionWakeQueue.isEmpty() && System.nanoTime() < deadlineNanos) {
            String regionKey = regionWakeQueue.peekFirst();
            Set<Long> waitingChunks = chunksWaitingByRegion.get(regionKey);
            if (waitingChunks == null || waitingChunks.isEmpty()) {
                chunksWaitingByRegion.remove(regionKey);
                regionWakeQueue.removeFirst();
                queuedRegionWakes.remove(regionKey);
                continue;
            }

            Iterator<Long> iterator = waitingChunks.iterator();
            long chunkKey = iterator.next();
            iterator.remove();
            chunkWaitingRegions.remove(chunkKey, regionKey);
            if (!chunkRevisions.containsKey(chunkKey) || !isChunkLoaded(chunkX(chunkKey), chunkZ(chunkKey))) {
                forgetChunk(chunkKey);
            } else {
                boolean urgent = urgentChunks.remove(chunkKey);
                boolean forcedRebuild = forcedRebuildChunks.contains(chunkKey);
                WorldMapRegionTile region = loadedRegions.get(regionKey);
                if (!urgent
                        && !forcedRebuild
                        && completeCachedChunkIfPresent(chunkKey, chunkX(chunkKey), chunkZ(chunkKey), region)) {
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
        if (pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS || chunksWaitingByRegion.isEmpty()) return;

        int scheduled = 0;
        for (Map.Entry<String, Set<Long>> entry : new ArrayList<>(chunksWaitingByRegion.entrySet())) {
            if (scheduled >= maxRegions || pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS) return;

            String regionKey = entry.getKey();
            if (pendingRegionLoads.containsKey(regionKey)) continue;
            if (loadedRegions.containsKey(regionKey)) {
                queueRegionWake(regionKey);
                continue;
            }

            Iterator<Long> waitingChunks = entry.getValue().iterator();
            if (!waitingChunks.hasNext()) {
                chunksWaitingByRegion.remove(regionKey);
                continue;
            }

            long chunkKey = waitingChunks.next();
            int regionX = WorldMapRegionTile.chunkToRegionCoord(chunkX(chunkKey));
            int regionZ = WorldMapRegionTile.chunkToRegionCoord(chunkZ(chunkKey));
            if (scheduleRegionLoad(regionKey, regionX, regionZ, false, false, 0L, 0)) {
                scheduled++;
            }
        }
    }

    private void removeChunkFromWaiters(long chunkKey) {
        removeChunkFromRegionWait(chunkKey);
    }

    private void removeChunkFromRegionWait(long chunkKey) {
        String regionKey = chunkWaitingRegions.remove(chunkKey);
        if (regionKey == null) return;

        Set<Long> waitingChunks = chunksWaitingByRegion.get(regionKey);
        if (waitingChunks == null) return;
        waitingChunks.remove(chunkKey);
        if (waitingChunks.isEmpty()) {
            chunksWaitingByRegion.remove(regionKey);
        }
    }

    private void forgetChunk(long chunkKey) {
        PendingChunkBuild pendingBuild = pendingChunkBuilds.remove(chunkKey);
        if (pendingBuild != null) {
            pendingBuild.cancel();
        }
        if (queuedChunks.remove(chunkKey)) {
            chunkUpdateQueue.remove(chunkKey);
        }
        urgentChunks.remove(chunkKey);
        forcedRebuildChunks.remove(chunkKey);
        chunkRevisions.remove(chunkKey);
        chunkReadyNanos.remove(chunkKey);
        removeChunkFromWaiters(chunkKey);
        discoveredChunks.remove(chunkKey);
    }

    private void closePendingChunkBuilds() {
        for (PendingChunkBuild pendingBuild : pendingChunkBuilds.values()) {
            pendingBuild.cancel();
        }
        pendingChunkBuilds.clear();
    }

    private boolean scheduleRegionLoad(String regionKey, int regionX, int regionZ, boolean useFrameBudget,
                                       boolean viewOnly, long generation, int distanceSquared) {
        if (isRegionLoadRetryBlocked(regionKey)) return false;
        if (pendingRegionLoads.containsKey(regionKey)) return false;
        if (pendingRegionLoads.size() >= MAX_PENDING_REGION_LOADS) return false;
        if (useFrameBudget) {
            if (regionLoadSchedulesLeft <= 0) return false;
            regionLoadSchedulesLeft--;
        }

        File activeRegionFile = getRegionFile(regionX, regionZ);
        File[] candidates = regionLoadCandidates(regionX, regionZ);
        CompletableFuture<WorldMapRegionPixels> future = WorldMapAsync.loadRegion(
                regionX + "," + regionZ,
                !viewOnly,
                generation,
                distanceSquared,
                () -> readRegionPixelsIfPresent(activeRegionFile, candidates));
        pendingRegionLoads.put(regionKey, new CachedRegionLoad(regionX, regionZ, future));
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
        int chunkPipelineSize = queuedChunks.size() + pendingChunkBuilds.size() + chunkWaitingRegions.size();
        WorldMapDebugProfiler.recordTileManagerState(
                loadedRegions.size(),
                pendingRegionLoads.size(),
                failedRegionLoads.size(),
                pendingRegionSaves.size(),
                chunkUpdateQueue.size(),
                chunkPipelineSize,
                renderTileCache.tileCount(),
                renderTileCache.pendingCount()
        );
    }

    private boolean isCurrentClientLevel(Level level) {
        return level != null && level.isClientSide && level == mc.level;
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

    private int loadedRegionContentEpoch() {
        return cachedRegionsStaleForResourceReload ? Math.max(0, mapContentEpoch - 1) : mapContentEpoch;
    }

    private int saveDirtyRegions(int maxRegions, boolean force) {
        if (regionDir == null) return 0;
        long nowNanos = System.nanoTime();
        int saved = 0;
        Iterator<String> iterator = dirtyRegionKeys.iterator();
        while (iterator.hasNext()) {
            String regionKey = iterator.next();
            WorldMapRegionTile region = loadedRegions.get(regionKey);
            if (region == null || !region.isDirty()) {
                iterator.remove();
                continue;
            }
            if (force || region.isReadyForBackgroundSave(nowNanos, REGION_SAVE_QUIET_PERIOD_NANOS)) {
                if (saveRegion(region)) {
                    saved++;
                }
                if (saved >= maxRegions) break;
            }
        }
        return saved;
    }

    private boolean saveRegion(WorldMapRegionTile region) {
        String regionKey = key(region.getRegionX(), region.getRegionZ());
        if (pendingRegionSaves.containsKey(regionKey)) return false;
        if (pendingRegionSaves.size() >= MAX_PENDING_REGION_SAVES) return false;

        WorldMapRegionTile.SaveSnapshot snapshot = region.beginSaveSnapshot();
        if (snapshot == null) return false;

        File regionFile = getRegionFile(region.getRegionX(), region.getRegionZ());
        CompletableFuture<WorldMapAsync.RegionSaveResult> future =
                WorldMapAsync.saveRegion(regionFile, snapshot);
        pendingRegionSaves.put(regionKey, new PendingRegionSave(region, snapshot.dirtyVersion(), future));
        return true;
    }

    private File getRegionFile(int regionX, int regionZ) {
        return getRegionFile(regionDir, regionX, regionZ);
    }

    private void rebuildRegionSourceIndex() {
        persistedRegionSources.clear();
        recoverInterruptedRegionSaves(regionDir);
        indexRegionSourceDir(regionDir);
        recoverInterruptedRegionSaves(legacyLevelZeroDir);
        indexRegionSourceDir(legacyLevelZeroDir);
        if (worldMapDir != null && !worldMapDir.equals(regionDir)) {
            recoverInterruptedRegionSaves(worldMapDir);
            indexRegionSourceDir(worldMapDir);
        }
    }

    private void recoverInterruptedRegionSaves(File sourceDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) return;

        File[] temporaryFiles = sourceDir.listFiles(file ->
                file.getName().endsWith(WorldMapRegionStorage.DATA_EXTENSION + ".new")
                        || file.getName().endsWith(WorldMapLegacyRegionImporter.IMAGE_EXTENSION + ".new"));
        if (temporaryFiles == null) return;

        for (File temporaryFile : temporaryFiles) {
            String originalName = temporaryFile.getName().substring(0, temporaryFile.getName().length() - 4);
            File destinationFile = new File(sourceDir, originalName);
            if (destinationFile.exists()) {
                temporaryFile.delete();
                continue;
            }
            recoverInterruptedRegionSave(temporaryFile, destinationFile);
        }
    }

    private void recoverInterruptedRegionSave(File temporaryFile, File destinationFile) {
        try {
            try {
                Files.move(temporaryFile.toPath(), destinationFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile.toPath(), destinationFile.toPath());
            }
        } catch (IOException exception) {
            Main.LOGGER.debug("Could not recover interrupted world map region save {}", temporaryFile, exception);
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
        if (!WorldMapRegionStorage.isUsable(file) && !WorldMapLegacyRegionImporter.isUsable(file)) return null;

        String name = file.getName();
        String extension;
        if (name.endsWith(WorldMapRegionStorage.DATA_EXTENSION)) {
            extension = WorldMapRegionStorage.DATA_EXTENSION;
        } else if (name.endsWith(WorldMapLegacyRegionImporter.IMAGE_EXTENSION)) {
            extension = WorldMapLegacyRegionImporter.IMAGE_EXTENSION;
        } else {
            return null;
        }

        String coords = name.substring(0, name.length() - extension.length());
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

    private WorldMapRegionPixels readRegionPixelsIfPresent(File activeRegionFile, File... candidates) {
        for (File candidate : candidates) {
            WorldMapRegionStorage.ReadResult result = WorldMapRegionStorage.read(candidate);
            if (result.status() == WorldMapRegionStorage.ReadStatus.SUCCESS) {
                return result.pixels();
            }
            if (result.status() == WorldMapRegionStorage.ReadStatus.CORRUPT) {
                quarantineCorruptRegion(candidate);
            }

            int[] pixels = WorldMapLegacyRegionImporter.read(candidate);
            if (pixels == null) continue;

            migrateLegacyRegion(activeRegionFile, pixels);
            return WorldMapRegionPixels.fromBase(pixels);
        }
        return null;
    }

    private void removeMissingRegionSource(String regionKey, int regionX, int regionZ) {
        for (File candidate : regionLoadCandidates(regionX, regionZ)) {
            if (WorldMapRegionStorage.isUsable(candidate) || WorldMapLegacyRegionImporter.isUsable(candidate)) {
                return;
            }
        }
        persistedRegionSources.remove(regionKey);
        failedRegionLoads.remove(regionKey);
    }

    private void quarantineCorruptRegion(File file) {
        if (file == null || !file.exists()) return;

        File quarantined = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file.toPath(), quarantined.toPath());
            Main.LOGGER.warn("Moved corrupt world map region {} to {}", file, quarantined);
        } catch (IOException exception) {
            Main.LOGGER.warn("Could not quarantine corrupt world map region {}", file, exception);
        }
    }

    private void migrateLegacyRegion(File activeRegionFile, int[] pixels) {
        if (activeRegionFile == null) return;

        try {
            WorldMapRegionStorage.write(activeRegionFile, pixels);
        } catch (Exception exception) {
            Main.LOGGER.debug("Could not migrate legacy world map region {}", activeRegionFile, exception);
        }
    }

    private File[] regionLoadCandidates(int regionX, int regionZ) {
        return new File[]{
                getRegionFile(regionDir, regionX, regionZ),
                getLegacyRegionFile(regionDir, regionX, regionZ),
                getRegionFile(legacyLevelZeroDir, regionX, regionZ),
                getLegacyRegionFile(legacyLevelZeroDir, regionX, regionZ),
                getRegionFile(worldMapDir, regionX, regionZ),
                getLegacyRegionFile(worldMapDir, regionX, regionZ)
        };
    }

    private static File getRegionFile(File sourceDir, int regionX, int regionZ) {
        return sourceDir == null
                ? null
                : new File(sourceDir, regionX + "_" + regionZ + WorldMapRegionStorage.DATA_EXTENSION);
    }

    private static File getLegacyRegionFile(File sourceDir, int regionX, int regionZ) {
        return sourceDir == null
                ? null
                : new File(sourceDir, regionX + "_" + regionZ + WorldMapLegacyRegionImporter.IMAGE_EXTENSION);
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
        boolean queuedUrgent = urgentChunks.remove(chunkKey);
        return pendingBuild.urgent() || queuedUrgent;
    }

    private static boolean isHigherPriority(boolean urgent, int distanceSquared, long chunkKey,
                                            boolean bestUrgent, int bestDistanceSquared, long bestChunkKey) {
        if (urgent != bestUrgent) return urgent;
        if (distanceSquared != bestDistanceSquared) return distanceSquared < bestDistanceSquared;
        return Long.compareUnsigned(chunkKey, bestChunkKey) < 0;
    }

    private static String key(int x, int z) {
        return x + "_" + z;
    }

    private WorldMapRegionTile touchRegion(WorldMapRegionTile region) {
        region.markAccessed(++regionAccessSequence);
        return region;
    }

    private void closePendingRegionLoads() {
        for (CachedRegionLoad pendingLoad : pendingRegionLoads.values()) {
            pendingLoad.future().cancel(false);
        }
        pendingRegionLoads.clear();
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

    private record CachedRegionLoad(int regionX, int regionZ, CompletableFuture<WorldMapRegionPixels> future) {
    }

    private record PendingRegionSave(WorldMapRegionTile region, int dirtyVersion,
                                     CompletableFuture<WorldMapAsync.RegionSaveResult> future) {
    }

    private record PendingChunkBuild(ChunkSamplingContext context, long revision, ChunkImageBuilder builder,
                                     CompletableFuture<ChunkBuildResult> future, boolean urgent,
                                     boolean forcedRebuild) {
        private void cancel() {
            builder.cancel();
            future.cancel(false);
        }
    }

    private record BuildCandidate(long chunkKey, boolean urgent, int checkedChunks) {
        private boolean hasChunk() {
            return chunkKey != Long.MIN_VALUE;
        }
    }

    private record ChunkScanOffset(int dx, int dz, int distanceSquared) {
    }

}
