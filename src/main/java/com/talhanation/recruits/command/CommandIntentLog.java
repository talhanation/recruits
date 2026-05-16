package com.talhanation.recruits.command;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player in-memory ring buffer of recently issued recruit commands.
 */
public final class CommandIntentLog {
    public static final int MAX_ENTRIES_PER_PLAYER = 256;

    private static final CommandIntentLog INSTANCE = new CommandIntentLog();

    private final Map<UUID, Deque<Entry>> byPlayer = new ConcurrentHashMap<>();

    private CommandIntentLog() {
    }

    public static CommandIntentLog instance() {
        return INSTANCE;
    }

    public record Entry(UUID playerUuid, CommandIntent intent, int actorCount) {
    }

    public void record(ServerPlayer player, CommandIntent intent, int actorCount) {
        if (player == null || intent == null) {
            return;
        }
        Deque<Entry> deque = byPlayer.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addFirst(new Entry(player.getUUID(), intent, actorCount));
            while (deque.size() > MAX_ENTRIES_PER_PLAYER) {
                deque.pollLast();
            }
        }
    }

    public List<Entry> recentFor(UUID playerUuid) {
        if (playerUuid == null) {
            return List.of();
        }
        Deque<Entry> deque = byPlayer.get(playerUuid);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return List.copyOf(deque);
        }
    }

    public int sizeFor(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        Deque<Entry> deque = byPlayer.get(playerUuid);
        return deque == null ? 0 : deque.size();
    }

    public void clearFor(UUID playerUuid) {
        if (playerUuid != null) {
            byPlayer.remove(playerUuid);
        }
    }
}
