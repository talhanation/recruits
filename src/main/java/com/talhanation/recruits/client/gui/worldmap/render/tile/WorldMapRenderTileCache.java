package com.talhanation.recruits.client.gui.worldmap.render.tile;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.worldmap.pipeline.WorldMapAsync;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegion;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Persistent GPU-facing tile hierarchy. Canonical map data stays in 512px regions, while rendering
 * only publishes small fixed-size textures for the current view.
 */
public final class WorldMapRenderTileCache {
    private static final int MAX_CACHED_TILES = 768;
    private static final int TRIMMED_CACHED_TILES = 640;
    private static final int MAX_PENDING_BUILDS = 96;
    private static final int MAX_READY_TILES = 128;
    private static final int MAX_BUILD_SCHEDULES_PER_FRAME = 8;
    private static final int MAX_BUILD_COMPLETIONS_PER_FRAME = 12;
    private static final int MAX_TEXTURE_UPLOADS_PER_FRAME = 8;
    private static final int OFFSCREEN_BUILD_GRACE_GENERATIONS = 3;
    private static final long TEXTURE_UPLOAD_BUDGET_NANOS = 1_000_000L;

    private final WorldMapCacheManager mapCache;
    private final WorldMapTextureUploader textureUploader = new WorldMapTextureUploader();
    private final List<WorldMapTextureAtlas> textureAtlases = new ArrayList<>();
    private final Map<WorldMapRenderTileKey, WorldMapRenderTile> tiles = new HashMap<>();
    private final Map<WorldMapRenderTileKey, PendingBuild> pendingBuilds = new LinkedHashMap<>();
    private final Map<WorldMapRenderTileKey, WorldMapRegion.RenderSnapshot> readyTiles =
            new LinkedHashMap<>();
    private final Map<WorldMapRenderTileKey, Integer> invalidatedTiles = new HashMap<>();
    private final Set<WorldMapRenderTileKey> dirtyTiles = new HashSet<>();
    private final List<WorldMapRenderTileKey> lastRequestedTiles = new ArrayList<>();
    private final List<WorldMapRenderTileKey> neededTiles = new ArrayList<>();
    private final Set<WorldMapRenderTileKey> neededTileSet = new HashSet<>();

    private long visibleGeneration;
    private long accessSequence;
    private WorldMapRenderTileKey lastCenterTile;
    private boolean lastIncludeAncestors = true;

    public WorldMapRenderTileCache(WorldMapCacheManager mapCache) {
        this.mapCache = mapCache;
    }

    public void prepareGpuResources() {
        if (!RenderSystem.isOnRenderThreadOrInit()) return;

        textureUploader.prepare();
        if (textureAtlases.isEmpty()) {
            textureAtlases.add(new WorldMapTextureAtlas());
        }
    }

    public void prepareVisible(List<WorldMapRenderTileKey> requestedTiles, boolean includeAncestors) {
        prepareGpuResources();
        consumeReadyBuilds(MAX_BUILD_COMPLETIONS_PER_FRAME);
        refreshNeededTiles(requestedTiles, includeAncestors);
        discardUnneededReadyTiles();
        scheduleBuilds(neededTiles, MAX_BUILD_SCHEDULES_PER_FRAME);
        UploadBudget uploadBudget = new UploadBudget();
        publishDirtyTiles(neededTiles, uploadBudget);
        publishReadyTiles(neededTiles, uploadBudget);
        trim(neededTileSet);
    }

    public TileView findBestAvailable(WorldMapRenderTileKey requested, boolean allowParentFallback) {
        WorldMapRenderTile exact = tiles.get(requested);
        if (exact != null) {
            return TileView.full(exact.slot(nextAccessOrder()));
        }
        if (!allowParentFallback) {
            return null;
        }

        WorldMapRenderTileKey parent = requested.parent();
        int levelDifference = 1;
        while (parent != null) {
            WorldMapRenderTile parentTile = tiles.get(parent);
            if (parentTile != null) {
                WorldMapTextureAtlas.Slot slot = parentTile.slot(nextAccessOrder());
                int sections = 1 << levelDifference;
                int localX = Math.floorMod(requested.x(), sections);
                int localZ = Math.floorMod(requested.z(), sections);
                float atlasPixel = 1.0F / WorldMapTextureAtlas.ATLAS_SIZE;
                float sectionPixels = (float) WorldMapRenderTileKey.PIXEL_SIZE / sections;
                float u1 = slot.u1() + localX * sectionPixels * atlasPixel;
                float v1 = slot.v1() + localZ * sectionPixels * atlasPixel;
                float u2 = slot.u1() + (localX + 1) * sectionPixels * atlasPixel;
                float v2 = slot.v1() + (localZ + 1) * sectionPixels * atlasPixel;
                return new TileView(slot.atlas().textureId(), u1, v1, u2, v2);
            }
            parent = parent.parent();
            levelDifference++;
        }
        return null;
    }

    public TileView findExact(WorldMapRenderTileKey key) {
        WorldMapRenderTile tile = tiles.get(key);
        return tile == null ? null : TileView.full(tile.slot(nextAccessOrder()));
    }

    public int tileCount() {
        return tiles.size();
    }

    public int pendingCount() {
        return pendingBuilds.size() + readyTiles.size();
    }

    public void invalidateChunk(
            WorldMapRegion region, int chunkXInRegion, int chunkZInRegion, int[] chunkPixels) {
        int baseTileX =
                chunkXInRegion * WorldMapRegion.PIXELS_PER_CHUNK / WorldMapRenderTileKey.PIXEL_SIZE;
        int baseTileZ =
                chunkZInRegion * WorldMapRegion.PIXELS_PER_CHUNK / WorldMapRenderTileKey.PIXEL_SIZE;

        for (int level = 0; level <= WorldMapRenderTileKey.MAX_LEVEL; level++) {
            int localX = baseTileX >> level;
            int localZ = baseTileZ >> level;
            WorldMapRenderTileKey key =
                    WorldMapRenderTileKey.fromRegionLocal(
                            level, region.getRegionX(), region.getRegionZ(), localX, localZ);
            int sourceVersion = region.renderVersion(level, localX, localZ);
            WorldMapRenderTile tile = tiles.get(key);
            if (tile != null) {
                discardPendingBuild(key);
                discardReadyTile(key);
                tile.patchChunk(chunkPixels, chunkXInRegion, chunkZInRegion, sourceVersion);
                dirtyTiles.add(key);
                invalidatedTiles.remove(key);
            } else if (isTracked(key)) {
                invalidatedTiles.put(key, sourceVersion);
            }
        }
    }

    public void close() {
        for (PendingBuild pendingBuild : pendingBuilds.values()) {
            cancelPendingBuild(pendingBuild);
        }
        pendingBuilds.clear();

        for (WorldMapRegion.RenderSnapshot snapshot : readyTiles.values()) {
            snapshot.release();
        }
        readyTiles.clear();
        invalidatedTiles.clear();
        dirtyTiles.clear();

        for (WorldMapRenderTile tile : tiles.values()) {
            tile.releaseSlot();
        }
        tiles.clear();
        for (WorldMapTextureAtlas textureAtlas : textureAtlases) {
            textureAtlas.close();
        }
        textureAtlases.clear();
        textureUploader.close();
        lastRequestedTiles.clear();
        neededTiles.clear();
        neededTileSet.clear();
        lastCenterTile = null;
        lastIncludeAncestors = true;
        visibleGeneration = 0L;
        accessSequence = 0L;
    }

    private void refreshNeededTiles(
            List<WorldMapRenderTileKey> requestedTiles, boolean includeAncestors) {
        if (lastIncludeAncestors == includeAncestors && lastRequestedTiles.equals(requestedTiles))
            return;

        lastRequestedTiles.clear();
        lastRequestedTiles.addAll(requestedTiles);
        lastIncludeAncestors = includeAncestors;
        neededTiles.clear();
        neededTileSet.clear();
        if (requestedTiles.isEmpty()) {
            updateVisibleGeneration(requestedTiles);
            return;
        }

        if (includeAncestors) {
            // Publish coarse parents first so a stable map remains visible while
            // the current zoom level is still loading.
            for (int targetLevel = WorldMapRenderTileKey.MAX_LEVEL;
                    targetLevel > requestedTiles.get(0).level();
                    targetLevel--) {
                for (WorldMapRenderTileKey requested : requestedTiles) {
                    WorldMapRenderTileKey ancestor = ancestorAtLevel(requested, targetLevel);
                    if (ancestor != null && neededTileSet.add(ancestor)) {
                        neededTiles.add(ancestor);
                    }
                }
            }
        }
        for (WorldMapRenderTileKey requested : requestedTiles) {
            if (neededTileSet.add(requested)) {
                neededTiles.add(requested);
            }
        }
        updateVisibleGeneration(requestedTiles);
    }

    private void updateVisibleGeneration(List<WorldMapRenderTileKey> requestedTiles) {
        WorldMapRenderTileKey centerTile = requestedTiles.isEmpty() ? null : requestedTiles.get(0);
        if (centerTile != null && !centerTile.equals(lastCenterTile)) {
            lastCenterTile = centerTile;
            visibleGeneration++;
        }

        pruneOffscreenPendingBuilds(false);
    }

    private static WorldMapRenderTileKey ancestorAtLevel(WorldMapRenderTileKey key, int targetLevel) {
        WorldMapRenderTileKey ancestor = key;
        while (ancestor != null && ancestor.level() < targetLevel) {
            ancestor = ancestor.parent();
        }
        return ancestor;
    }

    private void pruneOffscreenPendingBuilds(boolean makeRoom) {
        int targetSize =
                makeRoom ? MAX_PENDING_BUILDS - MAX_BUILD_SCHEDULES_PER_FRAME : MAX_PENDING_BUILDS;
        Iterator<Map.Entry<WorldMapRenderTileKey, PendingBuild>> pendingIterator =
                pendingBuilds.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<WorldMapRenderTileKey, PendingBuild> entry = pendingIterator.next();
            if (neededTileSet.contains(entry.getKey())) continue;
            PendingBuild pendingBuild = entry.getValue();
            boolean expired =
                    visibleGeneration - pendingBuild.scheduledGeneration()
                            >= OFFSCREEN_BUILD_GRACE_GENERATIONS;
            if (!expired && pendingBuilds.size() <= targetSize) continue;

            cancelPendingBuild(entry.getValue());
            pendingIterator.remove();
            if (makeRoom && pendingBuilds.size() <= targetSize) return;
        }
    }

    private void scheduleBuilds(List<WorldMapRenderTileKey> neededTiles, int maxSchedules) {
        if (pendingBuilds.size() >= MAX_PENDING_BUILDS - maxSchedules) {
            pruneOffscreenPendingBuilds(true);
        }

        ArrayList<BuildRequest> requests = new ArrayList<>(maxSchedules);
        int scheduled = 0;
        for (int index = 0; index < neededTiles.size() && scheduled < maxSchedules; index++) {
            if (pendingBuilds.size() + requests.size() >= MAX_PENDING_BUILDS) break;

            WorldMapRenderTileKey key = neededTiles.get(index);
            WorldMapRenderTile tile = tiles.get(key);
            Integer invalidatedVersion = invalidatedTiles.get(key);
            if (tile != null && invalidatedVersion == null) continue;

            WorldMapRegion region = resolveRegion(key, index);
            if (region == null) continue;

            int sourceVersion = sourceVersion(key, region);
            if (tile != null && tile.sourceVersion() == sourceVersion) {
                invalidatedTiles.remove(key, sourceVersion);
                continue;
            }

            PendingBuild pending = pendingBuilds.get(key);
            if (pending != null) {
                if (pending.sourceVersion() == sourceVersion) continue;
                cancelPendingBuild(pending);
                pendingBuilds.remove(key);
            }

            WorldMapRegion.RenderSnapshot ready = readyTiles.get(key);
            if (ready != null) {
                if (ready.sourceVersion() == sourceVersion) continue;
                readyTiles.remove(key);
                ready.release();
            }

            requests.add(new BuildRequest(key, region, sourceVersion, index));
            scheduled++;
        }

        // Submit only after collecting requests so workers cannot hold a region lock
        // while this render-thread pass is still inspecting nearby tiles.
        for (BuildRequest request : requests) {
            WorldMapRenderTileKey key = request.key();
            CompletableFuture<WorldMapRegion.RenderSnapshot> future =
                    WorldMapAsync.buildRenderTile(
                            visibleGeneration,
                            request.priority(),
                            () ->
                                    request
                                            .region()
                                            .createRenderSnapshot(
                                                    key.level(),
                                                    key.localXInRegion(),
                                                    key.localZInRegion(),
                                                    request.sourceVersion()));
            pendingBuilds.put(key, new PendingBuild(request.sourceVersion(), visibleGeneration, future));
        }
    }

    private void consumeReadyBuilds(int maxCompletions) {
        int remaining = maxCompletions;
        Iterator<Map.Entry<WorldMapRenderTileKey, PendingBuild>> iterator =
                pendingBuilds.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            Map.Entry<WorldMapRenderTileKey, PendingBuild> entry = iterator.next();
            PendingBuild pending = entry.getValue();
            if (!pending.future().isDone()) continue;

            iterator.remove();
            remaining--;
            WorldMapRegion.RenderSnapshot snapshot;
            try {
                snapshot = pending.future().join();
            } catch (CancellationException ignored) {
                continue;
            } catch (Exception ignored) {
                continue;
            }
            if (snapshot == null) continue;

            Integer invalidatedVersion = invalidatedTiles.get(entry.getKey());
            if (snapshot.sourceVersion() != pending.sourceVersion()
                    || invalidatedVersion != null && invalidatedVersion != snapshot.sourceVersion()) {
                snapshot.release();
                continue;
            }

            WorldMapRegion.RenderSnapshot previous = readyTiles.put(entry.getKey(), snapshot);
            if (previous != null) {
                previous.release();
            }
        }
        trimReadyTiles();
    }

    private void publishDirtyTiles(
            List<WorldMapRenderTileKey> neededTiles, UploadBudget uploadBudget) {
        for (WorldMapRenderTileKey key : neededTiles) {
            if (!dirtyTiles.contains(key)) continue;
            if (!uploadBudget.canUpload()) return;

            WorldMapRenderTile tile = tiles.get(key);
            if (tile == null) {
                dirtyTiles.remove(key);
                continue;
            }
            if (tile.uploadPending(textureUploader)) {
                uploadBudget.recordUpload();
            }
            dirtyTiles.remove(key);
        }
    }

    private void publishReadyTiles(
            List<WorldMapRenderTileKey> neededTiles, UploadBudget uploadBudget) {
        for (WorldMapRenderTileKey key : neededTiles) {
            if (!uploadBudget.canUpload()) return;

            WorldMapRegion.RenderSnapshot snapshot = readyTiles.remove(key);
            if (snapshot == null) continue;

            try {
                Integer invalidatedVersion = invalidatedTiles.get(key);
                if (invalidatedVersion != null && invalidatedVersion != snapshot.sourceVersion()) {
                    continue;
                }

                WorldMapRenderTile existing = tiles.get(key);
                if (existing != null) {
                    existing.update(snapshot, textureUploader);
                } else {
                    WorldMapTextureAtlas.Slot slot = allocateSlot();
                    try {
                        tiles.put(key, WorldMapRenderTile.publish(key, snapshot, slot, textureUploader));
                    } catch (RuntimeException exception) {
                        slot.atlas().release(slot);
                        throw exception;
                    }
                }
                invalidatedTiles.remove(key, snapshot.sourceVersion());
                dirtyTiles.remove(key);
                uploadBudget.recordUpload();
            } finally {
                snapshot.release();
            }
        }
    }

    private WorldMapRegion resolveRegion(WorldMapRenderTileKey key, int priority) {
        WorldMapRegion region = mapCache.getLoadedRegion(key.regionX(), key.regionZ());
        return region != null
                ? region
                : mapCache.getOrSchedulePersistedRegion(
                        key.regionX(), key.regionZ(), visibleGeneration, priority);
    }

    private static int sourceVersion(WorldMapRenderTileKey key, WorldMapRegion region) {
        return region.renderVersion(key.level(), key.localXInRegion(), key.localZInRegion());
    }

    private void trim(Set<WorldMapRenderTileKey> protectedTiles) {
        if (tiles.size() <= MAX_CACHED_TILES) {
            removeUntrackedInvalidations();
            removeEmptyAtlases();
            return;
        }

        ArrayList<WorldMapRenderTile> ordered = new ArrayList<>(tiles.values());
        ordered.sort(Comparator.comparingLong(WorldMapRenderTile::lastAccessOrder));
        for (WorldMapRenderTile tile : ordered) {
            if (tiles.size() <= TRIMMED_CACHED_TILES) break;
            if (protectedTiles.contains(tile.key())) continue;
            if (tiles.remove(tile.key(), tile)) {
                dirtyTiles.remove(tile.key());
                tile.releaseSlot();
            }
        }
        removeUntrackedInvalidations();
        removeEmptyAtlases();
    }

    private WorldMapTextureAtlas.Slot allocateSlot() {
        for (WorldMapTextureAtlas textureAtlas : textureAtlases) {
            WorldMapTextureAtlas.Slot slot = textureAtlas.allocate();
            if (slot != null) return slot;
        }

        WorldMapTextureAtlas textureAtlas = new WorldMapTextureAtlas();
        textureAtlases.add(textureAtlas);
        WorldMapTextureAtlas.Slot slot = textureAtlas.allocate();
        if (slot == null) {
            throw new IllegalStateException("New world map texture atlas has no free slots");
        }
        return slot;
    }

    private void trimReadyTiles() {
        Iterator<Map.Entry<WorldMapRenderTileKey, WorldMapRegion.RenderSnapshot>> iterator =
                readyTiles.entrySet().iterator();
        while (readyTiles.size() > MAX_READY_TILES && iterator.hasNext()) {
            Map.Entry<WorldMapRenderTileKey, WorldMapRegion.RenderSnapshot> entry = iterator.next();
            entry.getValue().release();
            iterator.remove();
        }
    }

    private void discardUnneededReadyTiles() {
        Iterator<Map.Entry<WorldMapRenderTileKey, WorldMapRegion.RenderSnapshot>> iterator =
                readyTiles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WorldMapRenderTileKey, WorldMapRegion.RenderSnapshot> entry = iterator.next();
            if (neededTileSet.contains(entry.getKey())) continue;

            entry.getValue().release();
            iterator.remove();
        }
    }

    private static void cancelPendingBuild(PendingBuild pendingBuild) {
        CompletableFuture<WorldMapRegion.RenderSnapshot> future = pendingBuild.future();
        if (future.cancel(false) || !future.isDone()) return;

        try {
            WorldMapRegion.RenderSnapshot snapshot = future.join();
            if (snapshot != null) {
                snapshot.release();
            }
        } catch (CancellationException ignored) {
        } catch (Exception ignored) {
        }
    }

    private void discardPendingBuild(WorldMapRenderTileKey key) {
        PendingBuild pendingBuild = pendingBuilds.remove(key);
        if (pendingBuild != null) {
            cancelPendingBuild(pendingBuild);
        }
    }

    private void discardReadyTile(WorldMapRenderTileKey key) {
        WorldMapRegion.RenderSnapshot snapshot = readyTiles.remove(key);
        if (snapshot != null) {
            snapshot.release();
        }
    }

    private long nextAccessOrder() {
        return ++accessSequence;
    }

    private boolean isTracked(WorldMapRenderTileKey key) {
        return tiles.containsKey(key) || pendingBuilds.containsKey(key) || readyTiles.containsKey(key);
    }

    private void removeUntrackedInvalidations() {
        invalidatedTiles.keySet().removeIf(key -> !isTracked(key));
    }

    private void removeEmptyAtlases() {
        Iterator<WorldMapTextureAtlas> iterator = textureAtlases.iterator();
        while (iterator.hasNext()) {
            WorldMapTextureAtlas atlas = iterator.next();
            if (!atlas.isEmpty()) continue;

            iterator.remove();
            atlas.close();
        }
    }

    public record TileView(ResourceLocation textureId, float u1, float v1, float u2, float v2) {
        private static TileView full(WorldMapTextureAtlas.Slot slot) {
            return new TileView(slot.atlas().textureId(), slot.u1(), slot.v1(), slot.u2(), slot.v2());
        }
    }

    private record PendingBuild(
            int sourceVersion,
            long scheduledGeneration,
            CompletableFuture<WorldMapRegion.RenderSnapshot> future) {}

    private record BuildRequest(
            WorldMapRenderTileKey key, WorldMapRegion region, int sourceVersion, int priority) {}

    private static final class UploadBudget {
        private final long startedNanos = System.nanoTime();
        private int uploads;

        private boolean canUpload() {
            if (uploads < MAX_TEXTURE_UPLOADS_PER_FRAME
                    && (uploads == 0 || System.nanoTime() - startedNanos < TEXTURE_UPLOAD_BUDGET_NANOS)) {
                return true;
            }
            return false;
        }

        private void recordUpload() {
            uploads++;
        }
    }
}
