package com.talhanation.recruits.client.events;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Purely CLIENT-SIDE debug store for the recruit pathfinder.
 *
 * No networking. In single-player / local testing the integrated server runs in
 * the same JVM as the client, so the pathfinder worker thread can write the
 * finished path straight into this static map and the world renderer
 * ({@link PathDebugRenderer}) reads it. That is enough to SEE the path while
 * testing locally, which is all we want here.
 *
 * Toggle with {@link #setEnabled}. When disabled the pathfinder skips all debug
 * work, so there is zero overhead in normal play.
 *
 * NOTE: because there is no netcode this will NOT show anything on a dedicated
 * server (the path would be built in a different process). That is intentional
 * per the current requirement (local client testing only).
 */
public final class ClientPathDebug {

    public record Entry(int x, int y, int z, float f, float malus, boolean onPath) {}

    public record Snapshot(int mobId,
                           BlockPos target,
                           List<Entry> pathNodes,
                           List<Entry> visited,
                           boolean partial,
                           long timestamp) {}

    private static volatile boolean enabled = false;

    private static final Map<Integer, Snapshot> snapshots = new ConcurrentHashMap<>();

    private ClientPathDebug() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) snapshots.clear();
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    /** called from the pathfinder worker thread (same JVM in local testing). */
    public static void publish(int mobId, BlockPos target, List<Entry> pathNodes, List<Entry> visited, boolean partial) {
        if (!enabled) return;
        snapshots.put(mobId, new Snapshot(mobId, target, pathNodes, visited, partial, System.currentTimeMillis()));
    }

    public static Map<Integer, Snapshot> getSnapshots() {
        return snapshots;
    }

    public static void clear() {
        snapshots.clear();
    }
}