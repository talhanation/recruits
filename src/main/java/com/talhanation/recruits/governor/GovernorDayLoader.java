package com.talhanation.recruits.governor;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.*;

/**
 * Daily term of office of a Governor whose claim is not loaded: at the villagers' meeting the whole claim is
 * loaded for a short time, as if the owner stood there. Taxes, the count of inhabitants, the noble election,
 * the daily check of the modules (Workers production) and hiring run for real in this time.
 * <ul>
 *     <li>Claims load one after another (one every {@link #STAGGER} ticks), so there is no load spike.</li>
 *     <li>At most {@code GovernorDayMaxChunks} chunks at the same time; who waited longest comes first.</li>
 *     <li>A claim is released as soon as its Governor did his daily work, at the latest at the end of the meeting.</li>
 *     <li>A claim whose Governor is loaded anyway (a player is near) is skipped.</li>
 * </ul>
 * Tickets belong to the claim UUID; left over tickets (crash) are removed when the server starts.
 */
public final class GovernorDayLoader {

    private static final long LOAD_START = 10000L;
    private static final long LOAD_END = 11900L;
    /** Ticks a claim stays loaded at least: the villagers are counted every 200 ticks, nobles are chosen at the meeting. */
    private static final long MIN_LOADED_TICKS = 600L;
    private static final int STAGGER = 10;

    private record Loaded(List<ChunkPos> chunks, long since) {
    }

    private static final Map<UUID, Loaded> LOADED = new HashMap<>();
    private static final Map<UUID, Long> LAST_LOADED = new HashMap<>();
    private static final Deque<UUID> QUEUE = new ArrayDeque<>();
    private static long plannedDay = -1;
    private static int forcedChunks;

    private GovernorDayLoader() {
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        GovernorManager manager = GovernorManager.get();
        if (level == null || manager == null) return;

        long dayTime = level.getDayTime();
        long day = dayTime / 24000L;
        long time = dayTime % 24000L;
        if (!RecruitsServerConfig.GovernorDayLoading.get() || time < LOAD_START || time >= LOAD_END) {
            if (!LOADED.isEmpty()) releaseAll(level);
            return;
        }

        if (plannedDay != day) plan(manager, day);
        if (level.getGameTime() % STAGGER == 0 && !QUEUE.isEmpty()) load(level, manager, QUEUE.poll(), day);
        if (level.getGameTime() % 20 == 0) releaseFinished(level, manager, day);
    }

    /** The claims of today, the one loaded longest ago first. */
    private static void plan(GovernorManager manager, long day) {
        plannedDay = day;
        QUEUE.clear();
        List<UUID> claims = new ArrayList<>();
        for (ClaimGovernorData data : manager.getAll()) {
            if (data.hasGovernor()) claims.add(data.getClaimId());
        }
        claims.sort(Comparator.comparingLong(id -> LAST_LOADED.getOrDefault(id, -1L)));
        QUEUE.addAll(claims);
    }

    private static void load(ServerLevel level, GovernorManager manager, UUID claimId, long day) {
        ClaimGovernorData data = manager.getData(claimId);
        RecruitsClaim claim = GovernorManager.getClaim(claimId);
        if (data == null || claim == null || !data.hasGovernor() || LOADED.containsKey(claimId)) return;
        // done already (a player was there), or the Governor is loaded anyway
        if (data.getLastModuleDay() == day) return;
        if (data.getGovernorId() != null && level.getEntity(data.getGovernorId()) != null) return;

        List<ChunkPos> chunks = new ArrayList<>(claim.getClaimedChunks());
        if (chunks.isEmpty()) return;
        if (forcedChunks + chunks.size() > RecruitsServerConfig.GovernorDayMaxChunks.get()) {
            GovernorManager.notifyPlayer(level, data.getGovernorOwner(), Component.translatable("chat.recruits.governor.day_load_limit",
                    data.getGovernorName(), claim.getName()));
            return;
        }
        for (ChunkPos chunk : chunks) {
            ForgeChunkManager.forceChunk(level, Main.MOD_ID, claimId, chunk.x, chunk.z, true, true);
        }
        forcedChunks += chunks.size();
        LOADED.put(claimId, new Loaded(chunks, level.getGameTime()));
        LAST_LOADED.put(claimId, day);
    }

    private static void releaseFinished(ServerLevel level, GovernorManager manager, long day) {
        for (UUID claimId : new ArrayList<>(LOADED.keySet())) {
            Loaded loaded = LOADED.get(claimId);
            ClaimGovernorData data = manager.getData(claimId);
            boolean done = data == null || !data.hasGovernor()
                    || (data.getLastModuleDay() == day && level.getGameTime() - loaded.since() >= MIN_LOADED_TICKS);
            if (done) release(level, claimId);
        }
    }

    private static void release(ServerLevel level, UUID claimId) {
        Loaded loaded = LOADED.remove(claimId);
        if (loaded == null) return;
        for (ChunkPos chunk : loaded.chunks()) {
            ForgeChunkManager.forceChunk(level, Main.MOD_ID, claimId, chunk.x, chunk.z, false, true);
        }
        forcedChunks = Math.max(0, forcedChunks - loaded.chunks().size());
    }

    public static void releaseAll(ServerLevel level) {
        for (UUID claimId : new ArrayList<>(LOADED.keySet())) release(level, claimId);
        LOADED.clear();
        forcedChunks = 0;
    }

    /** Server start: tickets of claims that were still loaded when the server stopped (crash) are removed. */
    public static void clearStaleTickets(ServerLevel level, GovernorManager manager) {
        LOADED.clear();
        QUEUE.clear();
        plannedDay = -1;
        forcedChunks = 0;
        for (ClaimGovernorData data : manager.getAll()) {
            RecruitsClaim claim = GovernorManager.getClaim(data.getClaimId());
            if (claim == null) continue;
            for (ChunkPos chunk : claim.getClaimedChunks()) {
                ForgeChunkManager.forceChunk(level, Main.MOD_ID, data.getClaimId(), chunk.x, chunk.z, false, true);
            }
        }
    }
}
