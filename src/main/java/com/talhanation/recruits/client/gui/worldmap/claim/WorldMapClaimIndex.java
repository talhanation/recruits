package com.talhanation.recruits.client.gui.worldmap.claim;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.List;

public final class WorldMapClaimIndex {
    private static final Long2ObjectOpenHashMap<RecruitsClaim> CLAIM_BY_CHUNK = new Long2ObjectOpenHashMap<>();

    private static List<RecruitsClaim> indexedClaims;
    private static int indexedClaimCount = -1;
    private static int indexedChunkCount = -1;

    private WorldMapClaimIndex() {}

    public static void invalidate() {
        indexedClaims = null;
        indexedClaimCount = -1;
        indexedChunkCount = -1;
        CLAIM_BY_CHUNK.clear();
        ClaimRenderer.invalidateShapeCache();
    }

    @Nullable
    public static RecruitsClaim getClaimAt(ChunkPos chunk) {
        if (chunk == null) return null;
        return getClaimAt(chunk.x, chunk.z);
    }

    @Nullable
    public static RecruitsClaim getClaimAt(int chunkX, int chunkZ) {
        ensureFresh();
        return CLAIM_BY_CHUNK.get(chunkKey(chunkX, chunkZ));
    }

    public static boolean isClaimed(ChunkPos chunk) {
        return getClaimAt(chunk) != null;
    }

    @Nullable
    public static RecruitsClaim getNeighborClaim(ChunkPos chunk) {
        if (chunk == null) return null;
        ensureFresh();

        RecruitsClaim claim = CLAIM_BY_CHUNK.get(chunkKey(chunk.x + 1, chunk.z));
        if (claim != null) return claim;
        claim = CLAIM_BY_CHUNK.get(chunkKey(chunk.x - 1, chunk.z));
        if (claim != null) return claim;
        claim = CLAIM_BY_CHUNK.get(chunkKey(chunk.x, chunk.z + 1));
        if (claim != null) return claim;
        return CLAIM_BY_CHUNK.get(chunkKey(chunk.x, chunk.z - 1));
    }

    public static long chunkKey(ChunkPos chunk) {
        return chunkKey(chunk.x, chunk.z);
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    private static void ensureFresh() {
        List<RecruitsClaim> claims = ClientManager.recruitsClaims;
        int claimCount = claims == null ? 0 : claims.size();
        int chunkCount = countChunks(claims);
        if (claims == indexedClaims
                && claimCount == indexedClaimCount
                && chunkCount == indexedChunkCount) {
            return;
        }

        rebuild(claims, claimCount, chunkCount);
    }

    private static void rebuild(List<RecruitsClaim> claims, int claimCount, int chunkCount) {
        CLAIM_BY_CHUNK.clear();
        indexedClaims = claims;
        indexedClaimCount = claimCount;
        indexedChunkCount = chunkCount;

        if (claims == null) return;
        for (RecruitsClaim claim : claims) {
            if (!isIndexable(claim)) continue;
            for (ChunkPos chunk : claim.getClaimedChunks()) {
                if (chunk != null) {
                    CLAIM_BY_CHUNK.put(chunkKey(chunk), claim);
                }
            }
        }
    }

    private static int countChunks(List<RecruitsClaim> claims) {
        if (claims == null) return 0;

        int count = 0;
        for (RecruitsClaim claim : claims) {
            if (isIndexable(claim)) {
                count += claim.getClaimedChunks().size();
            }
        }
        return count;
    }

    private static boolean isIndexable(RecruitsClaim claim) {
        return claim != null && !claim.isRemoved && claim.getClaimedChunks() != null;
    }
}
