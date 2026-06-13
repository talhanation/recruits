package com.talhanation.recruits.client.gui.worldmap.pipeline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;

public final class WorldMapChunkBuildQueue {
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
    private long nextChunkRevision;

    public void clear() {
        closePendingBuilds();
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
        nextChunkRevision = 0L;
    }

    public int queuedChunkCount() {
        return queuedChunks.size();
    }

    public int pendingBuildCount() {
        return pendingChunkBuilds.size();
    }

    public int waitingChunkCount() {
        return chunkWaitingRegions.size();
    }

    public int pipelineSize() {
        return queuedChunks.size() + pendingChunkBuilds.size() + chunkWaitingRegions.size();
    }

    public boolean hasQueuedChunks() {
        return !queuedChunks.isEmpty();
    }

    public boolean hasWaitingRegions() {
        return !chunksWaitingByRegion.isEmpty();
    }

    public boolean isForcedRebuild(long chunkKey) {
        return forcedRebuildChunks.contains(chunkKey);
    }

    public Long revision(long chunkKey) {
        return chunkRevisions.get(chunkKey);
    }

    public long readyNanosOrZero(long chunkKey) {
        return chunkReadyNanos.getOrDefault(chunkKey, 0L);
    }

    public boolean hasRevision(long chunkKey) {
        return chunkRevisions.containsKey(chunkKey);
    }

    public boolean hasWork(long chunkKey) {
        return chunkRevisions.containsKey(chunkKey)
                || queuedChunks.contains(chunkKey)
                || pendingChunkBuilds.containsKey(chunkKey)
                || chunkWaitingRegions.containsKey(chunkKey);
    }

    public boolean canMergeForcedReadyTime(long chunkKey) {
        return chunkRevisions.containsKey(chunkKey)
                && forcedRebuildChunks.contains(chunkKey)
                && (queuedChunks.contains(chunkKey) || chunkWaitingRegions.containsKey(chunkKey));
    }

    public void mergeReadyNanos(long chunkKey, long readyNanos) {
        chunkReadyNanos.merge(chunkKey, readyNanos, Math::max);
    }

    public void startRevision(long chunkKey, long readyNanos, boolean forceRebuild) {
        if (forceRebuild) {
            forcedRebuildChunks.add(chunkKey);
        }
        chunkRevisions.put(chunkKey, ++nextChunkRevision);
        chunkReadyNanos.put(chunkKey, readyNanos);
    }

    public void removeBuildMetadata(long chunkKey) {
        forcedRebuildChunks.remove(chunkKey);
        chunkRevisions.remove(chunkKey);
        chunkReadyNanos.remove(chunkKey);
    }

    public void completeRevision(long chunkKey, long revision) {
        chunkRevisions.remove(chunkKey, revision);
        chunkReadyNanos.remove(chunkKey);
        forcedRebuildChunks.remove(chunkKey);
    }

    public boolean enqueueForBuild(long chunkKey, boolean urgent, int maxQueuedChunks) {
        if (pendingChunkBuilds.containsKey(chunkKey)) {
            removeFromWaiters(chunkKey);
            if (urgent) urgentChunks.add(chunkKey);
            return true;
        }
        if (queuedChunks.contains(chunkKey)) {
            removeFromWaiters(chunkKey);
            if (urgent && urgentChunks.add(chunkKey)) {
                chunkUpdateQueue.remove(chunkKey);
                chunkUpdateQueue.addFirst(chunkKey);
            }
            return true;
        }
        if (queuedChunks.size() >= maxQueuedChunks) return false;

        removeFromWaiters(chunkKey);
        queuedChunks.add(chunkKey);
        if (urgent) {
            urgentChunks.add(chunkKey);
            chunkUpdateQueue.addFirst(chunkKey);
        } else {
            chunkUpdateQueue.addLast(chunkKey);
        }
        return true;
    }

    public boolean hasRequiredBuild(long chunkKey) {
        if (urgentChunks.contains(chunkKey)) return true;
        if (forcedRebuildChunks.contains(chunkKey)) return true;

        PendingChunkBuild pendingBuild = pendingChunkBuilds.get(chunkKey);
        return pendingBuild != null && (pendingBuild.urgent() || pendingBuild.forcedRebuild());
    }

    public void cancelWork(long chunkKey) {
        PendingChunkBuild pendingBuild = pendingChunkBuilds.remove(chunkKey);
        if (pendingBuild != null) {
            pendingBuild.cancel();
        }
        if (queuedChunks.remove(chunkKey)) {
            chunkUpdateQueue.remove(chunkKey);
        }
        urgentChunks.remove(chunkKey);
        removeBuildMetadata(chunkKey);
        removeFromWaiters(chunkKey);
    }

    public void forget(long chunkKey) {
        cancelWork(chunkKey);
    }

    public void discardOutside(
            int centerChunkX, int centerChunkZ, int keepRadius, LongConsumer onForgotten) {
        int keepRadiusSquared = keepRadius * keepRadius;
        Iterator<Long> iterator = chunkUpdateQueue.iterator();
        while (iterator.hasNext()) {
            long chunkKey = iterator.next();
            if (isInsideRadius(chunkKey, centerChunkX, centerChunkZ, keepRadiusSquared)) continue;

            iterator.remove();
            queuedChunks.remove(chunkKey);
            urgentChunks.remove(chunkKey);
            removeBuildMetadata(chunkKey);
            removeFromWaiters(chunkKey);
            onForgotten.accept(chunkKey);
        }

        Iterator<Map.Entry<Long, PendingChunkBuild>> pendingIterator =
                pendingChunkBuilds.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<Long, PendingChunkBuild> entry = pendingIterator.next();
            long chunkKey = entry.getKey();
            if (isInsideRadius(chunkKey, centerChunkX, centerChunkZ, keepRadiusSquared)) continue;

            pendingIterator.remove();
            entry.getValue().cancel();
            urgentChunks.remove(chunkKey);
            removeBuildMetadata(chunkKey);
            removeFromWaiters(chunkKey);
            onForgotten.accept(chunkKey);
        }

        ArrayList<Long> waitingChunks = new ArrayList<>(chunkWaitingRegions.keySet());
        for (long chunkKey : waitingChunks) {
            if (!isInsideRadius(chunkKey, centerChunkX, centerChunkZ, keepRadiusSquared)) {
                forget(chunkKey);
                onForgotten.accept(chunkKey);
            }
        }
    }

    public void prioritizeAround(int centerChunkX, int centerChunkZ) {
        if (chunkUpdateQueue.size() < 2) return;

        ArrayList<Long> orderedChunks = new ArrayList<>(chunkUpdateQueue);
        orderedChunks.sort(
                (left, right) -> {
                    boolean leftUrgent = urgentChunks.contains(left);
                    boolean rightUrgent = urgentChunks.contains(right);
                    if (leftUrgent != rightUrgent) return leftUrgent ? -1 : 1;

                    return Integer.compare(
                            distanceSquaredToChunk(left, centerChunkX, centerChunkZ),
                            distanceSquaredToChunk(right, centerChunkX, centerChunkZ));
                });
        chunkUpdateQueue.clear();
        chunkUpdateQueue.addAll(orderedChunks);
    }

    public BuildCandidate pollNextForBuild(long nowNanos, int maxAttempts, long deadlineNanos) {
        if (maxAttempts <= 0) return null;

        int checkedChunks = 0;
        Iterator<Long> iterator = chunkUpdateQueue.iterator();
        while (iterator.hasNext() && checkedChunks < maxAttempts && System.nanoTime() < deadlineNanos) {
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
                removeBuildMetadata(chunkKey);
                continue;
            }

            long readyNanos = readyNanosOrZero(chunkKey);
            if (nowNanos < readyNanos) continue;

            boolean urgent = urgentChunks.contains(chunkKey);
            iterator.remove();
            queuedChunks.remove(chunkKey);
            return new BuildCandidate(chunkKey, urgentChunks.remove(chunkKey) || urgent, checkedChunks);
        }

        return new BuildCandidate(Long.MIN_VALUE, false, checkedChunks);
    }

    public void putPendingBuild(long chunkKey, PendingChunkBuild pendingBuild) {
        pendingChunkBuilds.put(chunkKey, pendingBuild);
    }

    public Iterator<Map.Entry<Long, PendingChunkBuild>> pendingBuildIterator() {
        return pendingChunkBuilds.entrySet().iterator();
    }

    public boolean consumePendingUrgency(long chunkKey, PendingChunkBuild pendingBuild) {
        boolean queuedUrgent = consumeUrgency(chunkKey);
        return pendingBuild.urgent() || queuedUrgent;
    }

    public boolean consumeUrgency(long chunkKey) {
        return urgentChunks.remove(chunkKey);
    }

    public void enqueueDeferred(long chunkKey, boolean urgent) {
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

    public void enqueueDelayed(long chunkKey, boolean urgent) {
        if (urgent) {
            urgentChunks.add(chunkKey);
        }
        if (queuedChunks.add(chunkKey)) {
            chunkUpdateQueue.addLast(chunkKey);
        }
    }

    public void parkForRegion(String regionKey, long chunkKey, boolean urgent) {
        removeFromRegionWait(chunkKey);
        chunkWaitingRegions.put(chunkKey, regionKey);
        chunksWaitingByRegion.computeIfAbsent(regionKey, ignored -> new HashSet<>()).add(chunkKey);
        if (urgent) urgentChunks.add(chunkKey);
    }

    public void queueRegionWake(String regionKey) {
        if (regionKey != null && queuedRegionWakes.add(regionKey)) {
            regionWakeQueue.addLast(regionKey);
        }
    }

    public boolean hasRegionWake() {
        return !regionWakeQueue.isEmpty();
    }

    public String peekRegionWake() {
        return regionWakeQueue.peekFirst();
    }

    public Long takeWaitingChunkForRegion(String regionKey) {
        Set<Long> waitingChunks = chunksWaitingByRegion.get(regionKey);
        if (waitingChunks == null || waitingChunks.isEmpty()) {
            chunksWaitingByRegion.remove(regionKey);
            regionWakeQueue.removeFirst();
            queuedRegionWakes.remove(regionKey);
            return null;
        }

        Iterator<Long> iterator = waitingChunks.iterator();
        long chunkKey = iterator.next();
        iterator.remove();
        chunkWaitingRegions.remove(chunkKey, regionKey);
        return chunkKey;
    }

    public List<String> waitingRegionKeysSnapshot() {
        return new ArrayList<>(chunksWaitingByRegion.keySet());
    }

    public Long peekWaitingChunk(String regionKey) {
        Set<Long> waitingChunks = chunksWaitingByRegion.get(regionKey);
        if (waitingChunks == null || waitingChunks.isEmpty()) return null;
        return waitingChunks.iterator().next();
    }

    public void removeWaitingRegionIfEmpty(String regionKey) {
        Set<Long> waitingChunks = chunksWaitingByRegion.get(regionKey);
        if (waitingChunks != null && waitingChunks.isEmpty()) {
            chunksWaitingByRegion.remove(regionKey);
        }
    }

    private void removeFromWaiters(long chunkKey) {
        removeFromRegionWait(chunkKey);
    }

    private void closePendingBuilds() {
        for (PendingChunkBuild pendingBuild : pendingChunkBuilds.values()) {
            pendingBuild.cancel();
        }
        pendingChunkBuilds.clear();
    }

    private void removeFromRegionWait(long chunkKey) {
        String regionKey = chunkWaitingRegions.remove(chunkKey);
        if (regionKey == null) return;

        Set<Long> waitingChunks = chunksWaitingByRegion.get(regionKey);
        if (waitingChunks == null) return;
        waitingChunks.remove(chunkKey);
        if (waitingChunks.isEmpty()) {
            chunksWaitingByRegion.remove(regionKey);
        }
    }

    private static boolean isInsideRadius(
            long chunkKey, int centerChunkX, int centerChunkZ, int radiusSquared) {
        return distanceSquaredToChunk(chunkKey, centerChunkX, centerChunkZ) <= radiusSquared;
    }

    private static int distanceSquaredToChunk(long chunkKey, int centerChunkX, int centerChunkZ) {
        int dx = chunkX(chunkKey) - centerChunkX;
        int dz = chunkZ(chunkKey) - centerChunkZ;
        return dx * dx + dz * dz;
    }

    private static int chunkX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    private static int chunkZ(long chunkKey) {
        return (int) chunkKey;
    }
    public record BuildCandidate(long chunkKey, boolean urgent, int checkedChunks) {
        public boolean hasChunk() {
            return chunkKey != Long.MIN_VALUE;
        }
    }
}
