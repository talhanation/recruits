package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.worldmap.pipeline.WorldMapAsync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class WorldMapRegionSaveQueue {
    private final WorldMapRegionRepository repository;
    private final Consumer<String> successfulSaveCallback;
    private final Set<String> dirtyRegionKeys = new LinkedHashSet<>();
    private final Map<String, PendingRegionSave> pendingRegionSaves = new HashMap<>();

    WorldMapRegionSaveQueue(
            WorldMapRegionRepository repository, Consumer<String> successfulSaveCallback) {
        this.repository = repository;
        this.successfulSaveCallback = successfulSaveCallback;
    }

    int pendingCount() {
        return pendingRegionSaves.size();
    }

    boolean isPending(String regionKey) {
        return pendingRegionSaves.containsKey(regionKey);
    }

    void markDirty(String regionKey) {
        dirtyRegionKeys.add(regionKey);
    }

    void forget(String regionKey) {
        dirtyRegionKeys.remove(regionKey);
    }

    void clear() {
        pendingRegionSaves.clear();
        dirtyRegionKeys.clear();
    }

    void consumeReadySaves(int maxCompletions) {
        int remaining = maxCompletions;
        Iterator<Map.Entry<String, PendingRegionSave>> iterator =
                pendingRegionSaves.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            Map.Entry<String, PendingRegionSave> entry = iterator.next();
            PendingRegionSave pendingSave = entry.getValue();
            if (!pendingSave.future().isDone()) continue;

            iterator.remove();
            remaining--;
            completeRegionSave(entry.getKey(), pendingSave);
        }
    }

    int saveDirtyRegions(
            Map<String, WorldMapRegion> loadedRegions,
            int maxRegions,
            boolean force,
            int maxPendingSaves,
            long quietPeriodNanos) {
        if (!repository.isReady()) return 0;

        long nowNanos = System.nanoTime();
        int saved = 0;
        Iterator<String> iterator = dirtyRegionKeys.iterator();
        while (iterator.hasNext()) {
            String regionKey = iterator.next();
            WorldMapRegion region = loadedRegions.get(regionKey);
            if (region == null || !region.isDirty()) {
                iterator.remove();
                continue;
            }
            if (force || region.isReadyForBackgroundSave(nowNanos, quietPeriodNanos)) {
                if (saveRegion(region, maxPendingSaves)) {
                    saved++;
                }
                if (saved >= maxRegions) break;
            }
        }
        return saved;
    }

    boolean saveRegion(WorldMapRegion region, int maxPendingSaves) {
        String regionKey = WorldMapRegionKey.of(region.getRegionX(), region.getRegionZ());
        if (pendingRegionSaves.containsKey(regionKey)) return false;
        if (pendingRegionSaves.size() >= maxPendingSaves) return false;

        WorldMapRegion.SaveSnapshot snapshot = region.beginSaveSnapshot();
        if (snapshot == null) return false;

        CompletableFuture<WorldMapAsync.RegionSaveResult> future =
                WorldMapAsync.saveRegion(
                        repository.regionFile(region.getRegionX(), region.getRegionZ()), snapshot);
        pendingRegionSaves.put(
                regionKey, new PendingRegionSave(region, snapshot.dirtyVersion(), future));
        return true;
    }

    boolean hasDirtyRegions(Map<String, WorldMapRegion> loadedRegions) {
        Iterator<String> iterator = dirtyRegionKeys.iterator();
        while (iterator.hasNext()) {
            String regionKey = iterator.next();
            WorldMapRegion region = loadedRegions.get(regionKey);
            if (region != null && region.isDirty()) return true;
            iterator.remove();
        }
        return false;
    }

    void saveAllDirtyRegionsAndWait(
            Map<String, WorldMapRegion> loadedRegions,
            int maxPendingSaves,
            long quietPeriodNanos,
            int maxAttempts) {
        int attempts = 0;
        while (hasDirtyRegions(loadedRegions) || !pendingRegionSaves.isEmpty()) {
            saveDirtyRegions(loadedRegions, Integer.MAX_VALUE, true, maxPendingSaves, quietPeriodNanos);
            waitForPendingSaves();
            attempts++;
            if (attempts > maxAttempts) {
                break;
            }
        }
    }

    private void waitForPendingSaves() {
        while (!pendingRegionSaves.isEmpty()) {
            ArrayList<Map.Entry<String, PendingRegionSave>> saves =
                    new ArrayList<>(pendingRegionSaves.entrySet());
            pendingRegionSaves.clear();
            for (Map.Entry<String, PendingRegionSave> entry : saves) {
                completeRegionSave(entry.getKey(), entry.getValue());
            }
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
        repository.recordSource(regionKey);
        successfulSaveCallback.accept(regionKey);
    }

    private record PendingRegionSave(
            WorldMapRegion region,
            int dirtyVersion,
            CompletableFuture<WorldMapAsync.RegionSaveResult> future) {}
}
